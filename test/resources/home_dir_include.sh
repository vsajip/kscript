echo "fun foo() = 42" > ~/.kscript_home_include.kts

echo '
//INCLUDE ~/.kscript_home_include.kts
println(foo())
' > home_dir_master.kts

kscript home_dir_master.kts