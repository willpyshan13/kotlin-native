/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Types.h"

// TODO: Populate the file with more accurate values.

namespace {

TypeInfo theAnyTypeInfoImpl = {};
TypeInfo theArrayTypeInfoImpl = {};
TypeInfo theBooleanArrayTypeInfoImpl = {};
TypeInfo theByteArrayTypeInfoImpl = {};
TypeInfo theCharArrayTypeInfoImpl = {};
TypeInfo theDoubleArrayTypeInfoImpl = {};
TypeInfo theFloatArrayTypeInfoImpl = {};
TypeInfo theForeignObjCObjectTypeInfoImpl = {};
TypeInfo theFreezableAtomicReferenceTypeInfoImpl = {};
TypeInfo theIntArrayTypeInfoImpl = {};
TypeInfo theLongArrayTypeInfoImpl = {};
TypeInfo theNativePtrArrayTypeInfoImpl = {};
TypeInfo theObjCObjectWrapperTypeInfoImpl = {};
TypeInfo theOpaqueFunctionTypeInfoImpl = {};
TypeInfo theShortArrayTypeInfoImpl = {};
TypeInfo theStringTypeInfoImpl = {};
TypeInfo theThrowableTypeInfoImpl = {};
TypeInfo theUnitTypeInfoImpl = {};
TypeInfo theWorkerBoundReferenceTypeInfoImpl = {};

template <class T>
struct KBox {
    ObjHeader header;
    const T value;
};

} // namespace

extern "C" {

extern const int KonanNeedDebugInfo = 0;

extern const TypeInfo* theAnyTypeInfo = &theAnyTypeInfoImpl;
extern const TypeInfo* theArrayTypeInfo = &theArrayTypeInfoImpl;
extern const TypeInfo* theBooleanArrayTypeInfo = &theBooleanArrayTypeInfoImpl;
extern const TypeInfo* theByteArrayTypeInfo = &theByteArrayTypeInfoImpl;
extern const TypeInfo* theCharArrayTypeInfo = &theCharArrayTypeInfoImpl;
extern const TypeInfo* theDoubleArrayTypeInfo = &theDoubleArrayTypeInfoImpl;
extern const TypeInfo* theFloatArrayTypeInfo = &theFloatArrayTypeInfoImpl;
extern const TypeInfo* theForeignObjCObjectTypeInfo = &theForeignObjCObjectTypeInfoImpl;
extern const TypeInfo* theFreezableAtomicReferenceTypeInfo = &theFreezableAtomicReferenceTypeInfoImpl;
extern const TypeInfo* theIntArrayTypeInfo = &theIntArrayTypeInfoImpl;
extern const TypeInfo* theLongArrayTypeInfo = &theLongArrayTypeInfoImpl;
extern const TypeInfo* theNativePtrArrayTypeInfo = &theNativePtrArrayTypeInfoImpl;
extern const TypeInfo* theObjCObjectWrapperTypeInfo = &theObjCObjectWrapperTypeInfoImpl;
extern const TypeInfo* theOpaqueFunctionTypeInfo = &theOpaqueFunctionTypeInfoImpl;
extern const TypeInfo* theShortArrayTypeInfo = &theShortArrayTypeInfoImpl;
extern const TypeInfo* theStringTypeInfo = &theStringTypeInfoImpl;
extern const TypeInfo* theThrowableTypeInfo = &theThrowableTypeInfoImpl;
extern const TypeInfo* theUnitTypeInfo = &theUnitTypeInfoImpl;
extern const TypeInfo* theWorkerBoundReferenceTypeInfo = &theWorkerBoundReferenceTypeInfoImpl;

extern const ObjHeader theEmptyArray = {};

OBJ_GETTER(makeWeakReferenceCounter, void*) {
    return nullptr;
}

OBJ_GETTER(makePermanentWeakReferenceImpl, void*) {
    return nullptr;
}

OBJ_GETTER(makeObjCWeakReferenceImpl, void*) {
    return nullptr;
}

void checkRangeIndexes(KInt from, KInt to, KInt size) {}

OBJ_GETTER(WorkerLaunchpad, KRef) {
    return nullptr;
}

void RUNTIME_NORETURN ThrowWorkerInvalidState() {
    throw 0;
}

void RUNTIME_NORETURN ThrowNullPointerException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowArrayIndexOutOfBoundsException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowClassCastException(const ObjHeader* instance, const TypeInfo* type_info) {
    throw 0;
}

void RUNTIME_NORETURN ThrowArithmeticException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowNumberFormatException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowOutOfMemoryError() {
    throw 0;
}

void RUNTIME_NORETURN ThrowNotImplementedError() {
    throw 0;
}

void RUNTIME_NORETURN ThrowCharacterCodingException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowIllegalArgumentException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowIllegalStateException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowInvalidMutabilityException(KConstRef where) {
    throw 0;
}

void RUNTIME_NORETURN ThrowIncorrectDereferenceException() {
    throw 0;
}

void RUNTIME_NORETURN ThrowIllegalObjectSharingException(KConstNativePtr typeInfo, KConstNativePtr address) {
    throw 0;
}

void RUNTIME_NORETURN ThrowFreezingException(KRef toFreeze, KRef blocker) {
    throw 0;
}

OBJ_GETTER0(TheEmptyString) {
    return nullptr;
}

void ReportUnhandledException(KRef throwable) {}

OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address) {
    return nullptr;
}

void ExceptionReporterLaunchpad(KRef reporter, KRef throwable) {}

void Kotlin_WorkerBoundReference_freezeHook(KRef thiz) {}

OBJ_GETTER(Kotlin_boxBoolean, KBoolean value) {
    return nullptr;
}

extern const KBoolean BOOLEAN_RANGE_FROM = false;
extern const KBoolean BOOLEAN_RANGE_TO = true;
extern KBox<KBoolean> BOOLEAN_CACHE[] = {
        {{}, false},
        {{}, true},
};

extern const KByte BYTE_RANGE_FROM = -1;
extern const KByte BYTE_RANGE_TO = 1;
extern KBox<KByte> BYTE_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KChar CHAR_RANGE_FROM = 0;
extern const KChar CHAR_RANGE_TO = 2;
extern KBox<KChar> CHAR_CACHE[] = {
        {{}, 0},
        {{}, 1},
        {{}, 2},
};

extern const KShort SHORT_RANGE_FROM = -1;
extern const KShort SHORT_RANGE_TO = 1;
extern KBox<KShort> SHORT_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KInt INT_RANGE_FROM = -1;
extern const KInt INT_RANGE_TO = 1;
extern KBox<KInt> INT_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KLong LONG_RANGE_FROM = -1;
extern const KLong LONG_RANGE_TO = 1;
extern KBox<KLong> LONG_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

OBJ_GETTER(Kotlin_Throwable_getMessage, KRef throwable) {
    return nullptr;
}

} // extern "C"
