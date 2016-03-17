#!/usr/bin/env kscript

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**

expandcp.kts is a kotlin script for setting a Java classpath from a Maven repository.
It accepts a set of Maven ids of dependencies and resolves them to a
classpath suitable for use with 'java -cp' or 'kotlin =cp' (it will download if necessary).

## Features

 * Support for transitive Maven dependencies (including exclusions)
* Caching of dependency requests (cached requests take around 30ms)

## Todo

- Display of the dependency 'tree' of transitive dependencies
- MVNCP_TEMPLATE can be set to override the inbuilt pom.xml template.
- --clear-cache option to delete the cache

## Example

Return the classpath for log4j (downloading it first if required).
$ resdep log4j:log4j:1.2.14

## References

Inspired by mvncp created by Andrew O'Malley
Written be Holger Brandl 2016

 */


val useCache = true
val cacheFile = File("/tmp/kscript_deps_cache.txt")


val depIds = args
val depsHash = depIds.joinToString(";")

// todo maybe we should not use cache here but from bash to avoid jvm launch for cached classpaths
if (useCache && cacheFile.isFile()) {
    val cache = cacheFile.
            readLines().filter { it.isNotBlank() }.
            associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

    if (cache.containsKey(depIds.joinToString(";"))) {
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
    val temp = File.createTempFile("__mvncp__temp__", "_pom.xml")
    //    val temp = File("test.pom")
    temp.writeText(pom)
    val exec = Runtime.getRuntime().exec("mvn -f ${temp.absolutePath} ${goal}")

    return BufferedReader(InputStreamReader(exec.inputStream)).
            lines().toArray().map { it.toString() }
}

val mavenResult = runMaven(pom, "dependency:build-classpath")

val classPath = mavenResult.dropWhile { !it.startsWith("[INFO] Dependencies classpath:") }.drop(1).first()


// add classpath to cache
cacheFile.appendText(depsHash + " " + classPath + "\n")

// print the classpath and exit
println(classPath)
exitProcess(0)
