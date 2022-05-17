## temp projects with include symlinks
assert_raises "TMP_DIR=$(kscript --idea ${PROJECT_DIR}/test/resources/includes/include_variations.kts); cd $TMP_DIR && gradle build" 0

## support diamond-shaped include schemes (see #133)
assert_raises "TMP_DIR=$(kscript --idea ${PROJECT_DIR}/test/resources/includes/diamond.kts); cd $TMP_DIR && gradle build" 0


## todo re-enable interactive mode tests using kscript_nocall
#kscript_nocall() { kotlin -classpath ${PROJECT_DIR}/build/libs/kscript.jar kscript.app.KscriptKt "$@";}
#export -f kscript_nocall
