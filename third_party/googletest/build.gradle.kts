/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("compile-to-bitcode")
}

bitcode {
    create("googletest") {
        srcDir = file("googletest/googletest/src")
        headersDir = file("googletest/googletest")
        compilerArgs.add("-I" + file("googletest/googletest/include"))
        includeFiles = listOf("*.cc")
        excludeFiles = listOf("gtest-all.cc", "gtest_main.cc")
    }

    create("googlemock") {
        srcDir = file("googletest/googlemock/src")
        headersDir = file("googletest/googlemock")
        compilerArgs.add("-I" + file("googletest/googlemock/include"))
        compilerArgs.add("-I" + file("googletest/googletest/include"))
        includeFiles = listOf("*.cc")
        excludeFiles = listOf("gmock-all.cc", "gmock_main.cc")
    }
}

val hostName: String by project

val build by tasks.registering {
    dependsOn("${hostName}Googletest")
    dependsOn("${hostName}Googlemock")
}
