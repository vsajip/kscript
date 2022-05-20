package kscript.app.model

enum class OsType(val osName: String) {
    LINUX("linux-gnu"), DARWIN("darwin"), WINDOWS("windows"), CYGWIN("cygwin"), MSYS("msys"), FREEBSD("freebsd");

    fun isUnixLike() = (this == LINUX || this == DARWIN || this == FREEBSD)
    fun isWindowsLike() = (this == WINDOWS)
    fun isUnixHostedOnWindows() = (this == CYGWIN || this == MSYS)

    companion object {
        fun findOrThrow(name: String) =
            //Exact comparison (it.osName.equals(name, true)) seems to be not feasible as there is also e.g. "darwin21"
            //and maybe even other osTypes, but specific versions of os'es shouldn't belong to OsType.
            //https://github.com/holgerbrandl/kscript/issues/356
            values().find { name.contains(it.osName, true) } ?: throw IllegalArgumentException("Unsupported OS: $name")
    }
}
