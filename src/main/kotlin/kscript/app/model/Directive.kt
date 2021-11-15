package kscript.app.model

import kscript.app.resolver.LineParser

enum class Directive(val processor: (String) -> Section?) {
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
