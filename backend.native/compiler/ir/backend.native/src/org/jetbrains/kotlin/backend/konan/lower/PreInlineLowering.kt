/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isTypeOfIntrinsic
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass runs before inlining and performs the following additional transformations over some operations:
 *     - Assertion call removal.
 */
internal class PreInlineLowering(val context: Context) : BodyLoweringPass {

    private val symbols get() = context.ir.symbols

    private val kTypeGenerator = KTypeGenerator(context)

    private val asserts = symbols.asserts
    private val enableAssertions = context.config.configuration.getBoolean(KonanConfigKeys.ENABLE_ASSERTIONS)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        var symbolOwner = container
        while (symbolOwner !is IrSymbolOwner)
            symbolOwner = symbolOwner.parent as IrDeclaration
        val builder = context.createIrBuilder(symbolOwner.symbol, irBody.startOffset, irBody.endOffset)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                // Replace assert() call with an empty composite if assertions are not enabled.
                if (!enableAssertions && expression.symbol in asserts) {
                    assert(expression.type.isUnit())
                    return IrCompositeImpl(expression.startOffset, expression.endOffset, expression.type)
                } else if (expression.symbol.owner.isTypeOfIntrinsic()) {
                    val type = expression.getTypeArgument(0)
                            ?: org.jetbrains.kotlin.backend.konan.error(container.file, expression, "missing type argument")
                    return with (kTypeGenerator) { builder.at(expression).irKType(type) }
                }

                return expression
            }
        })
    }
}