package kscript.app.resolver

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class IntegrationTests {

    @Test
    fun `it should run kscript and resolve dependencies`(){
        // clear .m2 cache
        val log4jCached = File(System.getProperty("user.home"), ".m2/repository/log4j/log4j/1.2.14/")
        if(log4jCached.isDirectory) {
            System.err.println("Cleaning up cached .m2 copy of log4j")
            log4jCached.deleteRecursively()
        }

        //clear kscript cache
        kscript.app.main(arrayOf("--clear-cache"))

        // run as when being on commandline
//        val observed = captureOutput {
            kscript.app.main(arrayOf("test/resources/depends_on_annot.kts"))
//        }.stdout.replace(System.getProperty("user.home")!!, "")

//        Assertions.assertEquals("kotlin  -classpath /.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar:/.m2/repository/com/offbytwo/docopt/0.6.0.20150202/docopt-0.6.0.20150202.jar:/.kscript/cache/jar_7cb43f5abd032b31680d6649e4ef6a87/scriplet.jar:/.sdkman/candidates/kotlin/1.5.31/lib/kotlin-script-runtime.jar Main_Depends_on_annot", observed)
        // todo complete the assertion to fix the test
    }
}


internal data class CapturedOutput(val stdout: String, val stderr: String)

internal fun captureOutput(expr: () -> Any): CapturedOutput {
    val origOut = System.out
    val origErr = System.err
    // https://stackoverflow.com/questions/216894/get-an-outputstream-into-a-string

    val baosOut = ByteArrayOutputStream()
    val baosErr = ByteArrayOutputStream()

    System.setOut(PrintStream(baosOut));
    System.setErr(PrintStream(baosErr));


    // run the expression
    expr()

    val stdout = String(baosOut.toByteArray()).trim()
    val stderr = String(baosErr.toByteArray()).trim()

    System.setOut(origOut)
    System.setErr(origErr)

    return CapturedOutput(stdout, stderr)
}