package kscript.app.model

//All scripts unified to single code
data class ResolvedScript(
    val code: String,

    val packageName: Package?,
    val entryPoint: Entry?,

    val scriptSources: Set<ScriptSource>,
    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>
)
