package io.github.hnalvaradohn.oclax

internal enum class HomeBackAction {
    CLEAR_OCLAX_VIEW,
    RETURN_TO_OCLAX,
    SHOW_EXIT_CONFIRMATION,
    EXIT_APP,
}

internal fun homeBackAction(
    sourceIsOclAx: Boolean,
    hasContentDrillDown: Boolean,
    exitArmed: Boolean,
): HomeBackAction = when {
    !sourceIsOclAx -> HomeBackAction.RETURN_TO_OCLAX
    hasContentDrillDown -> HomeBackAction.CLEAR_OCLAX_VIEW
    exitArmed -> HomeBackAction.EXIT_APP
    else -> HomeBackAction.SHOW_EXIT_CONFIRMATION
}
