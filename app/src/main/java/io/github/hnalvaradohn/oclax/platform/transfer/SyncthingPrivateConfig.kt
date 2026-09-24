package io.github.hnalvaradohn.oclax.platform.transfer

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal object SyncthingPrivateConfig {
    const val SAFE_LISTEN_ADDRESS = "tcp://127.0.0.1:0"

    private const val DISALLOW_DOCTYPE =
        "http://apache.org/xml/features/disallow-doctype-decl"
    private const val EXTERNAL_GENERAL_ENTITIES =
        "http://xml.org/sax/features/external-general-entities"
    private const val EXTERNAL_PARAMETER_ENTITIES =
        "http://xml.org/sax/features/external-parameter-entities"
    private const val LOAD_EXTERNAL_DTD =
        "http://apache.org/xml/features/nonvalidating/load-external-dtd"

    fun harden(
        configFile: File,
        apiKey: String,
    ) {
        require(apiKey.length >= 32) { "La API key local es demasiado corta." }
        check(configFile.isFile) { "No existe la configuración del motor." }

        val document = parsePrivateConfig(configFile)
        document.documentElement.normalize()

        val root = document.documentElement
        check(root.tagName == "configuration") {
            "La configuración del motor tiene un formato inesperado."
        }

        val gui = root.directChild("gui")
            ?: error("La configuración del motor no contiene GUI.")
        val options = root.directChild("options")
            ?: error("La configuración del motor no contiene opciones.")

        gui.replaceDirectChildren("address", listOf("127.0.0.1:8384"))
        gui.replaceDirectChildren("apikey", listOf(apiKey))

        options.replaceDirectChildren("listenAddress", listOf(SAFE_LISTEN_ADDRESS))
        options.replaceDirectChildren("globalAnnounceEnabled", listOf("false"))
        options.replaceDirectChildren("localAnnounceEnabled", listOf("false"))
        options.replaceDirectChildren("relaysEnabled", listOf("false"))
        options.replaceDirectChildren("natEnabled", listOf("false"))
        options.replaceDirectChildren("startBrowser", listOf("false"))
        options.replaceDirectChildren("urAccepted", listOf("-1"))
        options.replaceDirectChildren("crashReportingEnabled", listOf("false"))

        val tempFile = File(configFile.parentFile, configFile.name + ".oclax.tmp")
        check(!tempFile.exists() || tempFile.delete()) {
            "No se pudo preparar el archivo temporal de configuración."
        }

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
        }

        tempFile.outputStream().buffered().use { output ->
            transformer.transform(DOMSource(document), StreamResult(output))
            output.flush()
        }

        try {
            Files.move(
                tempFile.toPath(),
                configFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                tempFile.toPath(),
                configFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    internal fun snapshot(configFile: File): SyncthingPrivateConfigSnapshot {
        val document = parsePrivateConfig(configFile)
        val root = document.documentElement
        val gui = root.directChild("gui") ?: error("GUI ausente.")
        val options = root.directChild("options") ?: error("Opciones ausentes.")

        return SyncthingPrivateConfigSnapshot(
            guiAddress = gui.directChildText("address"),
            apiKey = gui.directChildText("apikey"),
            listenAddresses = options.directChildren("listenAddress").map { it.textContent.trim() },
            globalDiscovery = options.directChildText("globalAnnounceEnabled").toBooleanStrict(),
            localDiscovery = options.directChildText("localAnnounceEnabled").toBooleanStrict(),
            relays = options.directChildText("relaysEnabled").toBooleanStrict(),
            nat = options.directChildText("natEnabled").toBooleanStrict(),
            startBrowser = options.directChildText("startBrowser").toBooleanStrict(),
            usageReportingAccepted = options.directChildText("urAccepted").toInt(),
            crashReporting = options.directChildText("crashReportingEnabled").toBooleanStrict(),
        )
    }

    private fun parsePrivateConfig(configFile: File): Document {
        rejectUnsafeDeclarations(configFile)

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false

            // Android vendors do not expose an identical set of Xerces features.
            // These flags are defense-in-depth only: the pre-scan and entity resolver below
            // enforce the security boundary even when a feature is unsupported.
            applyOptionalXmlFeature(DISALLOW_DOCTYPE, true, ::setFeature)
            applyOptionalXmlFeature(EXTERNAL_GENERAL_ENTITIES, false, ::setFeature)
            applyOptionalXmlFeature(EXTERNAL_PARAMETER_ENTITIES, false, ::setFeature)
            applyOptionalXmlFeature(LOAD_EXTERNAL_DTD, false, ::setFeature)
            runCatching { isXIncludeAware = false }
            isExpandEntityReferences = false
        }

        val builder = factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ ->
                throw SAXException("La configuración del motor referencia una entidad externa.")
            }
        }

        return builder.parse(configFile)
    }

    private fun rejectUnsafeDeclarations(configFile: File) {
        configFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                check(!line.contains("<!DOCTYPE", ignoreCase = true)) {
                    "La configuración del motor contiene una declaración DOCTYPE no permitida."
                }
                check(!line.contains("<!ENTITY", ignoreCase = true)) {
                    "La configuración del motor contiene una declaración ENTITY no permitida."
                }
            }
        }
    }

    internal fun applyOptionalXmlFeature(
        feature: String,
        value: Boolean,
        setter: (String, Boolean) -> Unit,
    ): Boolean = runCatching {
        setter(feature, value)
    }.isSuccess

    private fun Element.directChild(name: String): Element? =
        directChildren(name).firstOrNull()

    private fun Element.directChildText(name: String): String =
        directChild(name)?.textContent?.trim()
            ?: error("Falta la opción $name.")

    private fun Element.directChildren(name: String): List<Element> =
        buildList {
            val nodes = childNodes
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                if (node is Element && node.tagName == name) {
                    add(node)
                }
            }
        }

    private fun Element.replaceDirectChildren(
        name: String,
        values: List<String>,
    ) {
        directChildren(name).forEach { removeChild(it) }
        values.forEach { value ->
            appendChild(
                ownerDocument.createElement(name).apply {
                    textContent = value
                },
            )
        }
    }
}

internal data class SyncthingPrivateConfigSnapshot(
    val guiAddress: String,
    val apiKey: String,
    val listenAddresses: List<String>,
    val globalDiscovery: Boolean,
    val localDiscovery: Boolean,
    val relays: Boolean,
    val nat: Boolean,
    val startBrowser: Boolean,
    val usageReportingAccepted: Int,
    val crashReporting: Boolean,
)
