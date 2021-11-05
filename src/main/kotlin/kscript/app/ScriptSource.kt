package kscript.app

import java.net.URI

enum class SourceType { FILE, HTTP, STD_INPUT, OTHER_FILE, PARAMETER }
enum class ScriptType { KT, KTS, OTHER }

data class ScriptSource(
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val includeContext: URI,
    val sourceUri: URI?,
    val codeText: String
)
