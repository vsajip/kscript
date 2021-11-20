package kscript.app.model

interface ScriptAnnotation

@JvmInline
value class Include(val value: String) : ScriptAnnotation
@JvmInline
value class Package(val value: String) : ScriptAnnotation
@JvmInline
value class Import(val value: String) : ScriptAnnotation
@JvmInline
value class Dependency(val value: String) : ScriptAnnotation
@JvmInline
value class KotlinOpt(val value: String) : ScriptAnnotation
@JvmInline
value class CompilerOpt(val value: String) : ScriptAnnotation
@JvmInline
value class Entry(val value: String) : ScriptAnnotation

data class Repository(val id: String, val url: String, val user: String = "", val password: String = "") : ScriptAnnotation

object SheBang : ScriptAnnotation
object Code : ScriptAnnotation
