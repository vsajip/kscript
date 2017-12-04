
see https://github.com/holgerbrandl/kscript/issues/67

make sure to add mydsl to PATH
```
export PATH=${PATH}:${KSCRIPT_HOME}/test/resources/custom_dsl
```


```bash

kscript - <<"EOF"
#!/usr/bin/env mydsl

println(foo)
included()
EOF

mydsl --idea - <<"EOF"
@file:DependsOn("com.beust:klaxon:0.24")

import com.beust.klaxon.Parser

val p = Parser()

println(foo)
included()
EOF

```


For remote debugging export preamble and run
```
$KSCRIPT_HOME/misc/experimental/kscriptD test/resources/custom_dsl/mydsl_test_with_deps.kts
```