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
gradle build

#kotlinc -help

## iteration 1 simple script
misc/experimental/kscript2 "1+1"
gradle shadowJar && misc/experimental/kscript2 "1+1"

## iteration 2 url inputs etc.
## iteration 3 dependencies
## iteration 4 kotlin opts


########################################################################################################################

#kotlinc -d test.jar kkscript.kts
kscript tester.kts

#find jar
ls -tlh ~/.kscript/*tester*
#rm ~/.kscript/*kkscript*

