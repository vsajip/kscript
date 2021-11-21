package kscript.app.resolver

import kscript.app.model.*

data class ResolutionContext(
    val maxResolutionLevel: Int,

    var packageLevel: Int = 0,
    var packageName: PackageName? = null,

    var entryLevel: Int = 0,
    var entry: Entry? = null,

    val scriptNodes: MutableSet<ScriptNode> = mutableSetOf(),
    val includes: MutableSet<Include> = mutableSetOf(),
    val dependencies: MutableSet<Dependency> = mutableSetOf(),
    val repositories: MutableSet<Repository> = mutableSetOf(),
    val kotlinOpts: MutableSet<KotlinOpt> = mutableSetOf(),
    val compilerOpts: MutableSet<CompilerOpt> = mutableSetOf(),
    val importNames: MutableSet<ImportName> = mutableSetOf(),
)
