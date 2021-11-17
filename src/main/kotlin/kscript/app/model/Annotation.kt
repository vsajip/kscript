package kscript.app.model

import java.net.URI

enum class SourceType { FILE, HTTP, STD_INPUT, OTHER_FILE, PARAMETER }
enum class ScriptType(val extension: String) { KT(".kt"), KTS(".kts") }

data class Section(val code: String, val annotation: Annotation)

interface Annotation

data class Include(val include: String) : Annotation
data class Package(val packageName: String) : Annotation
data class Import(val importName: String) : Annotation
data class Dependency(val dependencies: List<String>) : Annotation
data class KotlinOpts(val kotlinOpts: List<String>) : Annotation
data class CompilerOpts(val compileOpts: List<String>) : Annotation
data class Entry(val entry: String) : Annotation
data class Repository(val id: String, val url: String, val user: String = "", val password: String = "") : Annotation

class SheBang : Annotation
class Code : Annotation

//URI to another, resolved, single script
//This class replaces include in sections
data class ScriptSource(
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI
) : Annotation
