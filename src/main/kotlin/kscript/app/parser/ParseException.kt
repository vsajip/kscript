package kscript.app.parser

class ParseException(lineText: String, exceptionMessage: String) : RuntimeException(lineText + "\n" + exceptionMessage)
