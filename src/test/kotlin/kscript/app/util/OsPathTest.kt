package kscript.app.util

import assertk.assertThat
import assertk.assertions.*
import kscript.app.model.OsType
import org.junit.jupiter.api.Test

internal class OsPathTest {
    // ************************************************** LINUX PATHS **************************************************
    @Test
    fun `Test Linux paths`() {
        assertThat(OsPath.create(OsType.LINUX, "/")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("/"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, "/home/admin/.kscript")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("/", "home", "admin", ".kscript"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, "./home/admin/.kscript")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf(".", "home", "admin", ".kscript"))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, "../home/admin/.kscript")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("..", "home", "admin", ".kscript"))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, "..")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf(".."))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, ".")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("."))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }
    }

    @Test
    fun `Normalization of Linux paths`() {
        assertThat(OsPath.create(OsType.LINUX, "/home/admin/.kscript/../../")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("/", "home"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, "./././../../script")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("..", "..", "script"))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat(OsPath.create(OsType.LINUX, "/a/b/c/../d/script")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("/", "a", "b", "d", "script"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.LINUX)
        }

        assertThat { OsPath.create(OsType.LINUX, "/.kscript/../../") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Path after normalization goes beyond root element: '/.kscript/../../'")
    }

    @Test
    fun `Test invalid Linux paths`() {
        assertThat { OsPath.create(OsType.LINUX, "//") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Duplicated path separators or empty path names in '//'")

        assertThat { OsPath.create(OsType.LINUX, "/ad\\asdf") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid characters in path: '\\'")
    }

    @Test
    fun `Test Linux stringPath conversion`() {
        assertThat(OsPath.create(OsType.LINUX, "/home/admin/.kscript").stringPath()).isEqualTo("/home/admin/.kscript")
        assertThat(OsPath.create(OsType.LINUX, "/a/b/c/../d/script").stringPath()).isEqualTo("/a/b/d/script")
        assertThat(OsPath.create(OsType.LINUX, "./././../../script").stringPath()).isEqualTo("../../script")
    }

    // ************************************************* WINDOWS PATHS *************************************************

    @Test
    fun `Test Windows paths`() {
        assertThat(OsPath.create(OsType.WINDOWS, "C:\\")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("C:\\"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, "C:\\home\\admin\\.kscript")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("C:\\", "home", "admin", ".kscript"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, ".\\home\\admin\\.kscript")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf(".", "home", "admin", ".kscript"))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, "..\\home\\admin\\.kscript")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("..", "home", "admin", ".kscript"))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, "..")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf(".."))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, ".")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("."))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }
    }

    @Test
    fun `Normalization of Windows paths`() {
        assertThat(OsPath.create(OsType.WINDOWS, "C:\\home\\admin\\.kscript\\..\\..\\")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("C:\\", "home"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, ".\\.\\.\\..\\..\\script")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("..", "..", "script"))
            it.prop(OsPath::pathType).isEqualTo(PathType.RELATIVE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat(OsPath.create(OsType.WINDOWS, "C:\\a\\b\\c\\..\\d\\script")).let {
            it.prop(OsPath::pathParts).isEqualTo(listOf("C:\\", "a", "b", "d", "script"))
            it.prop(OsPath::pathType).isEqualTo(PathType.ABSOLUTE)
            it.prop(OsPath::osType).isEqualTo(OsType.WINDOWS)
        }

        assertThat { OsPath.create(OsType.WINDOWS, "C:\\.kscript\\..\\..\\") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Path after normalization goes beyond root element: 'C:\\.kscript\\..\\..\\'")
    }

    @Test
    fun `Test invalid Windows paths`() {
        assertThat { OsPath.create(OsType.WINDOWS, "C:\\\\") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Duplicated path separators or empty path names in 'C:\\\\'")

        assertThat { OsPath.create(OsType.WINDOWS, "C") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid root element of path: 'C'")

        assertThat { OsPath.create(OsType.WINDOWS, "C:\\adas?df") }.isFailure()
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid characters in path: '?'")
    }
}
