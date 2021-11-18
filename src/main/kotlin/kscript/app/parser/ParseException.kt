package kscript.app.parser

class ParseException(private val line: String, exceptionMessage: String) : RuntimeException(exceptionMessage)
