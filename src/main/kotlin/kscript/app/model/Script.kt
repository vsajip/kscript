package kscript.app.model

import java.net.URI

data class Script(
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI,
    val scriptName: String,
    val sections: List<Section>,
) : Annotation
