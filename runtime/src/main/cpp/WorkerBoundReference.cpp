/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "WorkerBoundReference.h"

#include "Alloc.h"
#include "Memory.h"
#include "MemorySharedRefs.hpp"

// Defined in WorkerBoundReference.kt
extern "C" void Kotlin_WorkerBoundReference_freezeHook(KRef thiz);

RUNTIME_NOTHROW void WorkerBoundReferenceFreezeHook(KRef thiz) {
  Kotlin_WorkerBoundReference_freezeHook(thiz);
}

extern "C" {

KNativePtr Kotlin_WorkerBoundReference_create(KRef value) {
  auto* holder = konanConstructInstance<KRefSharedHolder>();
  holder->init(value);
  return holder;
}

OBJ_GETTER(Kotlin_WorkerBoundReference_deref, KNativePtr holder) {
  RETURN_OBJ(reinterpret_cast<KRefSharedHolder*>(holder)->ref<ErrorPolicy::kDefaultValue>());
}

OBJ_GETTER(Kotlin_WorkerBoundReference_describe, KNativePtr holder) {
  RETURN_RESULT_OF0(reinterpret_cast<KRefSharedHolder*>(holder)->describe);
}

RUNTIME_NOTHROW void Kotlin_WorkerBoundReference_clean(KNativePtr holder) {
    auto* typedHolder = reinterpret_cast<KRefSharedHolder*>(holder);
    typedHolder->dispose();
    konanDestructInstance(typedHolder);
}
}
