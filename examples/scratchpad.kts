#!/usr/bin/env kscript

import de.mpicbg.scicomp.kscript.processLines

println("ok")
println(args.joinToString())

// test


//generateSequence() { readLine() }.map {
//File(args[0]).readLines().map {}

java.io.File(args[0]).useLines {
    it.map {
        if (!it.startsWith(">")) {
            it.substring(0, 20)
        } else it
    }.forEach { println(it) }
}


java.io.File(args[0]).processLines { it + "foo" }

