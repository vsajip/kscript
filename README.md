## kscript - Having fun with Kotlin scripting

[![Build Status](https://travis-ci.org/holgerbrandl/kscript.svg?branch=master)](https://travis-ci.org/holgerbrandl/kscript)

Enhanced scripting support for [Kotlin](https://kotlinlang.org/) on *nix-based systems.

Kotlin has a limited support for scripting already but it's not (yet) feature-rich enough to be fun.

In particular this wrapper around `kotlinc-jvm -script` adds
* Compiled script caching (using md5 checksums)
* Automatic dependency resolution via gradle-style resource locators

## Installation

To use `kscript` just Kotlin and Maven are required. To [install Kotlin](https://kotlinlang.org/docs/tutorials/command-line.html) we recommend [sdkman](http://sdkman.io/install.html):
```
curl -s "https://get.sdkman.io" | bash  # install sdkman
source ~/.bash_profile                  # add sdkman to PATH
sdk install kotlin                      # install Kotlin
sdk install maven                       # install Maven
```

Once kotlin is installed, simply download [kscript](kscript)  to your `~/bin` with:
```bash
curl -L -o ~/bin/kscript https://git.io/vaoNi && chmod u+x ~/bin/kscript
```



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
//DEPS com.offbytwo:docopt:0.6.0.20150202,log4j:log4j:1.2.14

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
`kscript` will read dependencies from all lines in a script that start with `//DEPS` (if any). Multiple dependencies can be split by comma, space or semicolon.

Note: It might feel more intuitive to provide  dependencies as an argument to `kscript`, however because of the way the shebang line works on Linux this is not possible.



Inline Usage
============

You can even inline `kscript` solutions into larger scripts, because `kscript` can read from stdin as well. So, depending on your preference you could simply pipe a kotlin snippet into `kscript`

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
//DEPS com.offbytwo:docopt:0.6.0.20150202 log4j:log4j:1.2.14

import org.docopt.Docopt
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

println("hello again")
EOF
```

Finally (for sake of completeness) it also works with process substitution and for sure you can always provide additional arguments (exposed as `args : Array<String>` within the script)
```{bash}
kscript - arg1 arg2 arg3 <(echo 'println("k-onliner")')
```

Inlined _kscripts_ are also cached based on `md5` checksum, so running the same snippet again will use a cached jar (sitting in `$TMPDIR`).


Tool repositories
=================

To support remote scriplet repositories, `kscript` can also work with URLs. Consider the following [hello-world-gist-scriptlet](https://github.com/holgerbrandl/kscript/blob/master/examples/url_example.kts) which is hosted on github (but any URL would work). To run it locally as a tool simply refer to it (here using the shortened [raw-URL](https://raw.githubusercontent.com/holgerbrandl/kscript/master/examples/url_example.kts) of the script for better readability)

```bash
kscript https://git.io/v1cG6 my argu ments 
```

To streamline the usage, the first part could be even aliased:
```bash
alias hello_kscript="kscript https://git.io/v1cG6"
hello_kscript my argu ments
```
Via this mechanism, `kscript` allows for easy integration of remotely hosted (mini) programs into data workflows.

URL-scripts are cached locally to speed up processing, and `kscript --clear-cache` can be used to wipe the cache if needed.

See this [blogpost](http://holgerbrandl.github.io/kotlin/2016/12/02/mini_programs_with_kotlin.html) for a more extensive overview about URL support in `kscript`.


References
============

The initial version of `kscript` was kindly contributed by Oscar Gonzalez.

`kscript` is inspired by [kotlin-script](https://github.com/andrewoma/kotlin-script) which is another great way to do scripting in Kotlin. `kotlin-script` has more options compared to `kscript`, but the latter is conceptually cleaner (no code wrapping) and more simplistic.


`kscript` works better with [Intellij](https://www.jetbrains.com/idea/), since extended multi-line shebang-headers are not (yet?) supported by Intellij's' Kotlin plugin (even if the kotlin-script parser seems to be able to handle them).  However,  for dependency resolution, `kotlin script` relies on such mutli-line shebang headers (see [here](https://github.com/andrewoma/kotlin-script#mvncp)). In contrast, since `kscript` just works with just a standard shebang line, code parsing works very well in Intellij.

FAQ
============

### How to adjust the memory the JVM running my scriptlets?

`kscript` allows to provide a `//KOTLIN_OPTS` line followed by parameters passed on to `kotlin` similar to how dependencies are defined:
```kotlin
#!/usr/bin/env kscript
//KOTLIN_OPTS -J-Xmx5g  -J-server

println("Hello from Kotlin with 5g of heap memory in server mode!")
```


Issues
=======

Feel welcome to submit ideas and suggestions.

Related Projects
================

* [coursier](https://github.com/alexarchambault/coursier) - Pure Scala Artifact Fetching. Potential more powerful replacement for `expandcp.kts`
* [kotlin-script](https://github.com/andrewoma/kotlin-script) - Support for using Kotlin as a scripting language



