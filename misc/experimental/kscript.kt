package kscript.app

// A kscript reimplementation in kotlin

fun main(args: Array<String>) {

    /**
     * @author Holger Brandl
     */
    val kotlin_opts=""
    val jarFile= "/Users/brandl/.kscript/tester.e5dbe66253ca2a6d.jar"
    val KOTLIN_HOME="/Users/brandl/.sdkman/candidates/kotlin/current"
    val classpath=""
    val execClassName = "Main_Tester"


    // todo

    // todo fix args here
    println("kotlin ${kotlin_opts} -classpath ${jarFile}:${KOTLIN_HOME}/lib/kotlin-script-runtime.jar:${classpath} ${execClassName} \"\$@\" ")
}