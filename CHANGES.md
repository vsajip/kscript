* Improved test_suite.sh
  * Automatic setting up of test environment (assert.sh, test directories) 
  * Automatic compilation
  * idea - script to help to test idea use cases
  * Script setup_environment.sh can be used for local testing
  * Some script tests moved to Unit Tests 
* Improved Unit Tests
  * Several new Unit tests
  * Another Unit tests can be created much easier
* Improved Logging
  * Silent mode / Development mode
* Modularisation of source code
  * Removed duplication
  * Code divided in logical pieces and moved to packages 
  * Script resolution creates immutable objects
* Updated Kotlin to 1.5.31 (Can not move to Kotlin 1.6, because of issues with resolution of artifacts in kotlin-scripting) and Gradle to 7.3
* Fixed many warnings in code

INCOMPATIBLE CHANGES:
* In annotations the only allowed delimiter is coma "," (to allow options with arguments)
* Resolution of env variables is more restrictive - only vars expected by kscript can be resolved (for security - it's not good to include arbitrary strings from user env into the script)
* Reworked caching mechanism

SUGGESTIONS:
* I would drop old annotations based on comments or at least depreciate them
* Caching should be further extended, so that there is a single cached project for different use-cases e.g. packaging, idea, jar creation...  
