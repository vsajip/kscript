* Improved test_suite.sh
  * Automatic setting up of test environment (assert.sh, test directories) 
  * Automatic compilation
  * idea - script to help to test idea use cases
  * Script setup_environment.sh can be used for local testing
  * Some script tests moved to Unit Tests 

* Improved Unit Tests
  * Several new Unit tests
  * New Unit Tests can be created much easier (Major point why modularization makes sense)
 
* Improved Logging
  * Silent mode / Development mode logging

* Modularisation of source code
  * Removed duplication
  * Code divided in logical pieces and moved to packages 
  * Script resolution creates immutable objects

* Build script
  * Updated Gradle to version 7.3 and shadowJar to 6.1.0
  * Fixes in build file

* Performance
  * Much less IO operations - that should contribute to better performance

* Updated Kotlin to 1.5.31, but only for compiler, not kotlin-scripting. It's far from optimal, but it is not possible to move fully to Kotlin 1.5 or even 1.6, because of the issues with resolution of artifacts in latest kotlin-scripting. I have put report here: https://youtrack.jetbrains.com/issue/KT-49511

* Fixed a lot of IDE warnings in code

* Packaging - gradle file converted to Kotlin; still does not work, but it was like that before anyway

* Changes for kscript dir allow simple implementation of config file if needed. (.kscript/kscript.config); Not implemented by me, but might be useful e.g. for storing preambles

INCOMPATIBLE CHANGES:
* In annotations the only allowed delimiter is coma "," (to allow options with arguments, separated by space)
* Resolution of env variables is more restrictive - only vars expected by kscript can be resolved (for security - it's not good to include arbitrary strings from user env into the script)
* Reworked caching mechanism
* Dropped cache for resolution of artifacts - I can not observe change in resolution time. In fact .m2 repository is already kind of cache, so it should not be needed to add another one.  

SUGGESTIONS:
* I would drop old annotations based on comments or at least depreciate them 
* It's worthy to consider dropping ability to reference script by $HOME and by '/'. It will break other scripts if used in web scripts.
* There might be duplicate names when loading even different files from URI. Then when the idea project creates source dir it will fail: e.g. idea project from include_variations.kts. Requires some other way of setting up IDEA project.

SUGGESTIONS - MERGE:
* Please do "Squash Merge" - there is a lot of changes and experiments in code - no reason to keep them in log
* One test in ScriptResolverTest is @Disabled. It should be enabled and committed again after merge, so that references to URLs are correct
* I would suggest to do release of kscript 4.0-beta for better tests of new implementation.
* 
