#!/usr/bin/env bash

export DEBUG="--verbose"

. ~/bin/assert.sh



# `echo test` is expected to write "test" on stdout
#assert "echo test" "test2"

# expect `exit 127` to terminate with code 128
#assert_raises "exit 127" 128

## make sure that scripts can be piped into kscript
assert "echo 'println(1+1)' | kscript -" "2"

## make sure that heredoc is accepted as argument
assert "source resources/here_doc_test.sh" "hello kotlin"

## make sure that it runs with local script files
assert "source resources/local_script_file.sh" "kscript rocks!"

## todo test what happens if kotlin is not in PATH

assert_end pipe_tests



## interactive mode without dependencies
assert "echo '' | kscript -i " "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


assert "kscript -i " "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


assert_end cli_helper_tests
