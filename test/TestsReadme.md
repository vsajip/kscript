## How to run the unit-tests?


We use https://github.com/lehmannro/assert.sh for the tests. To install it just do:


```bash
cd ~/bin
wget https://raw.githubusercontent.com/lehmannro/assert.sh/master/assert.sh
chmod u+x assert.sh
```




To run the tests, just run the [`test_suite.sh`](test_suite.sh)
```bash
export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"
## change into this/test directory

cd ${KSCRIPT_HOME}

## make sure to use devel-version
export PATH=${KSCRIPT_HOME}:${PATH}

## clean up the environment
sdk use kotlin 1.1-RC
kscript --clear-cache

./test_suite.sh

# run again with kotlin 1.0.X
sdk use kotlin 1.0.6
./test_suite.sh


```


For more examples see https://github.com/lehmannro/assert.sh/blob/master/tests.sh

