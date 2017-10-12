#!/usr/bin/env bash


#kotlin -classpath test.jar kscript.app.KscriptKt "$@"
exec $(kotlin -classpath test.jar kscript.app.KscriptKt "$@")