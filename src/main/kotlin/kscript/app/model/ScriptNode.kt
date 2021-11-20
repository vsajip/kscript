package kscript.app.model

import java.net.URI

data class ScriptNode(
    val level: Int,
    val scriptSource: ScriptSource,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI,
    val scriptName: String,
    val sections: List<Section>,
) : Annotation
