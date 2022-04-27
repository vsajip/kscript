package kscript.app.util

import kscript.app.util.Logger.info
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.UnknownHostException
import java.util.*

object VersionChecker {
    /** Determine the latest version by checking github repo and print info if newer version is available. */
    fun versionCheck(currentVersion: String) {
        //    val latestVersion = fetchFromURL("https://git.io/v9R73")?.useLines {
        //    val kscriptRawReleaseURL= "https://git.io/v9R73"
        // todo use the actual kscript.app.Kscript.kt here to infer version
        val kscriptRawReleaseURL = "https://raw.githubusercontent.com/holgerbrandl/kscript/releases/kscript"

        val latestVersion = try {
            BufferedReader(InputStreamReader(URL(kscriptRawReleaseURL).openStream())).useLines {
                it.first { it.startsWith("KSCRIPT_VERSION") }.split("=")[1]
            }
        } catch (e: UnknownHostException) {
            return // skip version check here, since the use has no connection to the internet at the moment
        }

        fun padVersion(version: String) = try {
            var versionNumbers = version.split(".").map { Integer.valueOf(it) }
            // adjust versions without a patch-release
            while (versionNumbers.size != 3) {
                versionNumbers = versionNumbers + 0
            }

            java.lang.String.format("%03d%03d%03d", *versionNumbers.toTypedArray())
        } catch (e: MissingFormatArgumentException) {
            throw IllegalArgumentException("Could not pad version $version", e)
        }

        if (padVersion(latestVersion) > padVersion(currentVersion)) {
            info("""A new version (v${latestVersion}) of kscript is available.""")
        }
    }
}
