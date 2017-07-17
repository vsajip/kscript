#!/usr/bin/env bash

export DEBUG="--verbose"

. assert.sh


## define test helper, see https://github.com/lehmannro/assert.sh/issues/24
assert_statement(){
    # usage cmd exp_stout exp_stder exp_exit_code
    assert "$1" "$2"
    assert "( $1 ) 2>&1 >/dev/null" "$3"
    assert_raises "$1" "$4"
}
#assert_statment "echo foo; echo bar  >&2; exit 1" "foo" "bar" 1


#http://stackoverflow.com/questions/3005963/how-can-i-have-a-newline-in-a-string-in-sh
#http://stackoverflow.com/questions/3005963/how-can-i-have-a-newline-in-a-string-in-sh
export NL=$'\n'



## make sure that scripts can be piped into kscript
assert "source ${KSCRIPT_HOME}/test/resources/direct_script_arg.sh" "kotlin rocks"

## also allow for empty programs
assert "kscript ''" ""

## provide script via stidin
assert "echo 'println(1+1)' | kscript -" "2"

## make sure that heredoc is accepted as argument
assert "source ${KSCRIPT_HOME}/test/resources/here_doc_test.sh" "hello kotlin"

## make sure that command substitution works as expected
assert "source ${KSCRIPT_HOME}/test/resources/cmd_subst_test.sh" "command substitution works as well"

## make sure that it runs with local script files
assert "source ${KSCRIPT_HOME}/test/resources/local_script_file.sh" "kscript rocks!"

## make sure that it runs with local script files
assert "kscript ${KSCRIPT_HOME}/test/resources/multi_line_deps.kts" "kscript is  cool!"

## scripts with dashes in the file name should work as well
assert "kscript ${KSCRIPT_HOME}/test/resources/dash-test.kts" "dash alarm!"


## missing script
assert_raises "kscript i_do_not_exist.kts" 1
assert "kscript i_do_not_exist.kts 2>&1" "[ERROR] Could not open script file 'i_do_not_exist.kts'"

## make sure that it runs with remote URLs
assert "kscript https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts" "I came from the internet"


assert_end script_input_modes


## interactive mode without dependencies
#assert "kscript -i 'exitProcess(0)'" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"
#assert "echo '' | kscript -i -" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


## first version is disabled because support-auto-prefixing kicks in
#assert "kscript -i '//DEPS log4j:log4j:1.2.14'" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"
assert "kscript -i <(echo '//DEPS log4j:log4j:1.2.14')" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"

assert_end cli_helper_tests


## do not run interactive mode prep without script argument
assert_statement "kscript -i" "" "[ERROR] Script argument for interactive mode preparation is missing" 1

## make sure that KOTLIN_HOME can be guessed from kotlinc correctly
assert "unset KOTLIN_HOME; echo 'println(99)' | kscript -" "99"

## todo test what happens if kotlin/kotlinc/java/maven is not in PATH


assert_end environment_tests

# export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"; export PATH=${KSCRIPT_HOME}:${PATH}

assert "resdeps.kts log4j:log4j:1.2.14" "${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar"

## impossible version
assert_raises "resdeps.kts log4j:log4j:9.8.76" 1

## wrong format should exit with 1
assert_raises "resdeps.kts log4j:1.0" 1

## wrong format should give meaningful error message
assert "resdeps.kts log4j:1.0 2>&1" "invalid dependency locator: log4j:1.0\nExpected format is groupId:artifactId:version[:classifier]"

## other version of wrong format should die with useful error.
assert "resdeps.kts log4j:::1.0 2>&1" "Failed to lookup dependencies. Check dependency locators or file a bug on https://github.com/holgerbrandl/kscript"

## one good dependency,  one wrong
assert_raises "resdeps.kts org.docopt:docopt:0.9.0-SNAPSHOT log4j:log4j:1.2.14" 1

assert_end dependency_lookup


## make sure that one-liners include support-api
assert 'echo "foo${NL}bar" | kscript "stdin.print()"' $'foo\nbar'
#echo "$'foo\nbar' | kscript 'stdin.print()'

assert 'echo "foo${NL}bar" | kscript "stdin.print()"' $'foo\nbar'
assert 'kscript "println(1+1)"' '2'


assert_statement 'echo "foo${NL}bar" | kscript "stdin.split().select(1, 2, -3)"' "" "[ERROR] Can not mix positive and negative selections" 1

assert_end support_api

