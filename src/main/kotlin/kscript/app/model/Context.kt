package kscript.app.model

import java.net.URI

data class Context(
    var sourceUri: URI? = null,
    var sourceContextUri: URI? = null,

    var allowFileReferences: Boolean = true,
    var matched: Boolean = false,
    var level: Int = 0,

    val includes: MutableSet<URI> = mutableSetOf(),
    val lines: MutableList<String> = mutableListOf(),
    val dependencies: MutableSet<String> = mutableSetOf(),
    val repositories: MutableSet<Repository> = mutableSetOf(),
    val kotlinOpts: MutableSet<String> = mutableSetOf(),
    val compilerOpts: MutableSet<String> = mutableSetOf(),
    val imports: MutableSet<String> = mutableSetOf(),
    var packageName: String? = null,
    var entryPoint: String? = null,
)
