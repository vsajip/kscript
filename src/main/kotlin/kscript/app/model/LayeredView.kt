package kscript.app.model

import java.net.URI

data class LayeredView(
    val level: Int,
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI,
    val scriptName: String,
    val sections: List<Section>,
) : Annotation
