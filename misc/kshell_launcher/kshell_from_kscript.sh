#!/usr/bin/env bash


if [ $# -ne 1 ]; then
    echo "Usage: kshell_from_kscript.sh <kscript.kts>"
    exit 0
fi

tmpfile=$(mktemp).kts

echo '@file:Include("https://git.io/fAJ5h")' >> $tmpfile
echo '' >> $tmpfile

argScript=$1
#argScript=krangl_example.kts

cat $argScript >> $tmpfile

#cat $tmpfile


echo "Preparing interactive session by resolving script dependencies..."

## resolve dependencies without running the kscript
KSCRIPT_DIR=$(dirname $(which kscript))
kscript_nocall() { kotlin -classpath ${KSCRIPT_DIR}/kscript.jar kscript.app.KscriptKt "$@";}

kshellCP=$(kscript_nocall $tmpfile | cut -d' ' -f4)

## create new
exec java -classpath "${kshellCP}" sparklin.kshell.KotlinShell $@
