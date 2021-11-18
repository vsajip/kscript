package kscript.app.util

object Logger {
    var silentMode = false

    fun info(msg: String) = printer(msg)

    fun infoMsg(message: String) = printer(if (!silentMode) "[kscript] $message" else "")

    fun warnMsg(msg: String) = printer("[kscript] [WARN] $msg")

    fun errorMsg(msg: String) = printer("[kscript] [ERROR] $msg")

    fun errorMsg(exception: Exception): String {
        val message = exception.message ?: exception.javaClass.simpleName
        return printer(message.lines().joinToString("\n") { it.prependIndent("[kscript] [ERROR] ") })
    }

    private fun printer(message: String) = message.also { System.err.println(it) }
}
