#!/usr/bin/env bash

export DEBUG="--verbose"

. assert.sh



# `echo test` is expected to write "test" on stdout
#assert "echo test" "test2"

# expect `exit 127` to terminate with code 128
#assert_raises "exit 127" 128

## make sure that scripts can be piped into kscript
assert "echo 'println(1+1)' | kscript -" "2"

## make sure that heredoc is accepted as argument
assert "source ${KSCRIPT_HOME}/test/resources/here_doc_test.sh" "hello kotlin"

## make sure that it runs with local script files
assert "source ${KSCRIPT_HOME}/test/resources/local_script_file.sh" "kscript rocks!"

## make sure that it runs with local script files
assert "kscript ${KSCRIPT_HOME}/test/resources/multi_line_deps.kts" "kscript is  cool!"

## todo test what happens if kotlin is not in PATH

assert_end pipe_tests



## interactive mode without dependencies
assert "echo '' | kscript -i " "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


assert "kscript -i " "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


assert_end cli_helper_tests


## make sure that KOTLIN_HOME can be guessed from kotlinc correctly
assert "unset KOTLIN_HOME; echo 'println(99)' | kscript -" "99"


assert_end environment_tests

# export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"; export PATH=${KSCRIPT_HOME}:${PATH}

assert "expandcp.kts log4j:log4j:1.2.14" "${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar"

## impossible version
assert_raises "expandcp.kts log4j:log4j:9.8.76" 1

## wrong format should exit with 1
assert_raises "expandcp.kts log4j:1.0" 1

## wrong format should give meaningful error message
assert "expandcp.kts log4j:1.0 2>&1" "invalid dependency locator: log4j:1.0\nExpected format is groupId:artifactId:version[:classifier]"

## other version of wrong format should die with useful error.
assert "expandcp.kts log4j:::1.0 2>&1" "Failed to lookup dependencies. Check dependency locators or file a bug on https://github.com/holgerbrandl/kscript"

## one good dependency,  one wrong
assert_raises "expandcp.kts org.docopt:docopt:0.9.0-SNAPSHOT log4j:log4j:1.2.14" 1

assert_end dependency_lookup


