package kscript.app.model

import java.net.URI

//All scripts unified to single code
data class UnifiedScript(
    val code: String,

    val packageName: String?,
    val entryPoint: String?,

    val scriptSources: Set<URI>,
    val dependencies: Set<String>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<String>,
    val compilerOpts: Set<String>
)
