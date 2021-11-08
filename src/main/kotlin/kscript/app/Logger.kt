package kscript.app

object Logger {
    var silentMode = false

    fun info(msg: String) = System.err.println(msg)

    fun infoMsg(msg: String) {
        if (!silentMode) System.err.println("[kscript] $msg")
    }

    fun warnMsg(msg: String) = System.err.println("[kscript] [WARN] $msg")

    fun errorMsg(msg: String) = System.err.println("[kscript] [ERROR] $msg")
}
