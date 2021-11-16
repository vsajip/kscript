package org.docopt

import kscript.app.quit
import java.io.File

/**
 * @author Holger Brandl
 */

/** Simple Kotlin facade for org.org.docopt.Docopt.Docopt(java.lang.String) .*/
class DocOptWrapper(args: Iterable<String>, private val usage: String) {

    val parsedArgs = try {
        Docopt(usage).withExit(false).parse(args.toList())
    } catch (e: DocoptExitException) {
        // mimic docopts exit behavior but using process-substituion handler at the end
        val ps = System.err

        val message = e.message

        if (message != null) {
            ps.println(message)
        }

        if (e.printUsage) {
            ps.println(usage)
        }

        quit(e.exitCode)
    }

    private val myDO by lazy {
        parsedArgs.map {
            it.key.removePrefix("--").replace("[<>]".toRegex(), "") to it.value
        }.toMap()
    }

    fun getString(key: String) = myDO[key]!!.toString()
    fun getStrings(key: String) = (myDO[key]!! as List<*>).map { it as String }

    fun getFile(key: String) = File(getString(key))
    fun getFiles(key: String) = getStrings(key).map { File(it) }


    fun getInt(key: String) = myDO[key]!!.toString().toInt()

    fun getNumber(key: String) = myDO[key]!!.toString().toFloat()

    fun getBoolean(key: String) = myDO[key]!!.toString().toBoolean()

    override fun toString(): String {
        return parsedArgs.toString()
    }
}
