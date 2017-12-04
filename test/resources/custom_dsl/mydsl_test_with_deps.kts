#!/usr/bin/env mydsl

@file:DependsOn("com.beust:klaxon:0.24")


// instantiate something from extra dep
import com.beust.klaxon.Parser

val p = Parser()

// call a preamble included fundef
included()

// use a dsl context variable
println("foo" + foo)

