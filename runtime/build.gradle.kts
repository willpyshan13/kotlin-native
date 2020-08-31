/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*

plugins {
    id("compile-to-bitcode")
}

fun CompileToBitcode.includeRuntime() {
    compilerArgs.add("-I" + project.file("../common/src/hash/headers"))
    compilerArgs.add("-I" + project.file("src/main/cpp"))
}

val hostName: String by project
val targetList: List<String> by project

bitcode {
    create("runtime", file("src/main")) {
        dependsOn(
            ":common:${target}Hash",
            "${target}StdAlloc",
            "${target}OptAlloc",
            "${target}Mimalloc",
            "${target}Launcher",
            "${target}Debug",
            "${target}Release",
            "${target}Strict",
            "${target}Relaxed",
            "${target}ProfileRuntime",
            "${target}Objc",
            "${target}ExceptionsSupport"
        )
        includeRuntime()
        linkerArgs.add(project.file("../common/build/$target/hash.bc").path)
    }

    create("mimalloc") {
        language = CompileToBitcode.Language.C
        includeFiles = listOf("**/*.c")
        excludeFiles += listOf("**/alloc-override*.c", "**/page-queue.c", "**/static.c")
        if (!targetSupportsMimallocAllocator(target))
            excludedTargets.add(target)
        srcDir = File(srcRoot, "c")
        compilerArgs.add("-DKONAN_MI_MALLOC=1")
        headersDir = File(srcDir, "include")
    }

    create("launcher") {
        includeRuntime()
    }

    create("debug") {
        includeRuntime()
    }

    create("std_alloc")
    create("opt_alloc")

    create("exceptionsSupport", file("src/exceptions_support")) { // TODO: Fix naming?
        includeRuntime()
    }

    create("release") {
        includeRuntime()
    }

    create("strict") {
        includeRuntime()
    }

    create("relaxed") {
        includeRuntime()
    }

    create("profileRuntime", file("src/profile_runtime")) // TODO: Fix naming?

    create("objc") {
        includeRuntime()
    }

    create("test_support") {
        includeRuntime()
        compilerArgs.add(
                "-I" + project.rootProject.file("third_party/googletest/googletest/googletest/include")
        )
        compilerArgs.add(
                "-I" + project.rootProject.file("third_party/googletest/googletest/googlemock/include")
        )
    }
}

targetList.forEach { targetName ->
    createTestTask(
            project,
            "${targetName}StdAllocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}TestSupport",
                "${targetName}Strict",
                "${targetName}Release",
                "${targetName}StdAlloc"
            )
    )

    createTestTask(
            project,
            "${targetName}MimallocRuntimeTests",
            listOf(
                "${targetName}Runtime",
                "${targetName}TestSupport",
                "${targetName}Strict",
                "${targetName}Release",
                "${targetName}Mimalloc",
                "${targetName}OptAlloc"
            )
    )

    tasks.register("${targetName}RuntimeTests") {
        dependsOn("${targetName}StdAllocRuntimeTests")
        dependsOn("${targetName}MimallocRuntimeTests")
    }
}

val hostRuntime by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    dependsOn("${hostName}RuntimeTests")
}

val clean by tasks.registering {
    doLast {
        delete(buildDir)
    }
}

val generateJsMath by tasks.registering {
    dependsOn(":distCompiler")
    doLast {
        val distDir: File by project
        val jsinteropScript = if (PlatformInfo.isWindows()) "jsinterop.bat" else "jsinterop"
        val jsinterop = "$distDir/bin/$jsinteropScript"
        val targetDir = "$buildDir/generated"

        project.exec {
            commandLine(
                    jsinterop,
                    "-pkg", "kotlinx.interop.wasm.math",
                    "-o", "$targetDir/math",
                    "-target", "wasm32"
            )
        }

        val generated = file("$targetDir/math-build/natives/js_stubs.js")
        val mathJs = file("src/main/js/math.js")
        mathJs.writeText(
            "// NOTE: THIS FILE IS AUTO-GENERATED!\n" +
            "// Run ':runtime:generateJsMath' to re-generate it.\n\n"
        )
        mathJs.appendText(generated.readText())
    }
}
