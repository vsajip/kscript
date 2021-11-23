* Improved test_suite.sh
  * Automatic setting up of test environment (assert.sh, test directories) 
  * Automatic compilation
  * idea - script to help to test idea use cases
  * Script setup_environment.sh can be used for local testing
* Improved Unit Tests
  * Several new Unit tests
  * Another Unit tests can be created much easier
* Improved Logging
  * 
* Modularisation of source code
  * Removed duplication
  * Code moved 
  * Script resolution creates immutable objects
  * 
* 

INCOMPATIBLE CHANGES:
* In annotations the only allowed delimiter is coma "," (to allow options with arguments)
* Resolution of env variables is more restrictive - only vars expected by kscript can be resolved (for security - it's not good to include arbitrary strings from user env into the script)  

SUGGESTIONS:
* I would drop old annotations based on comments or at least depreciate them
* 
