package kscript.app.model

import kscript.app.parser.LineParser

enum class Directive(val processor: (String) -> List<Annotation>) {
    SheBang(LineParser::parseSheBang),
    Include(LineParser::parseInclude),
    Dependency(LineParser::parseDependency),
    Repository(LineParser::parseRepository),
    Entry(LineParser::parseEntry),
    KotlinOpts(LineParser::parseKotlinOpts),
    CompileOpts(LineParser::parseCompilerOpts),
    Import(LineParser::parseImport),
    Package(LineParser::parsePackage),
}
