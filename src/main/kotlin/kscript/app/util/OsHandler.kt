package kscript.app.util

import kscript.app.model.OsConfig
import java.io.File
import java.net.URI

class OsHandler(private val osConfig: OsConfig) {
    fun resolveRootUri(path: String): URI = File(path).toURI()
    fun resolveHomeDirUri(path: String): URI = File(osConfig.userHomeDir.resolve(path).stringPath()).toURI()
    fun resolveCurrentDir(): URI = File(".").toURI()
    fun createPath(path: String): OsPath = OsPath.create(osConfig.osType, path)
    fun canReadPath(path: OsPath): Boolean = path.native().path().toFile().canRead()


}
