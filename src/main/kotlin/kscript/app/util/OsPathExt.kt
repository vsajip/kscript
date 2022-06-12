package kscript.app.util

import kscript.app.model.OsType
import java.nio.file.Path
import java.nio.file.Paths

//Path is always native, so implicit conversion to native path
fun OsPath.toNativePath(): Path = Paths.get(toNativeOsPath().stringPath())

fun OsPath.toNativeOsPath() = if (osType.isPosixHostedOnWindows()) convert(OsType.WINDOWS) else this
