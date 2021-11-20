package kscript.app.resolver

import kscript.app.model.*

internal data class ResolutionContext(
    val maxResolutionLevel: Int,

    var code: MutableList<String> = mutableListOf(),

    var packageLevel: Int = 0,
    var packageName: Package? = null,

    var entryLevel: Int = 0,
    var entry: Entry? = null,

    val scriptNodes: MutableSet<ScriptNode> = mutableSetOf(),
    val includes: MutableSet<Include> = mutableSetOf(),
    val dependencies: MutableSet<Dependency> = mutableSetOf(),
    val repositories: MutableSet<Repository> = mutableSetOf(),
    val kotlinOpts: MutableSet<KotlinOpt> = mutableSetOf(),
    val compilerOpts: MutableSet<CompilerOpt> = mutableSetOf(),
    val imports: MutableSet<Import> = mutableSetOf(),
)
