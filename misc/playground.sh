export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"; export PATH=${KSCRIPT_HOME}:${PATH}

## switch back to the production version
cd ~/Desktop
echo 'println("hello du")' > test.kts

kscript --clear-cache
kscript test.kts

kscript 'println("hello du")'
kscript https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts


sdk list kotlin
sdk use kotlin 1.1.1
sdk use kotlin 1.0.6

kotlin -version


kscript <(echo 'println("command substitution works as well")')

scriptFile="/dev/stdin"
scriptFile=$(<(echo foo))
if [ ! -f "$scriptFile" ]; then
    echo "processing direct script argument"
fi


if [[ ! -e "$scriptFile" ]]; then
    echo "processing direct script argument"
fi


##################################################################################################
####### entry directive
##################################################################################################


kscript --clear-cache

kscript /Users/brandl/projects/kotlin/kotlin_playground/src/test.kt

## no package default //ENTRY
## no package custom //ENTRY
## with package default //ENTRY
## with package custom //ENTRY
kscript --clear-cache; kscript /Users/brandl/projects/kotlin/kotlin_playground/src/test.kt
kscript --clear-cache; kscript /Users/brandl/projects/kotlin/kotlin_playground/src/test/test_1.kt

scriptFile=/Users/brandl/projects/kotlin/kotlin_playground/src/test/test_1.kt
scriptFile=/Users/brandl/projects/kotlin/kotlin_playground/src/test.kt


kotlinc -d foo.jar /Users/brandl/projects/kotlin/kotlin_playground/src/test/test_1.kt
unzip -l foo.jar
java -classpath foo.jar 'test.Foo$Companion'

## does not owrk
javac  /Users/brandl/projects/kotlin/kotlin_playground/src/burrows/Huff.java


### java support for kscript?
#https://stackoverflow.com/questions/9941296/how-do-i-make-a-jar-from-a-java

mkdir ${TMPDIR}/tt

javac  -d ${TMPDIR}/tt /Users/brandl/projects/kotlin/kotlin_playground/src/test/SimpleJava.java


javac  -d ${TMPDIR}/tt /Users/brandl/projects/kotlin/kotlin_playground/src/test/SimpleJava.java
#(cd $(dirname ${mainJava}) && ${JAR_CMD:=jar} uf ${jarFile} $(basename ${mainJava%%.java}.class))
JAR_CMD=jar
(cd ${TMPDIR}/tt/ && ${JAR_CMD:=jar} cvf foojar.jar *)
unzip -vl ${TMPDIR}/tt/foojar.jar


#### dependency lookp

if [ ! -z $(grep "$STRING" "$FILE") ]; then echo "FOUND"; fi
grep "^"$(echo ${dependencies} | tr ' ' ';')" " "${dependency_cache}"

grep -F "^"$(echo ${dependencies} | tr ' ' ';')" " ${dependency_cache} | cut -d' ' -f2

if [ ! -z $(grep "^"$(echo ${dependencies} | tr ' ' ';')" " "${dependency_cache}") ]; then echo "FOUND"; fi
classpath=$(grep -F $(echo ${dependencies} | tr ' ' ';')" " ${dependency_cache} | cut -d' ' -f2)

awk '$1 == "'"${dependencies}"'"' ${dependency_cache}