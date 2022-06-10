package kscript.app.model

import kscript.app.util.OsPath

data class Config(
    val osType: OsType,
    val selfName: String,
    val kscriptDir: OsPath,
    val customPreamble: String,
    val intellijCommand: String,
    val gradleCommand: String,
    val kotlinHome: OsPath,
    val classPathSeparator: Char,
    val hostPathSeparatorChar: Char,
    val homeDir: OsPath,
    val kotlinOptsEnvVariable: String,
    val repositoryUrlEnvVariable: String,
    val repositoryUserEnvVariable: String,
    val repositoryPasswordEnvVariable: String,
) {
    companion object {
        fun builder() = ConfigBuilder()
    }
}
