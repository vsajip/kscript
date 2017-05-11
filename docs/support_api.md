# Scripting Tutorial


__{work in progress}__

# `sed`/`awk`-like table processing

See see this [blog post](http://holgerbrandl.github.io/kotlin/2017/05/08/kscript_as_awk_substitute.html).

Perform grep-lik operations
```kscript
kscript 'lines.split().filter{ it[7] == "UA" }' src/test/resources/some_flights.txt
```

For very general table processing, we recommend [csvkit](https://csvkit.readthedocs.io). For more specific table processing you should most likely push tables through R.


## Bioinformatics


1. Fasta

2. Fastq

3. Bed

3. Bam

4. HPC



