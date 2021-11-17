package kscript.app.model

import java.net.URI

interface Annotation

@JvmInline
value class Include(val value: String) : Annotation
@JvmInline
value class Package(val value: String) : Annotation
@JvmInline
value class Import(val value: String) : Annotation
@JvmInline
value class Dependency(val value: String) : Annotation
@JvmInline
value class KotlinOpt(val value: String) : Annotation
@JvmInline
value class CompilerOpt(val value: String) : Annotation
@JvmInline
value class Entry(val value: String) : Annotation

data class Repository(val id: String, val url: String, val user: String = "", val password: String = "") : Annotation

object SheBang : Annotation
object Code : Annotation

//URI to another, resolved, single script
//This class replaces include in sections
data class ScriptSource(
    val sourceType: SourceType,
    val scriptType: ScriptType,
    val sourceUri: URI?,
    val sourceContextUri: URI
) : Annotation
