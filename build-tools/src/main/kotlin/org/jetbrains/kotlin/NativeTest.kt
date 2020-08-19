/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import java.io.File
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.*

open class CompileNativeTest @Inject constructor(
        @InputFile val inputFile: File,
        @Input val target: String
) : DefaultTask () {
    @OutputFile
    var outputFile = File(File(project.buildDir, target), "${inputFile.nameWithoutExtension}.o")

    @Input
    val clangArgs = mutableListOf<String>()

    @TaskAction
    fun compile() {
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        plugin.execBareClang(Action {
            it.executable = "clang++"
            it.args = clangArgs + listOf(inputFile.absolutePath, "-o", outputFile.absolutePath)
        })
    }
}

open class LinkNativeTest @Inject constructor(
        @InputFiles val inputFiles: List<File>,
        @OutputFile val outputFile: File,
        @Input private val commands: List<List<String>>
) : DefaultTask () {
    companion object {
        fun create(
                project: Project,
                platformManager: PlatformManager,
                taskName: String,
                inputFiles: List<File>,
                target: String,
                outputFile: File,
                linkerArgs: List<String>
        ): LinkNativeTest {
            val linker = platformManager.platform(platformManager.targetByName(target)).linker
            val commands = linker.linkCommands(
                    inputFiles.map { it.absolutePath },
                    outputFile.absolutePath,
                    listOf(),
                    linkerArgs,
                    false,
                    false,
                    LinkerOutputKind.EXECUTABLE,
                    "",
                    false
            ).map { it.argsWithExecutable }
            return project.tasks.create(
                    taskName,
                    LinkNativeTest::class.java,
                    inputFiles,
                    outputFile,
                    commands)
        }

        fun create(
                project: Project,
                platformManager: PlatformManager,
                taskName: String,
                inputFiles: List<File>,
                target: String,
                executableName: String,
                linkerArgs: List<String> = listOf()
        ): LinkNativeTest = create(
                project,
                platformManager,
                taskName,
                inputFiles,
                target,
                File(File(project.buildDir, target), executableName),
                linkerArgs)
    }

    @TaskAction
    fun link() {
        for (command in commands) {
            project.exec {
                it.commandLine(command)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T: Task> T.configure(f: T.() -> Unit): T =
    this.configure(object: Closure<Unit>(this) {
        // Dynamically invoked by Groovy
        fun doCall() {
            f()
        }
    }) as T

fun createTestTask(
        project: Project,
        testTaskName: String,
        testedTaskNames: List<String>
): Task {
    val platformManager = project.rootProject.findProperty("platformManager") as PlatformManager
    val testedTasks = testedTaskNames.map {
        project.tasks.getByName(it) as CompileToBitcode
    }
    val compileToBitcodeTasks = testedTasks.mapNotNull {
        val name = "${it.name}TestBitcode"
        val task = project.tasks.findByName(name) as? CompileToBitcode ?:
            project.tasks.create(name,
                    CompileToBitcode::class.java,
                    it.srcRoot,
                    "${it.folderName}Tests",
                    it.target
                    ).configure {
                includeFiles.clear()
                excludeFiles.clear()
                includeFiles.addAll(listOf("**/*Test.cpp", "**/*Test.mm"))
                dependsOn(it)
                compilerArgs.addAll(it.compilerArgs)
            }
        if (task.inputFiles.count() == 0)
            null
        else
            task
    }
    val compileToObjectFileTasks = (compileToBitcodeTasks + testedTasks).map {
        val name = "${it.name}Object"
        val target = platformManager.targetByName(it.target)
        val clangFlags = platformManager.platform(target).configurables as ClangFlags
        project.tasks.findByName(name) as? CompileNativeTest ?:
                project.tasks.create(name,
                        CompileNativeTest::class.java,
                        it.outFile,
                        it.target
                ).configure {
                    dependsOn(it)
                    clangArgs.addAll(clangFlags.clangFlags)
                    clangArgs.addAll(clangFlags.clangNooptFlags)
                }
    }
    val target = compileToObjectFileTasks.map {
        it.target
    }.distinct().single()
    val linkTask = LinkNativeTest.create(
            project,
            platformManager,
            "${testTaskName}Link",
            compileToObjectFileTasks.map { it.outputFile },
            target,
            testTaskName
    ).configure {
        dependsOn(compileToObjectFileTasks)
    }
    return project.tasks.create(testTaskName, Exec::class.java).configure {
        dependsOn(linkTask)
        commandLine(linkTask.outputFile)
    }
}
