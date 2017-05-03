

## v1.3

Major new features

* Dramatically reduced overhead by using dependency lookup cache more efficiently. After the initially scriptlet-jar-building, `kscript` runs with almost zero overhead now (fixes  [#4](https://github.com/holgerbrandl/kscript/issues/4))
* Dependencies can now declared in multiple lines for better readability (fixes [#2](https://github.com/holgerbrandl/kscript/issues/2))
* Automatic inclusion of support library for one-liners
* Direct script arguments `kscript 'println("hello kotlin")'`
* More robust dependency resolution with more informative error messages

Other changes
* Allow dependencies to be declared in multiple lines prefixed by `//DEPS`
* To ensure long-term stability of `kscript` we've added a suite of unit test. The repository tested continuously by Travis CI
* Cache directory is now `~/.kscript`
* More heuristics to guess `KOTLIN_HOME`
* Cache cleanup `--clear-cache` now applies to jars, scripts, urls, and cached dependency lookups
* Fixed compatibility with [Kotlin v1.1](https://kotlinlang.org/docs/reference/whatsnew11.html)


## v1.2 

* Fixed compatibility with kotlin v1.1 (fixes #15)
* Added `-i` to dump interactive console command incl deps (fixes #10)
* Compile jars should go to TEMP (fixes #13)
* started test-suite 

## v1.1

* Support for stdin and process substitution as script source. See [examples](examples/unit_tests.sh)
* versioning and auto-update
* basic command-line help
* Added support for `KOTLIN_OPTS` (see [#8](https://github.com/holgerbrandl/kscript/issues/8))
* Added CLI help to `expandcp.kts`
* Added option to clear dependency lookup cache: `expandcp.kts --clear-cache`

## v1.0

Initial Release