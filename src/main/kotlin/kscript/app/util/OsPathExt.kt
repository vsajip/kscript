package kscript.app.util

import kscript.app.model.OsType
import java.nio.file.Path
import java.nio.file.Paths

fun OsPath.path(): Path = Paths.get(native().stringPath())
fun OsPath.native(): OsPath = if (osType.isPosixHostedOnWindows()) convert(OsType.WINDOWS) else this
