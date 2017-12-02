#!/usr/bin/env bash

# see https://github.com/holgerbrandl/kscript/issues/67

#note: make sure to add $KSCRIPT_HOME/test/resources/custom_dsl to PATH

kscript - <<"EOF"
#!/usr/bin/env mydsl

println(foo)
included()
EOF