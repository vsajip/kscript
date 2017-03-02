

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