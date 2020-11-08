package kscript.app

import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

suspend fun main() {


//    https://stackoverflow.com/questions/44265547/how-to-properly-specify-jcenter-repository-in-maven-config
    val jcenterResolver = MavenDependenciesResolver().apply {
        addRepository(RepositoryCoordinates("https://jcenter.bintray.com") )
    }

    val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver(), jcenterResolver)
//    resolver.addRepository()

    val example = "de.mpicbg.scicomp:kutils:0.7"

    val results = resolver.resolve(example)

    print(results)
}
