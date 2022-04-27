package kscript.app.model

import java.net.URI

data class Script(
    val scriptSource: ScriptSource,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI,
    val scriptName: String,

    val resolvedCode: String,

    val packageName: PackageName,
    val entryPoint: Entry?,
    val importNames: Set<ImportName>,

    val includes: Set<Include>,
    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>,

    val scriptNodes: Set<ScriptNode>,
    val rootNode: ScriptNode,

    val digest: String
)
