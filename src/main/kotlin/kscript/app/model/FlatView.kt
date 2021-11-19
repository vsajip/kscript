package kscript.app.model

data class FlatView(
    val code: String,

    val packageName: Package?,
    val entryPoint: Entry?,

    val layeredViews: Set<LayeredView>,
    val includes: Set<Include>,

    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>
)
