package io.github.hnalvaradohn.oclax.platform.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.hnalvaradohn.oclax.MainActivity
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class SyncthingRuntimeService : Service() {
    companion object {
        private const val ACTION_START = "io.github.hnalvaradohn.oclax.transfer.START"
        private const val ACTION_STOP = "io.github.hnalvaradohn.oclax.transfer.STOP"
        private const val ACTION_ENABLE_LAN = "io.github.hnalvaradohn.oclax.transfer.ENABLE_LAN"
        private const val ACTION_DISABLE_LAN = "io.github.hnalvaradohn.oclax.transfer.DISABLE_LAN"
        private const val CHANNEL_ID = "oclax_transfer_runtime"
        private const val NOTIFICATION_ID = 12041

        fun start(context: Context) {
            val intent = Intent(context, SyncthingRuntimeService::class.java)
                .setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SyncthingRuntimeService::class.java)
                .setAction(ACTION_STOP)
            ContextCompat.startForegroundService(context, intent)
        }

        fun enableLanDiscovery(context: Context) {
            val intent = Intent(context, SyncthingRuntimeService::class.java)
                .setAction(ACTION_ENABLE_LAN)
            ContextCompat.startForegroundService(context, intent)
        }

        fun disableLanDiscovery(context: Context) {
            val intent = Intent(context, SyncthingRuntimeService::class.java)
                .setAction(ACTION_DISABLE_LAN)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val processLock = Any()
    private lateinit var runtimeConfig: SyncthingRuntimeConfig
    private lateinit var startupDiagnostics: RuntimeStartupDiagnostics

    @Volatile
    private var stopping = false

    private var runtimeProcess: Process? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate() {
        super.onCreate()
        runtimeConfig = SyncthingRuntimeConfig(applicationContext)
        startupDiagnostics = RuntimeStartupDiagnostics(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification("Preparando envío entre dispositivos…")

        when (intent?.action) {
            ACTION_STOP -> requestStop()
            ACTION_ENABLE_LAN -> {
                requestStart()
                acquireLanDiscovery()
            }
            ACTION_DISABLE_LAN -> {
                releaseLanDiscovery()
                val running = synchronized(processLock) {
                    runtimeProcess?.isAlive == true
                }
                if (running) {
                    updateNotification("Motor de envío activo.")
                } else {
                    stopForegroundAndSelf()
                }
            }
            else -> requestStart()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopping = true
        releaseLanDiscovery()
        synchronized(processLock) {
            runtimeProcess?.destroyForcibly()
            runtimeProcess = null
        }
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        requestStop()
    }

    private fun requestStart() {
        stopping = false
        worker.execute {
            var stage = RuntimeStartupStage.PREPARING_PRIVATE_CONFIG
            try {
                val alreadyRunning = synchronized(processLock) {
                    runtimeProcess?.isAlive == true
                }
                if (alreadyRunning) {
                    startupDiagnostics.markStage(RuntimeStartupStage.PRIVATE_MODE_VERIFIED)
                    updateNotification("Motor de envío activo.")
                    return@execute
                }

                startupDiagnostics.markStage(stage)
                runtimeConfig.prepare()

                stage = RuntimeStartupStage.PRIVATE_CONFIG_READY
                startupDiagnostics.markStage(stage)

                val builder = ProcessBuilder(runtimeConfig.command())
                    .directory(runtimeConfig.homeDir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(runtimeConfig.logFile))
                runtimeConfig.applyPrivateEnvironment(builder)

                val process = builder.start()
                synchronized(processLock) {
                    runtimeProcess = process
                }
                stage = RuntimeStartupStage.PROCESS_STARTED
                startupDiagnostics.markStage(stage)

                Thread(
                    {
                        val exitCode = runCatching { process.waitFor() }.getOrDefault(-1)
                        val shouldStopService = synchronized(processLock) {
                            val ownedProcess = runtimeProcess === process
                            if (ownedProcess) {
                                runtimeProcess = null
                            }
                            ownedProcess && !stopping
                        }
                        if (shouldStopService) {
                            startupDiagnostics.markExited(exitCode)
                            updateNotification("Motor detenido (código $exitCode).")
                            stopForegroundAndSelf()
                        }
                    },
                    "oclax-transfer-runtime-watch",
                ).apply {
                    isDaemon = true
                    start()
                }

                val restClient = SyncthingRestClient(runtimeConfig)
                restClient.awaitReady()
                stage = RuntimeStartupStage.REST_READY
                startupDiagnostics.markStage(stage)

                restClient.enforcePrivateOptions()
                stage = RuntimeStartupStage.PRIVATE_MODE_VERIFIED
                startupDiagnostics.markStage(stage)

                if (stopping) {
                    requestStop()
                    return@execute
                }

                updateNotification("Motor de envío activo.")
            } catch (error: Exception) {
                startupDiagnostics.markFailure(stage, error.message)
                synchronized(processLock) {
                    runtimeProcess?.destroyForcibly()
                    runtimeProcess = null
                }
                updateNotification("No se pudo iniciar el motor de envío.")
                stopForegroundAndSelf()
            }
        }
    }

    private fun requestStop() {
        stopping = true
        releaseLanDiscovery()
        worker.execute {
            val process = synchronized(processLock) { runtimeProcess }
            if (process != null && process.isAlive) {
                runCatching {
                    SyncthingRestClient(runtimeConfig).shutdown()
                }

                val stoppedGracefully = runCatching {
                    process.waitFor(5, TimeUnit.SECONDS)
                }.getOrDefault(false)

                if (!stoppedGracefully && process.isAlive) {
                    process.destroy()
                    runCatching { process.waitFor(2, TimeUnit.SECONDS) }
                }
                if (process.isAlive) {
                    process.destroyForcibly()
                }
            }

            synchronized(processLock) {
                if (runtimeProcess === process) {
                    runtimeProcess = null
                }
            }
            startupDiagnostics.markStage(RuntimeStartupStage.STOPPED)
            stopForegroundAndSelf()
        }
    }

    private fun acquireLanDiscovery() {
        if (multicastLock?.isHeld == true) return

        val wifiManager = applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return

        multicastLock = wifiManager.createMulticastLock("OclAxLanDiscovery").apply {
            setReferenceCounted(false)
            acquire()
        }
        updateNotification("Buscando dispositivo en la red local…")
    }

    private fun releaseLanDiscovery() {
        multicastLock?.let { lock ->
            if (lock.isHeld) {
                runCatching { lock.release() }
            }
        }
        multicastLock = null
    }

    private fun startForegroundNotification(text: String) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(text),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private fun updateNotification(text: String) {
        // Updating through startForeground keeps this notification inside the
        // foreground-service contract without adding a separate notification permission.
        startForegroundNotification(text)
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("OclAx · Enviar a dispositivo")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transferencias entre dispositivos",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Mantiene activa una transferencia OclAx mientras está en curso."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
