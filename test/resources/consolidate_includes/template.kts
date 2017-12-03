#!/usr/bin/env kscript

//DEPS com.eclipsesource.minimal-json:minimal-json:0.9.4

//INCLUDE file1.kts
//INCLUDE file2.kts
//INCLUDE file3.kts

@file:DependsOn("log4j:log4j:1.2.14")

include_5()
importMoreStuff()