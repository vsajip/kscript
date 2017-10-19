#!/usr/bin/env bash



# todo push image to docker registry
cd ${KSCRIPT_HOME}/misc
#docker build --no-cache -t kscript_tester .
docker build -t kscript_tester .
docker run -it kscript_tester

#curl -Lso /bin/kscript https://raw.githubusercontent.com/holgerbrandl/kscript/abb5f4c6ee72ec90d22c0fe913284e92363cad0e/kscript && chmod u+x /bin/kscript
#curl -Lso /bin/kscript https://www.dropbox.com/s/l5g8vr0wz78y3zy/kscript?dl=1 && chmod u+x /bin/kscript

#kscript --help
#kscript --self-update


## or using github repo
git clone https://github.com/holgerbrandl/kscript.git
cd kscript
export KSCRIPT_HOME=$(pwd)

gradle shadowJar && cp build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.jar

export PATH=$(pwd):${PATH}
kscript --clear-cache

wget https://raw.githubusercontent.com/lehmannro/assert.sh/master/assert.sh
chmod u+x assert.sh

${KSCRIPT_HOME}/test/test_suite.sh

## copy newer jar into container
#scp brandl@scicomp-mac-12-usb:/Users/brandl/projects/kotlin/kscript/kscript.jar .
