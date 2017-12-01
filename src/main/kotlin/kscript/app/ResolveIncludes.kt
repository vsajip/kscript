package kscript.app

import java.io.File
import java.io.FileNotFoundException
import java.net.URL

/**
 * @author Holger Brandl
 * @author Ilan Pillemer
 */

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(template: File): File = resolveIncludesInternal(template)

private fun resolveIncludesInternal(template: File): File {
    val IMPORT_TEXT = "import "

    val scriptLines = template.readText().lines()

    if (!scriptLines.any { isIncludeDirective(it) }) {
        return template
    }

    val sb = StringBuilder()

    // collect up the set of imports in this
    val imports = emptySet<String>().toMutableSet()

    scriptLines.forEach {
        if (isIncludeDirective(it)) {
            val include = extractIncludeTarget(it)

            val includeURL = when {
                include.startsWith("http://") -> URL(include)
                include.startsWith("https://") -> URL(include)
                include.startsWith("./") || include.startsWith("../") -> File(template.parentFile, include).toURI().toURL()
                include.startsWith("/") -> File(include).toURI().toURL()
                else -> File(template.parentFile, include).toURI().toURL()
            }

            try {
                // collect the import or emit
                includeURL.readText().lines().forEach {
                    if (it.startsWith(IMPORT_TEXT)) {
                        imports.add(it)
                    } else {
                        sb.appendln(it)
                    }
                }
            } catch (e: FileNotFoundException) {
                errorMsg("Failed to resolve //INCLUDE '${include}'")
                System.err.println(e.message?.lines()!!.map { it.prependIndent("[ERROR] ") })
                quit(1)
            }
        } else if (it.startsWith(IMPORT_TEXT)) {
            imports.add(it)
        } else if (!it.startsWith("#!/")) {
            // if its not an include directive or an import or a bang line, emit as is
            sb.appendln(it)
        }
    }

    val incResolved = StringBuilder().apply {
        imports.map { appendln(it) }
        appendln(sb)
    }

    return createTmpScript(incResolved.toString())
}


private const val INCLUDE_ANNOT_PREFIX = "@file:Include("

private fun isIncludeDirective(line: String) = line.startsWith("//INCLUDE") || line.startsWith(INCLUDE_ANNOT_PREFIX)


private fun extractIncludeTarget(incDirective: String) = when {
    incDirective.startsWith(INCLUDE_ANNOT_PREFIX) -> incDirective
        .replaceFirst(INCLUDE_ANNOT_PREFIX, "")
        .split(")")[0].trim(' ', '"')
    else -> incDirective.split("[ ]+".toRegex()).last()
}



// basic launcher used for testing
fun main(args: Array<String>) {
    System.err.println(resolveIncludes(File(args[0])))
}

/**
# Usage Example
cd $KSCRIPT_HOME
gradle shadowJar
resolve_inc() { kotlin -classpath build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.ResolveIncludesKt "$@";}
resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts
cat $(resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts 2>&1)
 */