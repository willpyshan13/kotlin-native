/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include "Memory.h"
#include "Runtime.h"

// Defined in Cleaner.kt
extern "C" void Kotlin_CleanerImpl_clean(KRef thiz);
extern "C" void Kotlin_CleanerImpl_shutdownCleanerWorker(bool);

namespace {

bool cleanersDisabled = false;

void disposeCleaner(KRef thiz) {
    if (atomicGet(&cleanersDisabled)) {
        if (Kotlin_cleanersLeakCheckerEnabled()) {
            konan::consoleErrorf(
                    "Cleaner %p was disposed after main()\n"
                    "Use `Platform.isCleanersLeakCheckerActive = false` to avoid this check.\n",
                    thiz);
            RuntimeCheck(false, "Terminating now");
        }
        return;
    }

    Kotlin_CleanerImpl_clean(thiz);
}

} // namespace

RUNTIME_NOTHROW void DisposeCleaner(KRef thiz) {
#if KONAN_NO_EXCEPTIONS
    disposeCleaner(thiz);
#else
    try {
        disposeCleaner(thiz);
    } catch (...) {
        // A trick to terminate with unhandled exception. This will print a stack trace
        // and write to iOS crash log.
        std::terminate();
    }
#endif
}

void ShutdownCleaners(bool executeScheduledCleaners) {
    atomicSet(&cleanersDisabled, true);
    Kotlin_CleanerImpl_shutdownCleanerWorker(executeScheduledCleaners);
}
