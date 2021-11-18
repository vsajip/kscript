import kscript.app.*
import org.junit.jupiter.api.Test

/**
 * @author Holger Brandl
 */

class Tests {

//    @Test
//    fun test_consolidate_imports() {
//        val file = File("test/resources/consolidate_includes/template.kts")
//        val expected = File("test/resources/consolidate_includes/expected.kts")
//
//        val result = resolveIncludes(file.toURI())
//
//        result.scriptFile.readText() shouldBe (expected.readText())
//    }
//
//
//    @Test
//    fun test_include_annotations() {
//        val file = File("test/resources/includes/include_variations.kts")
//        val expected = File("test/resources/includes/expected_variations.kts")
//
//        val result = resolveIncludes(file.toURI())
//
//        result.scriptFile.readText() shouldBe (expected.readText())
//    }
//
//    @Test
//    fun test_include_detection() {
//        val result = resolveIncludes(File("test/resources/includes/include_variations.kts").toURI())
//
//        result.includes.filter { it.protocol == "file" }.map { File(it.toURI()).name } shouldBe List(7) { "include_${it + 1}.kt" }
//        result.includes.filter { it.protocol != "file" }.size shouldBe 2
//    }
//
//    @Test
//    fun `test include detection - should not include dependency twice`() {
//      val result = resolveIncludes(File("test/resources/includes/dup_include/dup_include.kts").toURI())
//
//      result.includes.map { File(it.toURI()).name } shouldBe listOf(
//            "dup_include_1.kt",
//            "dup_include_2.kt"
//        )
//    }
}
