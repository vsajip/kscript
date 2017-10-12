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

kotlinc -help
kotlinc -d test.jar kscript.kt

./kkscript.sh "1+1"

########################################################################################################################


#kotlinc -d test.jar kkscript.kts
kscript tester.kts

#find jar
ls -tlh ~/.kscript/*tester*
#rm ~/.kscript/*kkscript*

