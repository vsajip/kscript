#!/usr/bin/env bash


cd /Users/brandl/projects/kotlin/kscript/misc/experimental


########################################################################################################################


## build the kscript application jar
rm ~/.kscript/*kkscript*
kscript kkscript.kts

jarFile=/Users/brandl/.kscript/$(ls -t ~/.kscript | head -n1)
ls $jarFile


kotlin -classpath $jarFile Main_Kkscript


########################################################################################################################
## using regular app


cd /Users/brandl/projects/kotlin/kscript

## directly compile it
#kotlinc -d test.jar kscript.kt

## rather use gradle to package
gradle shadowJar
#gradle build

#kotlinc -help

## iteration 1 simple script
misc/experimental/kscript2 "1+1"

./kscript --clear-cache; (cd ../../ && gradle shadowJar); ./kscript2 "1+1"
./kscript --clear-cache; gradle shadowJar; misc/experimental/kscript2 "1+1"
kscript --clear-cache; gradle shadowJar; misc/experimental/kscript2 "lines.print()"

kscript --clear-cache; gradle shadowJar; kotlin -classpath /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.KscriptKt /Users/brandl/test.kts
cat /Users/brandl/test.kts

kscript --clear-cache; gradle shadowJar; kotlin -classpath /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.KscriptKt "1+1"

kotlin --clear-cache; gradle shadowJar; java -classpath /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.KscriptKt "1+1"
kotlin --clear-cache; gradle shadowJar; java -classpath /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.KscriptKt "1+1"


## iteration 2 url inputs etc.
## iteration 3 dependencies
## iteration 4 kotlin opts


########################################################################################################################

#kotlinc -d test.jar kkscript.kts
kscript tester.kts

#find jar
ls -tlh ~/.kscript/*tester*
#rm ~/.kscript/*kkscript*

#######################################################################



kotlin -version
kotlinc -version

## create test script
echo "println(42)" > myscript.kts

## create little apps that simply compiles
cat <<EOF >  Foo.kt
class Foo(){
    companion object {
        @JvmStatic fun main(args:Array<String>){
            val scriptFile = java.io.File("myscript.kts")
            val jarFile = java.io.File("test.jar")

            val pb = ProcessBuilder("kotlinc", "-d", jarFile.absolutePath, scriptFile.absolutePath).
                redirectOutput(ProcessBuilder.Redirect.INHERIT).
                redirectError(ProcessBuilder.Redirect.INHERIT)

            pb.environment()["KOTLIN_RUNNER"] = ""

            val exitVal = pb.start().waitFor()
            println("exit code was "+exitVal)
        }
    }
}

EOF

# compile works
kotlinc  Foo.kt

# fails with kotlin
kotlin  Foo
>> error: unsupported argument: -d
>> exit code was 1

## but it works with java!
java -classpath .:/Users/brandl/.sdkman/candidates/kotlin/current/lib/kotlin-stdlib.jar Foo
>> exit code was 0

# try to compile without the app --> works
kotlinc -d other.jar myscript.kts


########################################################################################################################
### comparison with kscript v1.x

kscript - ~/test.kts <<"EOF"
//DEPS com.github.holgerbrandl:kscript:1.2.2

import kscript.text.*
val lines = resolveArgFile(args)
lines.print()
EOF


export PATH=$KSCRIPT_HOME/misc/experimental:$PATH

./kscript - ~/test.kts <<"EOF"
//DEPS com.github.holgerbrandl:kscript:1.2.2

import kscript.text.*
val lines = resolveArgFile(args)
lines.print()
EOF


########################################################################################################################
### test suite coverage

export PATH=${KSCRIPT_HOME}/misc/experimental:${PATH}
which kscript
which assert.sh


cd ${KSCRIPT_HOME}
gradle shadowJar

## clean up the environment
#sdk use kotlin 1.1-RC
kscript --clear-cache

${KSCRIPT_HOME}/test/test_suite.sh




########################################################################################################################
### jar script embedding https://stackoverflow.com/questions/17339631/creating-shell-script-from-executable-java-jar-file

cd /Users/brandl/projects/kotlin/kscript/misc/experimental

#cp kscript2 kscriptJarStub
(cd ../../ && gradle shadowJar)

cat kscriptJarStub /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar > kscriptJar && chmod +x kscriptJar

./kscriptJar --help
./kscriptJar "println(1+1)"

ln -s /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.jar

#https://stackoverflow.com/questions/17583578/what-command-means-do-nothing-in-a-conditional-in-bash
#https://github.com/megastep/makeself
#https://stackoverflow.com/questions/10491704/embed-a-executable-binary-in-a-shell-script

########################################################################################################################
## windows testing

bash -c "kotlinc -classpath '' -d 'C:\cygwin\home\holger\.kscript\scriptlet.d96e018f51ea61e5.jar' 'C:\cygwin\tmp\tmp5305891320090504256.tmp\scriptlet.d96e018f51ea61e5.kts'"


bash -c kotlinc -classpath '' -d 'C:\cygwin\home\holger\.kscript\scriptlet.d96e018f51ea61e5.jar' 'C:\cygwin\tmp\tmp5305891320090504256.tmp\scriptlet.d96e018f51ea61e5.kts'


## todo for windows we need ; as classpath separator
kotlin  -classpath 'C:\cygwin\home\holger\.kscript\scriptlet.d96e018f51ea61e5.jar;C:\cygwin\home\holger\SDKMAN~1\CANDID~1\kotlin\111112~1.51/lib/kotlin-script-runtime.jar' Main_Scriptlet_d96e018f51ea61e5

kotlin  -classpath 'C:\cygwin\home\holger\.kscript\scriptlet.d96e018f51ea61e5.jar' Main_Scriptlet_d96e018f51ea61e5


ls -la 'C:\cygwin\home\holger\SDKMAN~1\CANDID~1\kotlin\111112~1.51/lib/kotlin-script-runtime.jar'
ls -la 'C:\cygwin\home\holger\.kscript\scriptlet.d96e018f51ea61e5.jar'

echo <<EOF

val file = java.io.File("C:\cygwin\home\holger\SDKMAN~1\CANDID~1\kotlin\111112~1.51/lib/kotlin-script-runtime.jar")

EOF


kotlin  -classpath 'C:\cygwin\home\holger\.kscript\scriptlet.d96e018f51ea61e5.jar;C:\cygwin\home\holger\SDKMAN~1\CANDID~1\kotlin\111112~1.51\lib\kotlin-script-runtime.jar;' Main_Scriptlet_d96e018f51ea61e5