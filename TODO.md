# kscript 4.1 features:

* Multiplatform tests for different OS-es
* Windows console support requires @argfiles as kotlin/kotlinc command line is too long to execute it from console.

* Fix for IntelliJ projects consisting of files with the same names + re-enable tests
* Fix for packaging + re-enable tests
* Cleanup of ENV variables (CUSTOM_KSCRIPT_PREAMBLE -> KSCRIPT_PREAMBLE, KSCRIPT_IDEA_COMMAND -> KSCRIPT_COMMAND_IDEA, KSCRIPT_GRADLE_COMMAND -> KSCRIPT_COMMAND_GRADLE)
* Depreciation of @MavenRepository -> @Repository is Kotlin standard
* Depreciation of some old features with WARN (comment based annotations, referencing script by $HOME and by '/' - those references won't work for web scripts)
* Improve Unit tests
