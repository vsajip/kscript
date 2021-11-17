package kscript.app.parser

import kscript.app.model.Code
import kscript.app.model.Directive
import kscript.app.model.Section

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
            val parsedAnnotations = directive.processor(line)

            if (parsedAnnotations.isNotEmpty()) {
                return Section(line, parsedAnnotations)
            }
        }
        return Section(line, listOf(Code))
    }
}
