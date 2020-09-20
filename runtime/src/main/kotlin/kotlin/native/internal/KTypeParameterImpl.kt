/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.*

internal data class KTypeParameterImpl(
        override val name: String,
        val containerFqName: String,
        override val upperBounds: List<KType>,
        override val variance: KVariance,
        override val isReified: Boolean
) : KTypeParameter {
    override fun toString(): String = when (variance) {
        KVariance.INVARIANT -> ""
        KVariance.IN -> "in "
        KVariance.OUT -> "out "
    } + name
}