#!/usr/bin/env bash

# see https://github.com/holgerbrandl/kscript/issues/67

#note: make sure to add mydsl to PATH
# export PATH=${PATH}:${KSCRIPT_HOME}/test/resources/custom_dsl

kscript - <<"EOF"
#!/usr/bin/env mydsl

println(foo)
included()
EOF