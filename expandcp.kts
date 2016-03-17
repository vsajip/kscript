#!/usr/bin/env kscript

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess


// resdep is a kotlin script for setting a Java classpath from a Maven repository.
// It accepts a set of Maven ids of dependencies and resolves them to a
// classpath suitable for use with 'java -cp' (it will download if necessary).

/*
Features
Features:
* Support for transitive Maven depenendencies (including exclusions)
* Caching of dependency requests (cached requests take around 30ms)
* Automatic Scala version detection (and name mangling as per sbt)

## Todo

- Display of the dependency 'tree' of transitive dependencies
- MVNCP_TEMPLATE can be set to override the inbuilt pom.xml template.
- --clear-cache option to delete the cache

## Example

Return the classpath for log4j (downloading it first if required).
$ resdep log4j:log4j:1.2.14

## References

Inspired by mvncp created by Andrew O'Malley
Written be Holger Brandl
*/

// Stores resolved dependencies
//class Dependencies
//attr_reader :classpath, :tree
//def initialize(classpath, tree)
//@classpath = classpath
//        @tree = tree
//        end
//def valid?
//@classpath && @tree
//end
//end

val useCache = true
val cacheFile = File("/tmp/kscript_deps_cache.txt")


val depIds = args
val depsHash = depIds.joinToString(";")

// todo maybe we should not use cache here but from bash to avoid jvm launch for caches classpaths
if (useCache && cacheFile.isFile()) {
    val cache = cacheFile.
            readLines().filter { it.isNotBlank() }.
            associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

    //    val depsHash = depsMd5(args)

    if (cache.containsKey(depIds.joinToString(";"))) {
        //        println("using cached classpath")
        println(cache.get(depsHash))
        exitProcess(0)
    }
}


val depTags = depIds.map {
    val splitDepId = it.split(":")
    require(listOf(3, 4).contains(splitDepId.size)) { "invalid dependency id: ${it}" }

    """
    <dependency>
            <groupId>${splitDepId[0]}</groupId>
            <artifactId>${splitDepId[1]}</artifactId>
            <version>${splitDepId[2]}</version>
            ${if (splitDepId.size == 4) "<classifier>" + splitDepId[2] + "<classifier>" else ""  }
    </dependency>
    """
}

val pom = """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>resdep_template</groupId>
    <artifactId>resdep_template</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
    ${depTags.joinToString("\n")}
    </dependencies>
</project>
"""


fun runMaven(pom: String, goal: String): Iterable<String> {
    //    val temp = File.createTempFile("__mvncp__temp__", "_pom.xml")
    val temp = File("test.pom")
    temp.writeText(pom)
    val exec = Runtime.getRuntime().exec("mvn -f ${temp.absolutePath} ${goal}")

    return BufferedReader(InputStreamReader(exec.inputStream)).
            lines().toArray().map { it.toString() }
}

val mavenResult = runMaven(pom, "dependency:build-classpath")

val classPath = mavenResult.dropWhile { !it.startsWith("[INFO] Dependencies classpath:") }.drop(1).first()

//println("classpath is ${classPath}")

// append classpath to cache
cacheFile.appendText(depsHash + " " + classPath + "\n")

// print the classpath and exit
println(classPath)
exitProcess(0)
