package kscript.app.model

import java.net.URI

data class Script(
    val scriptSource: ScriptSource,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI,
    val scriptName: String,

    val resolvedCode: String,

    val packageName: Package?,
    val entryPoint: Entry?,
    val imports: List<Import>,

    val includes: Set<Include>,
    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>,

    val scriptNodes: Set<ScriptNode>,
    val rootNode: ScriptNode,
)
