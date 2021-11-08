package kscript.app.code

import org.intellij.lang.annotations.Language

object Scriptlets {
    @Language("sh")
    val bootstrapHeader = """
        #!/bin/bash
        
        //usr/bin/env echo '
        /**** BOOTSTRAP kscript ****\'>/dev/null
        command -v kscript >/dev/null 2>&1 || curl -L "https://git.io/fpF1K" | bash 1>&2
        exec kscript $0 "$@"
        \*** IMPORTANT: Any code including imports and annotations must come after this line ***/
        
    """.trimIndent()

    val textProcessingPreamble = """
        //DEPS com.github.holgerbrandl:kscript-support-api:1.2.5

        import kscript.text.*
        val lines = resolveArgFile(args)

        """.trimIndent()
}
