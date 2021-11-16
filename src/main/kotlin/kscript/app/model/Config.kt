package kscript.app.model

import kscript.app.quit
import kscript.app.util.Logger
import java.nio.file.Path

data class Config(
    val selfName: String,
    val kscriptDir: Path,
    val customPreamble: String,
    val intellijCommand: String,
    val gradleCommand: String,
    val kotlinHome: Path?,
    val classPathSeparator: String
)
