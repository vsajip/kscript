#!/usr/bin/env kscript

//DEPS log4j:log4j:1.2.14
//ENTRY  Foo

package test

class Foo{
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            println("made it!")
            org.apache.log4j.Logger.getRootLogger()
        }
    }
}


