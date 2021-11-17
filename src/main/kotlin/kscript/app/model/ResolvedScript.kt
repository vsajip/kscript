package kscript.app.model

//All scripts unified to single code
data class ResolvedScript(
    val code: String,

    val packageName: String?,
    val entryPoint: String?,

    val scriptSources: Set<ScriptSource>,
    val dependencies: Set<String>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<String>,
    val compilerOpts: Set<String>
)
