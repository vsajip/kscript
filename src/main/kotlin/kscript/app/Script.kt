package kscript.app

import java.io.File

/* Immutable script class */
class Script(val script: List<String>) : Iterable<String> {

    constructor(scriptFile: File) : this(scriptFile.readLines())


    override fun toString(): String = script.joinToString("\n")


    override fun iterator(): Iterator<String> = script.iterator()


    fun stripShebang(): Script = script.filterNot { it.startsWith("#!/") }.let { Script(it) }


    fun createTmpScript() = createTmpScript(toString())


    fun injectAfterPckgStmnt(inject: () -> String): Script {
        val indexOfFirst = script.indexOfFirst { it.startsWith(PACKAGE_STATEMENT_PREFIX) }

        val withInject = if (indexOfFirst < 0) {
            inject().lines() + stripShebang().script
        } else {
            script.toMutableList().apply { addAll(indexOfFirst + 1, inject().lines()) }
        }

        return Script(withInject).consolidateStructure()
    }


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
            // preserve package statement if present
            script.firstOrNull { it.startsWith(PACKAGE_STATEMENT_PREFIX) }?.let {
                appendln(it)
            }

            with(annotations) {
                sorted().map(String::trim).distinct().map { appendln(it) }
                if (isNotEmpty()) appendln()
            }

            with(imports) {
                sorted().map(String::trim).distinct().map { appendln(it) }
                if (isNotEmpty()) appendln()
            }


            // append actual script
            codeBits.forEach { appendln(it) }
        }

        return Script(consolidated.lines())
    }
}