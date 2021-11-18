package kscript.app.model

data class ResolvedScript(
    val code: String,

    val packageName: Package?,
    val entryPoint: Entry?,

    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>
)
