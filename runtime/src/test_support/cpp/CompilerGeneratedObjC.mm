/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_OBJC_INTEROP

#include <Foundation/NSObject.h>

#include "Types.h"

extern "C" {

Class Kotlin_Interop_getObjCClass(const char* name) {
    return [NSObject class];
}

OBJ_GETTER0(Kotlin_NSEnumeratorAsKIterator_create) {
    return nullptr;
}

void Kotlin_NSEnumeratorAsKIterator_done(KRef thiz) {}

void Kotlin_NSEnumeratorAsKIterator_setNext(KRef thiz, KRef value) {}

OBJ_GETTER(Kotlin_ObjCExport_NSErrorAsExceptionImpl, KRef message, KRef error) {
    return nullptr;
}

void Kotlin_ObjCExport_ThrowCollectionConcurrentModification() {}

void Kotlin_ObjCExport_ThrowCollectionTooLarge() {}

typedef OBJ_GETTER((*convertReferenceFromObjC), id obj);
extern convertReferenceFromObjC* Kotlin_ObjCExport_blockToFunctionConverters = nullptr;
extern int Kotlin_ObjCExport_blockToFunctionConverters_size = 0;

OBJ_GETTER(Kotlin_ObjCExport_createContinuationArgumentImpl, KRef completionHolder, const TypeInfo** exceptionTypes) {
    return nullptr;
}

OBJ_GETTER(Kotlin_ObjCExport_getWrappedError, KRef throwable) {
    return nullptr;
}

void Kotlin_ObjCExport_resumeContinuationFailure(KRef continuation, KRef exception) {}

void Kotlin_ObjCExport_resumeContinuationSuccess(KRef continuation, KRef result) {}

} // extern "C"

#endif // KONAN_OBJC_INTEROP