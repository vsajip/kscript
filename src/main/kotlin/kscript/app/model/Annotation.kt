package kscript.app.model

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
