package kscript.app.resolver

import kscript.app.model.Code
import kscript.app.model.Directive
import kscript.app.model.Section

class ParseError(lineText: String, exceptionMessage: String) : RuntimeException(lineText + "\n" + exceptionMessage)

class Parser {
    fun parse(string: String): List<Section> {
        val codeTextAsLines = string.lines()

        val sections = mutableListOf<Section>()

        for (line in codeTextAsLines) {
            val section = parseLine(line)
            sections += section
        }
        return sections
    }

    private fun parseLine(line: String): Section {
        for (directive in Directive.values()) {
            val parsedLine = directive.processor(line)
            if (parsedLine != null) {
                return parsedLine
            }
        }
        return Code(line)
    }
}
