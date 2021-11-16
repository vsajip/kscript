package kscript.app.appdir

import java.nio.file.Path

class DependencyCache(private val dependencyCachePath: Path) {

    fun read() : String {
        return dependencyCachePath.toFile().readText()
    }

    fun append(dependency: String) {
        dependencyCachePath.toFile().appendText(dependency)
    }
}
