package kscript.app.model

import java.net.URI

//Script with all ScriptSources replaced with real Script's
data class ResolvedScript(
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI,
    val sections: List<Section>,
)
