# Scripting Tutorial


# `sed`/`awk`-like streaming


Use `argMap` to abstract from file input argument
```bash

## extract column
kscript 'argMap(args[1]) { it.split("\t")[7] }' big-file.txt

## also extract column but from stdin
cat big-file.txt | kscript 'argMap(args[1]) { it.split("\t")[7] }' -
        
```

Use `argFilter` to perform grep-lik operations
```
kscript 'argFilter(args[1]) { it.split("\t")[7] == "UA" }' src/test/resources/flights_head.txt
```

## Table processing

For very general table processing, we recommend [csvkit](https://csvkit.readthedocs.io). For more specific table processing you should most likely push tables through R.

However, for more typed dat
We recommend [krangl] to crunch tables with `kscript`.




## Bioinformatics


1. Fasta

2. Fastq

3. Bed

3. Bam

4. HPC



