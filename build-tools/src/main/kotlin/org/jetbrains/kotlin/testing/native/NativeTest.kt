/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.testing.native

import java.io.File
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.bitcode.CompileToBitcode
import org.jetbrains.kotlin.konan.target.*

open class CompileNativeTest @Inject constructor(
        @InputFile val inputFile: File,
        @Input val target: String
) : DefaultTask() {
    @OutputFile
    var outputFile = File(File(project.buildDir, target), "${inputFile.nameWithoutExtension}.o")

    @Input
    val clangArgs = mutableListOf<String>()

    @TaskAction
    fun compile() {
        val plugin = project.convention.getPlugin(ExecClang::class.java)
        plugin.execBareClang {
            it.executable = "clang++"
            it.args = clangArgs + listOf(inputFile.absolutePath, "-o", outputFile.absolutePath)
        }
    }
}

open class LlvmLinkNativeTest @Inject constructor(
        val baseName: String,
        @Input val target: String,
        @InputFile val mainFile: File
) : DefaultTask() {

    @SkipWhenEmpty
    @InputFiles
    var inputFiles: ConfigurableFileCollection = project.files()

    @OutputFile
    var outputFile: File = project.buildDir.resolve("$target/$baseName.bc")

    @TaskAction
    fun llvmLink() {
        val llvmDir = project.property("llvmDir")
        val tmpOutput = File.createTempFile("runtimeTests", ".bc").apply {
            deleteOnExit()
        }

        // The runtime provides our implementations for some standard functions (see StdCppStubs.cpp).
        // We need to internalize these symbols to avoid clashes with symbols provided by the C++ stdlib.
        // But llvm-link -internalize is kinda broken: it links modules one by one and can't see usages
        // of a symbol in subsequent modules. So it will mangle such symbols causing "unresolved symbol"
        // errors at the link stage. So we have to run llvm-link twice: the first one links all modules
        // except the one containing the entry point to a single *.bc without internalization. The second
        // run internalizes this big module and links it with a module containing the entry point.
        project.exec {
            it.executable = "$llvmDir/bin/llvm-link"
            it.args = listOf("-o", tmpOutput.absolutePath) + inputFiles.map { it.absolutePath }
        }

        project.exec {
            it.executable = "$llvmDir/bin/llvm-link"
            it.args = listOf(
                    "-o", outputFile.absolutePath,
                    mainFile.absolutePath,
                    tmpOutput.absolutePath,
                    "-internalize"
            )
        }
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
                    optimize = false,
                    debug = false,
                    kind = LinkerOutputKind.EXECUTABLE,
                    outputDsymBundle = "",
                    needsProfileLibrary = false
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

fun createTestTask(
        project: Project,
        testTaskName: String,
        testedTaskNames: List<String>
): Task {
    val platformManager = project.rootProject.findProperty("platformManager") as PlatformManager
    val googleTestExtension = project.extensions.getByName(RuntimeTestingPlugin.GOOGLE_TEST_EXTENSION_NAME) as GoogleTestExtension
    val testedTasks = testedTaskNames.map {
        project.tasks.getByName(it) as CompileToBitcode
    }
    val target = testedTasks.map {
        it.target
    }.distinct().single()
    val konanTarget = platformManager.targetByName(target)
    val compileToBitcodeTasks = testedTasks.mapNotNull {
        val name = "${it.name}TestBitcode"
        val task = project.tasks.findByName(name) as? CompileToBitcode ?:
            project.tasks.create(name,
                    CompileToBitcode::class.java,
                    it.srcRoot,
                    "${it.folderName}Tests",
                    target
                    ).apply {
                excludeFiles = emptyList()
                includeFiles = listOf("**/*Test.cpp", "**/*Test.mm")
                dependsOn(it)
                compilerArgs.addAll(it.compilerArgs)
                headersDirs += googleTestExtension.headersDirs
            }
        if (task.inputFiles.count() == 0)
            null
        else
            task
    }
    val testFrameworkTasks = listOf(
        project.tasks.getByName("${target}Googletest") as CompileToBitcode,
        project.tasks.getByName("${target}Googlemock") as CompileToBitcode
    )

    val testSupportTask = project.tasks.getByName("${target}TestSupport") as CompileToBitcode

    // TODO: It may make sense to merge llvm-link, compile and link to a single task.
    val llvmLinkTask = project.tasks.create(
            "${testTaskName}LlvmLink",
            LlvmLinkNativeTest::class.java,
            testTaskName, target, testSupportTask.outFile
    ).apply {
        val tasksToLink = (compileToBitcodeTasks + testedTasks + testFrameworkTasks)
        inputFiles = project.files(tasksToLink.map { it.outFile })
        dependsOn(testSupportTask)
        dependsOn(tasksToLink)
    }

    val clangFlags = platformManager.platform(konanTarget).configurables as ClangFlags
    val compileTask = project.tasks.create(
            "${testTaskName}Compile",
            CompileNativeTest::class.java,
            llvmLinkTask.outputFile,
            target
    ).apply {
        dependsOn(llvmLinkTask)
        clangArgs.addAll(clangFlags.clangFlags)
        clangArgs.addAll(clangFlags.clangNooptFlags)
    }

    val linkTask = LinkNativeTest.create(
            project,
            platformManager,
            "${testTaskName}Link",
            listOf(compileTask.outputFile),
            target,
            testTaskName
    ).apply {
        dependsOn(compileTask)
    }

    return project.tasks.create(testTaskName, Exec::class.java).apply {
        dependsOn(linkTask)

        workingDir = project.buildDir.resolve("testReports/$testTaskName")
        val xmlReport = workingDir.resolve("report.xml")
        executable(linkTask.outputFile)
        args("--gtest_output=xml:${xmlReport.absoluteFile}")

        doFirst {
            workingDir.mkdirs()
        }
    }
}
