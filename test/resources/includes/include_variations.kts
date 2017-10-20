println("Let's resolve includes!")

//INCLUDE rel_includes/include_1.kt
//INCLUDE ./rel_includes//include_2.kt

//INCLUDE ../includes/include_3.kt
//INCLUDE include_4.kt

include_1()
include_2()
include_3()
include_4()



println("wow, so many includes")
