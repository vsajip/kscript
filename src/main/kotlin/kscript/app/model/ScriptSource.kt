package kscript.app.model

import java.net.URI

data class ScriptSource(
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI
)
