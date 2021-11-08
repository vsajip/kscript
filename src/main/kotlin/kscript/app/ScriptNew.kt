package kscript.app

import java.net.URI

data class ScriptNew(
    val scriptType: ScriptType,
    val lines: MutableList<String>,
    val includes: MutableList<URI>,
    val dependencies: MutableList<String>,
    val customRepositories: MutableList<String>,
    val kotlinOpts: MutableList<String>,
    val compilerOpts: MutableList<String>
)
