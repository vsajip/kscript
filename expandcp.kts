#!/usr/bin/env kscript

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

//val args = arrayOf("org.docopt:docopt:0.6.0-SNAPSHOT", "log4j:log4j:1.2.14")

if (args.isNotEmpty() && listOf("--help", "-help", "-h").contains(args[0])) {
    System.err.println("""expandcp.kts resolves a space separated list of gradle-style resource locators into a
classpath suitable for use with 'java -cp' or 'kotlin -cp'. expandcp.kts will use maven to resolve dependencies.

For details see https://github.com/holgerbrandl/kscript

## Example

expandcp.kts org.apache.commons:commons-csv:1.3 log4j:log4j:1.2.14

## Features

* Support for transitive Maven dependencies
* Caching of dependency requests (cached requests take just around 30ms). Use `expandcp.kts --clear-cache` to
  clear this cache in case the dependency tree has changed

## Copyright

2017 Holger Brandl

Inspired by mvncp created by Andrew O'Malley
""")

    exitProcess(0)
}


// Use cached classpath from previous run if present
val DEP_LOOKUP_CACHE_FILE = File("/tmp/.kscript_deps_cache.txt")


if (args.size == 1 && args[0] == "--clear-cache") {
    if (DEP_LOOKUP_CACHE_FILE.exists()) DEP_LOOKUP_CACHE_FILE.delete()
    System.err.println("Cleaning up dependency lookup cache... Done!")
    exitProcess(0)
}


val depIds = args


// if no dependencies were provided we stop here
if (depIds.isEmpty()) {
    exitProcess(0)
}

val depsHash = depIds.joinToString(";")


if (DEP_LOOKUP_CACHE_FILE.isFile()) {
    val cache = DEP_LOOKUP_CACHE_FILE.
            readLines().filter { it.isNotBlank() }.
            associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

    if (cache.containsKey(depsHash)) {
        println(cache.get(depsHash))
        exitProcess(0)
    }
}


val depTags = depIds.map {
    val splitDepId = it.split(":")

    if (!listOf(3, 4).contains(splitDepId.size)) {
        System.err.println("invalid dependency locator: ${it}")
        System.err.println("Expected format is groupId:artifactId:version[:classifier]")
        exitProcess(1)
    }

    """
    <dependency>
            <groupId>${splitDepId[0]}</groupId>
            <artifactId>${splitDepId[1]}</artifactId>
            <version>${splitDepId[2]}</version>
            ${if (splitDepId.size == 4) "<classifier>" + splitDepId[2] + "<classifier>" else ""}
    </dependency>
    """
}

val pom = """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>kscript</groupId>
    <artifactId>maven_template</artifactId>
    <version>1.0</version>

     <repositories>
        <repository>
            <id>jcenter</id>
            <url>http://jcenter.bintray.com/</url>
        </repository>
    </repositories>

    <dependencies>
    ${depTags.joinToString("\n")}
    </dependencies>
</project>
"""


fun runMaven(pom: String, goal: String): Iterable<String> {
    val temp = File.createTempFile("__expandcp__temp__", "_pom.xml")
    temp.writeText(pom)
    val exec = Runtime.getRuntime().exec("mvn -f ${temp.absolutePath} ${goal}")
    return BufferedReader(InputStreamReader(exec.inputStream)).
            lines().toArray().map { it.toString() }
}

val mavenResult = runMaven(pom, "dependency:build-classpath")

//println(pom)
//println(mavenResult.joinToString("\n"))

// The following artifacts could not be resolved: log4ja:log4ja:jar:9.8.87, log4j:log4j:jar:9.8.105: Could not

// Check for errors (e.g. when using non-existing deps expandcp.kts log4j:log4j:1.2.14 org.docopt:docopt:22.3-MISSING)
mavenResult.filter { it.startsWith("[ERROR]") }.find { it.contains("Could not resolve dependencie") }?.let {
    System.err.println("Failed to lookup dependencies. Maven reported the following error:")
    System.err.println(it)
    //    val matchResult = "Failure to find ([^ ]*)".toRegex().find(it)
    //    println(matchResult.toString())
    //    val failedDep = matchResult !!.groupValues[1]
    //    System.err.println("Failed to resolve: ${failedDep}")

    exitProcess(1)
}


// Extract the classpath from the maven output
//System.err.println(mavenResult)
//mavenResult.dropWhile { !it.contains("Dependencies classpath:") }.forEach { println(">>>> ${it}") }

val classPath = mavenResult.dropWhile { !it.contains("Dependencies classpath:") }.drop(1).firstOrNull()

if (classPath == null) {
    System.err.println("Failed to lookup dependencies. Check dependency locators or file a bug on https://github.com/holgerbrandl/kscript")
    exitProcess(1)
}


// Add classpath to cache
DEP_LOOKUP_CACHE_FILE.appendText(depsHash + " " + classPath + "\n")

// Print the classpath
println(classPath)
