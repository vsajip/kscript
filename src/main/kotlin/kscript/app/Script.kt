package kscript.app

import java.io.File

/* Immutable script class */
data class Script(val script: List<String>, val extension: String) : Iterable<String> {

    constructor(scriptFile: File) : this(scriptFile.readLines(), scriptFile.extension)


    override fun toString(): String = script.joinToString("\n")


    override fun iterator(): Iterator<String> = script.iterator()


    fun stripShebang(): Script = script.filterNot { it.startsWith("#!/") }.let { copy(it) }


    fun createTmpScript() = createTmpScript(toString(), extension)


    fun prependWith(preamble: String): Script = copy(script = preamble.lines() + script).consolidateStructure()


    fun consolidateStructure(): Script {
        val codeBits = mutableListOf<String>()
        val imports = emptySet<String>().toMutableSet()
        val annotations = emptySet<String>().toMutableSet()

        stripShebang().forEach {
            if (it.startsWith(IMPORT_STATMENT_PREFIX)) {
                imports.add(it)
            } else if (isKscriptAnnotation(it)) {
                annotations.add(it)
            } else if (!it.startsWith(PACKAGE_STATEMENT_PREFIX)) {
                // if its not an annotation directive or an import, emit as is
                codeBits += it
            }
        }

        val consolidated = StringBuilder().apply {
            // file annotations have to be on top of everything, just switch places between your annotation and package
            with(annotations) {
                sorted().map(String::trim).distinct().map { appendln(it) }
                // kotlin seems buggy here, so maybe we need to recode annot-directives into comment directives
                if (isNotEmpty()) appendln()
            }

            // restablish the package statement if present
            script.firstOrNull { it.startsWith(PACKAGE_STATEMENT_PREFIX) }?.let {
                appendln(it)
            }

            with(imports) {
                sorted().map(String::trim).distinct().map { appendln(it) }
                if (isNotEmpty()) appendln()
            }

            // append actual script
            codeBits.forEach { appendln(it) }
        }

        return copy(script = consolidated.lines())
    }
}