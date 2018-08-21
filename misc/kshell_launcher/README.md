# Create kshell terminals from a kscript


Based on https://github.com/khud/sparklin which is a proof-of-concept software that includes experimental new Kotlin REPL.

## Installation

```bash
wget https://raw.githubusercontent.com/holgerbrandl/kscript/master/misc/kshell_launcher/kshell_from_kscript.sh
chmod +x kshell_from_kscript.sh
```

You may want to add it to your `PATH` as well.

## Usage

Simply provide any kscript as argument. E.g [`krangl_example.kts`]()
```
kshell_from_kscript.sh krangl_example.kts
```

## Todo

* bundle this with kscript itself