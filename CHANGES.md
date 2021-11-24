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
  * Silent mode / Development mode logging
* Modularisation of source code
  * Removed duplication
  * Code divided in logical pieces and moved to packages 
  * Script resolution creates immutable objects
* Updated Kotlin to 1.5.31 (Can not move to Kotlin 1.6, because of issues with resolution of artifacts in kotlin-scripting) and Gradle to 7.3
* Fixed many warnings in code
* Packaging - gradle file converted to Kotlin; still does not work, but it was like that before anyway

INCOMPATIBLE CHANGES:
* In annotations the only allowed delimiter is coma "," (to allow options with arguments)
* Resolution of env variables is more restrictive - only vars expected by kscript can be resolved (for security - it's not good to include arbitrary strings from user env into the script)
* Reworked caching mechanism
* Dropped cache for resolution of artifacts - I can not observe change in resolution time. In fact .m2 repository is already kind of cache, so it should not be needed to add another one.  

SUGGESTIONS:
* I would drop old annotations based on comments or at least depreciate them
* Caching should be further extended, so that there is a single cached project for different use-cases e.g. packaging, idea, jar creation...  
* It's worthy to consider dropping ability to reference script by $HOME and by '/'. It will break other scripts if used in web scripts (for security reasons).
* There might be duplicate names when loading even different files from URI. Then when the idea project creates source dir it will fail: e.g. idea project from include_variations.kts

SUGGESTIONS - MERGE:
* Please do "Squash Merge" - there is a lot of changes and experiments in code - no reason to keep them in log
* One test in ScriptResolverTest is @Disabled. It should be enabled and committed again after merge, so that references to URLs are correct
