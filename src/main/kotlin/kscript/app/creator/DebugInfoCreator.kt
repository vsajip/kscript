package kscript.app.creator

import kscript.app.model.Config
import net.igsoft.tablevis.TableBuilder
import net.igsoft.tablevis.printer.text.TextTablePrinter
import net.igsoft.tablevis.style.text.BoxTextTableStyleSet

class DebugInfoCreator {
    fun create(config: Config, kscriptArgs: List<String>, userArgs: List<String>): String {
        val printer = TextTablePrinter()

        val classpathSeparator =
            if (config.osConfig.osType.isWindowsLike() || config.osConfig.osType.isPosixHostedOnWindows()) ';' else ':'

        val table = TableBuilder(BoxTextTableStyleSet()) {
            row {
                cell { value = "Debugging information for KScript (using tablevis by aartiPl)" }
            }

            row {
                cell { id("header"); value = "Configuration" }
                cell { value = config }
            }

            row {
                cell { id("header"); value = "KScript arguments" }
                cell { value = kscriptArgs.joinToString("\n") }
            }

            row {
                cell { id("header"); value = "User arguments" }
                cell { value = userArgs.joinToString("\n") }
            }

            row {
                cell { id("header"); value = "Classpath" }
                cell {
                    value =
                        System.getProperty("java.class.path")
                            .replace("file:", "")
                            .split(classpathSeparator)
                            .joinToString("\n")
                }
            }

            forId("header").setMinimalWidth()
        }.build()

        return printer.print(table)
    }
}
