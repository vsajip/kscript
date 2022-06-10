package kscript.app.util

import java.nio.file.Path
import java.nio.file.Paths

fun OsPath.path(): Path = Paths.get(stringPath())
