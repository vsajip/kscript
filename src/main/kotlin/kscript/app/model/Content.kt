package kscript.app.model

import java.net.URI
import java.nio.file.Path

data class Content(
    val text: String,
    val scriptType: ScriptType,
    val fileName: String,
    val uri: URI, //Real one from Web, not the cached file
    val contextUri: URI, //Real one from Web, not the cached file
    val localPath: Path, //Local file path
)
