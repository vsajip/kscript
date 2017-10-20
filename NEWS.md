## v2.0.X

* Reimplemented in kotlin (fixes [#36](https://github.com/holgerbrandl/kscript/issues/36))
* Added cygwin support (fixes [#39](https://github.com/holgerbrandl/kscript/issues/39))
* Added `//INCLUDE` directive (fixes [#34](https://github.com/holgerbrandl/kscript/issues/34)
* Fixed: interactive mode is not correctly started when using stdin as script argument ([#40](https://github.com/holgerbrandl/kscript/issues/40)
* Fixed compatibility with java9 ([#41](https://github.com/holgerbrandl/kscript/issues/41))


## v1.5.1

* Fixed `--self-update`
* More robust self-update on OSses with file-locking (e.g. windows)



## v1.5

* removed `curl` dependency
* more streamlined dependency lookup


## v1.4

Major new features
* Redesigned [support library](https://github.com/holgerbrandl/kscript-support-api) for streamlined tabular data processing. See [here](http://holgerbrandl.github.io/kotlin/2017/05/08/kscript_as_awk_substitute.html) for an overview.


## v1.3

Major new features

* Dramatically reduced overhead by using dependency lookup cache more efficiently. After the initial scriptlet-jar-building, `kscript` runs with almost **zero overhead** now (fixes  [#4](https://github.com/holgerbrandl/kscript/issues/4))
* Dependencies can now declared in multiple lines for better readability (fixes [#2](https://github.com/holgerbrandl/kscript/issues/2))
* Automatic inclusion of support library for one-liners (fixes [#19](https://github.com/holgerbrandl/kscript/issues/19))
* Direct script arguments `kscript 'println("hello kotlin")'` (fixes [#18](https://github.com/holgerbrandl/kscript/issues/18))
* More robust dependency resolution with more informative error messages


Support API improvements
* Kotlin DocOpt helpers to build CLIs ([example](https://github.com/holgerbrandl/kscript-support-api/blob/master/src/test/kotlin/kscript/test/DocOptTest.kt))
* New [utilities](https://github.com/holgerbrandl/kscript-support-api/blob/master/src/main/kotlin/kscript/StreamUtil.kt) to automatically resolve arguments files and stdin to `Sequence<String` for by-line processing

Other changes
* Allow dependencies to be declared in multiple lines prefixed by `//DEPS` (fixes [#2](https://github.com/holgerbrandl/kscript/issues/2))
* To ensure long-term stability of `kscript` we've added a suite of [unit tests](test/TestsReadme.md). The repository tested continuously by [Travis CI](https://travis-ci.org/holgerbrandl/kscript)
* Cache directory is now `~/.kscript`
* More heuristics to guess `KOTLIN_HOME`
* Cache cleanup `--clear-cache` now applies to jars, scripts, urls, and cached dependency lookups


## v1.2 

* Fixed compatibility with [Kotlin v1.1](https://kotlinlang.org/docs/reference/whatsnew11.html)
 (fixes [#15](https://github.com/holgerbrandl/kscript/issues/15))
* Added `-i` to dump interactive console command incl deps (fixes [#10](https://github.com/holgerbrandl/kscript/issues/10))
* Compile jars should go to TEMP (fixes [#13](https://github.com/holgerbrandl/kscript/issues/13))
* started test-suite 

## v1.1

* Support for stdin and process substitution as script source. See [examples](examples/unit_tests.sh)
* versioning and auto-update
* basic command-line help
* Added support for `KOTLIN_OPTS` (see [#8](https://github.com/holgerbrandl/kscript/issues/8))
* Added CLI help to `resdeps.kts`
* Added option to clear dependency lookup cache: `resdeps.kts --clear-cache`

## v1.0

Initial Release