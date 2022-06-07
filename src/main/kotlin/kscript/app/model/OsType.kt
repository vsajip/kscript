package kscript.app.model

enum class OsType(val osName: String) {
    LINUX("linux"), DARWIN("darwin"), WINDOWS("windows"), CYGWIN("cygwin"), MSYS("msys"), FREEBSD("freebsd");

    fun isUnixLike() = (this == LINUX || this == DARWIN || this == FREEBSD || this == CYGWIN || this == MSYS)
    fun isWindowsLike() = (this == WINDOWS)
    fun isUnixHostedOnWindows() = (this == CYGWIN || this == MSYS)

    companion object {
        fun findOrThrow(name: String): OsType = find(name) ?: throw IllegalArgumentException("Unsupported OS: '$name'")

        // Exact comparison (it.osName.equals(name, true)) seems to be not feasible as there is also e.g. "darwin21"
        // "darwin19", "linux-musl" (for Docker Alpine), "linux-gnu" and maybe even other osTypes. It seems though that
        // startsWith() is covering all cases.
        // https://github.com/holgerbrandl/kscript/issues/356
        fun find(name: String): OsType? = values().find { name.startsWith(it.osName, true) }
    }
}
