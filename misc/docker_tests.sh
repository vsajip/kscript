#!/usr/bin/env bash


docker run -it bioinfo_base

#curl -Lso /bin/kscript https://raw.githubusercontent.com/holgerbrandl/kscript/abb5f4c6ee72ec90d22c0fe913284e92363cad0e/kscript && chmod u+x /bin/kscript
curl -Lso /bin/kscript https://www.dropbox.com/s/l5g8vr0wz78y3zy/kscript?dl=1 && chmod u+x /bin/kscript

kscript --help
kscript --self-update

