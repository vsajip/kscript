#!/usr/bin/env bash


##
## Unit tests for kscript
##


## todo make this more streamlined. Learn from http://stackoverflow.com/questions/1339416/unit-testing-bash-scripts

## make sure that expandcp.kts gives an exitcode 1 if some dependencies are not found
expandcp.kts org.docopt:docopt:0.6.0-FOOBAR

expandcp.kts org.docopt:docopt:0.6.0-SNAPSHOT



## make sure that scripts can be provided on the fly ...

## (1) via pipe
echo '
println(1+3)
' |  kscript -


## (2) heredoc
kscript - <<"EOF"
println(1+1)
EOF

## (2b) heredoc with dependencies
kscript - <<"EOF"
//DEPS org.docopt:docopt:0.6.0-SNAPSHOT log4j:log4j:1.2.14

import org.docopt.Docopt
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

println(1+1)
EOF



## (3) process substitution
echo '
println((1+3).toString() + " test")
' > .test.kts

kscript <(cat .test.kts)



## (4) KOTLIN_OPTS (

kscript - <<"EOF" | grep "^-ea"
//KOTLIN_OPTS -J-Xmx5g  -J-server -J-ea

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean

println("Hello from Kotlin with 5g of heap memory in server mode!")

val bean = ManagementFactory.getRuntimeMXBean()
val aList = bean.inputArguments

for (i in aList.indices) {
    println(aList[i])
}

EOF





public void runtimeParameters() {

}