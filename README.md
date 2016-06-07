## kscript - Having fun with Kotlin scripting


Easy-to-use scripting support for [Kotlin](https://kotlinlang.org/) on *nix-based systems.

Kotlin has a limited support for scripting already but it's not (yet) feature-rich enough to be fun.

In particular this wrapper around `kotlinc-jvm -script` adds
* Compiled script caching (using md5 checksums)
* Automatic dependency resolution via gradle-style resource locators

## Installation

Simply download [kscript](kscript)  to your `~/bin` with:
```bash
curl -L -o ~/bin/kscript https://git.io/vaoNi && chmod u+x ~/bin/kscript
```

Just `kotlinc-jvm` is required to be in your `PATH`. It will be once you have [installed Kotlin](https://kotlinlang.org/docs/tutorials/command-line.html).

## Usage

To use `kscript` just specify it in the shebang line of your Kotlin scripts:

```kotlin
#!/usr/bin/env kscript

println("Hello from Kotlin!")
for (arg in args) {
    println("arg: $arg")
}
```

To specify dependencies simply use gradle-style locators. Here's an example using [docopt](https://github.com/docopt/docopt.java) and [log4j](http://logging.apache.org/log4j/2.x/)

```kotlin
#!/usr/bin/env kscript
//DEPS org.docopt:docopt:0.6.0-SNAPSHOT,log4j:log4j:1.2.14

import org.docopt.Docopt
import java.util.*


val usage = """
Use this cool tool to do cool stuff
Usage: cooltool.kts [options] <igenome> <fastq_files>...

Options:
 --gtf <gtfFile>     Custom gtf file instead of igenome bundled copy
 --pc-only           Use protein coding genes only for mapping and quantification
"""

val doArgs = Docopt(usage).parse(args.toList())

println("Hello from Kotlin!")
println("Parsed script arguments are: \n" + doArgs.joinToString())
```
`kscript` will read dependencies from the *first* line in a script starting with `//DEPS` (if any). Multiple dependencies can be split by comma, space or semicolon.

Note: It might feel more intuitive to provide  dependencies as an argument to `kscript`, however because of the way the shebang line works on Linux this is not possible.



Inline Usage
============

You can even inline `kscript` solutions into larger scripts, because `kscript` can read from stdin as well. So, depdending your preference you could simply pipe a kotlin snippet into `kscript`

```{bash}
echo '
println("hello kotlin")
' |  kscript -
```


or do the same using `heredoc` (preferred solution) which gives you some more flexibility to also use single quotes in your script:
```{bash}
kscript - <<"EOF"
println("hello kotlin and heredoc")
EOF
```

Since the piped content is considered as a regular script it can also have dependencies
```{bash}
kscript - <<"EOF"
//DEPS org.docopt:docopt:0.6.0-SNAPSHOT log4j:log4j:1.2.14

import org.docopt.Docopt
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

println("hello again")
EOF
```

Finally (for sake of completeness) it also works with process substitution
```{bash}
kscript  <(echo 'println("k-onliner")')
```

Inlined _kscripts_ are also cached based on `md5` checksum, so running the same snippet again will use a cached jar (sitting in `$TMPDIR`).


References
============

The initial version of `kscript` was kindly contributed by Oscar Gonzalez.

`kscript` is inspired by [kotlin-script](https://github.com/andrewoma/kotlin-script) which is another great way to do scripting in Kotlin. `kotlin-script` has more options compared to `kscript`, but the latter is conceptually cleaner (no code wrapping) and more simplistic.


`kscript` works better with [Intellij](https://www.jetbrains.com/idea/), since extended multi-line shebang-headers are not (yet?) supported by Intellij's' Kotlin plugin (even if the kotlin-script parser seems to be able to handle them).  However,  for dependency resolution, `kotlin script` relies on such mutli-line shebang headers (see [here](https://github.com/andrewoma/kotlin-script#mvncp)). In contrast, since `kscript` just works with just a standard shebang line, code parsing works very well in Intellij.

Issues
=======

Feel welcome to submit ideas and suggestions.

Related Projects
================

* [coursier](https://github.com/alexarchambault/coursier) - Pure Scala Artifact Fetching. Potential more powerful replacement for `expandcp.kts`
* [kotlin-script](https://github.com/andrewoma/kotlin-script) - Support for using Kotlin as a scripting language



