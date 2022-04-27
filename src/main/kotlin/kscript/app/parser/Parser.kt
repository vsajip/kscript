package kscript.app.parser

import kscript.app.model.Code
import kscript.app.model.Section

class Parser {
    private val annotationParsers = listOf(
        LineParser::parseSheBang,
        LineParser::parseInclude,
        LineParser::parseDependency,
        LineParser::parseRepository,
        LineParser::parseEntry,
        LineParser::parseKotlinOpts,
        LineParser::parseCompilerOpts,
        LineParser::parseImport,
        LineParser::parsePackage,
    )

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
        for (parser in annotationParsers) {
            val parsedAnnotations = parser(line)

            if (parsedAnnotations.isNotEmpty()) {
                return Section(line, parsedAnnotations)
            }
        }
        return Section(line, listOf(Code))
    }
}
