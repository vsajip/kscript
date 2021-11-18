package kscript.app.util

object Logger {
    var silentMode = false

    fun info(msg: String) = printer(msg)

    fun infoMsg(message: String) = printer(message, !silentMode, "kscript")

    fun warnMsg(msg: String) = printer(msg, true, "kscript", "WARN")

    fun errorMsg(msg: String) = printer(msg, true, "kscript", "ERROR")

    fun errorMsg(exception: Exception) =
        printer(exception.message ?: exception.javaClass.simpleName, true, "kscript", "ERROR")

    private fun printer(message: String, shouldPrint: Boolean = true, vararg tags: String): String {
        var prefix = tags.joinToString(" ") { "[$it]" }

        if (prefix.isNotBlank()) {
            prefix += " "
        }

        val msg = message.lines().joinToString("\n") { it.prependIndent(prefix) }

        if (shouldPrint) {
            System.err.println(msg)
        }

        return msg
    }
}
