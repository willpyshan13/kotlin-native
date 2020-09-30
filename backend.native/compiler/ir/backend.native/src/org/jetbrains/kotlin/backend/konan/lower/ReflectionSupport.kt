/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.ir.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.ir.typeWithoutArguments
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.backend.konan.llvm.fullName
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.Variance

internal class KTypeGenerator(val context: KonanBackendContext, val irFile: IrFile, val irElement: IrElement) {
    private val symbols = context.ir.symbols

    fun IrBuilderWithScope.irKType(type: IrType, needExactTypeParameters: Boolean = false): IrExpression {
        return irKType(type, needExactTypeParameters, mutableSetOf())
    }

    private class RecursiveBoundsException(message: String) : Throwable(message)

    private fun IrBuilderWithScope.irKType(
            type: IrType,
            needExactTypeParameters: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>,
    ): IrExpression = if (type !is IrSimpleType) {
        // Represent as non-denotable type:
        irKTypeImpl(
                kClassifier = irNull(),
                irTypeArguments = emptyList(),
                isMarkedNullable = false,
                needExactTypeParameters = needExactTypeParameters,
                seenTypeParameters = seenTypeParameters
        )
    } else try {
        val kClassifier = when (val classifier = type.classifier) {
            is IrClassSymbol -> irKClass(classifier)
            is IrTypeParameterSymbol -> irKTypeParameter(classifier.owner, needExactTypeParameters, seenTypeParameters)
            else -> TODO("Unexpected classifier: $classifier")
        }

        irKTypeImpl(
                kClassifier = kClassifier,
                irTypeArguments = type.arguments,
                isMarkedNullable = type.hasQuestionMark,
                needExactTypeParameters = needExactTypeParameters,
                seenTypeParameters = seenTypeParameters
        )
    } catch (t: RecursiveBoundsException) {
        if (needExactTypeParameters)
            this@KTypeGenerator.context.reportCompilationError(t.message!!, irFile, irElement)
        irCall(symbols.kTypeImplForTypeParametersWithRecursiveBounds.constructors.single())
    }

    private fun IrBuilderWithScope.irKTypeImpl(
            kClassifier: IrExpression,
            irTypeArguments: List<IrTypeArgument>,
            isMarkedNullable: Boolean,
            needExactTypeParameters: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): IrExpression = irCall(symbols.kTypeImpl.constructors.single()).apply {
        putValueArgument(0, kClassifier)
        putValueArgument(1, irKTypeProjectionsList(irTypeArguments, needExactTypeParameters, seenTypeParameters))
        putValueArgument(2, irBoolean(isMarkedNullable))
    }

    private fun IrBuilderWithScope.irKClass(symbol: IrClassSymbol) = irKClass(this@KTypeGenerator.context, symbol)

    private fun IrBuilderWithScope.irKTypeParameter(
            typeParameter: IrTypeParameter,
            needExactTypeParameters: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ): IrMemberAccessExpression<*> {
        if (!seenTypeParameters.add(typeParameter))
            throw RecursiveBoundsException("Non-reified type parameters with recursive bounds are not supported yet: ${typeParameter.render()}")
        val result = irCall(symbols.kTypeParameterImpl.constructors.single()).apply {
            putValueArgument(0, irString(typeParameter.name.asString()))
            putValueArgument(1, irString(typeParameter.parentUniqueName))
            putValueArgument(2, irKTypeList(typeParameter.superTypes, needExactTypeParameters, seenTypeParameters))
            putValueArgument(3, irKVariance(typeParameter.variance))
            putValueArgument(4, irBoolean(typeParameter.isReified))
        }
        seenTypeParameters.remove(typeParameter)
        return result
    }

    private val IrTypeParameter.parentUniqueName get() = when (val parent = parent) {
        is IrFunction -> parent.fullName
        else -> parent.fqNameForIrSerialization.asString()
    }

    private fun IrBuilderWithScope.irKVariance(variance: Variance) =
            IrGetEnumValueImpl(
                    startOffset, endOffset,
                    symbols.kVariance.defaultType,
                    symbols.kVariance.owner.declarations.filterIsInstance<IrEnumEntry>()[variance.ordinal].symbol
            )

    private fun <T> IrBuilderWithScope.irKTypeLikeList(
            types: List<T>,
            itemType: IrType,
            itemBuilder: (T) -> IrExpression
    ) = if (types.isEmpty()) {
        irCall(symbols.emptyList, listOf(itemType))
    } else {
        irCall(symbols.listOf, listOf(itemType)).apply {
            putValueArgument(0, IrVarargImpl(
                    startOffset, endOffset,
                    type = symbols.array.typeWith(itemType),
                    varargElementType = itemType,
                    elements = types.map { itemBuilder(it) }
            ))
        }
    }

    private fun IrBuilderWithScope.irKTypeList(
            types: List<IrType>,
            needExactTypeParameters: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ) = irKTypeLikeList(types, symbols.kType.defaultType) {
        irKType(it, needExactTypeParameters, seenTypeParameters)
    }

    private fun IrBuilderWithScope.irKTypeProjectionsList(
            irTypeArguments: List<IrTypeArgument>,
            needExactTypeParameters: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ) = irKTypeLikeList(irTypeArguments, symbols.kTypeProjection.typeWithoutArguments) {
        irKTypeProjection(it, needExactTypeParameters, seenTypeParameters)
    }

    private fun IrBuilderWithScope.irKTypeProjection(
            argument: IrTypeArgument,
            needExactTypeParameters: Boolean,
            seenTypeParameters: MutableSet<IrTypeParameter>
    ) = when (argument) {
        is IrTypeProjection -> irCall(symbols.kTypeProjectionFactories.getValue(argument.variance)).apply {
            dispatchReceiver = irGetObject(symbols.kTypeProjectionCompanion)
            putValueArgument(0, irKType(argument.type, needExactTypeParameters, seenTypeParameters))
        }

        is IrStarProjection -> irCall(symbols.kTypeProjectionStar.owner.getter!!).apply {
            dispatchReceiver = irGetObject(symbols.kTypeProjectionCompanion)
        }

        else -> error("Unexpected IrTypeArgument: $argument (${argument::class})")
    }
}

internal fun IrBuilderWithScope.irKClass(context: KonanBackendContext, symbol: IrClassSymbol): IrExpression {
    val symbols = context.ir.symbols
    return when {
        symbol.descriptor.isObjCClass() ->
            irKClassUnsupported(context, "KClass for Objective-C classes is not supported yet")

        symbol.descriptor.getAllSuperClassifiers().any {
            it is ClassDescriptor && it.fqNameUnsafe == InteropFqNames.nativePointed
        } -> irKClassUnsupported(context, "KClass for interop types is not supported yet")

        else -> irCall(symbols.kClassImplConstructor.owner).apply {
            putValueArgument(0, irCall(symbols.getClassTypeInfo, listOf(symbol.typeWithStarProjections)))
        }
    }
}

private fun IrBuilderWithScope.irKClassUnsupported(context: KonanBackendContext, message: String) =
        irCall(context.ir.symbols.kClassUnsupportedImplConstructor.owner).apply {
            putValueArgument(0, irString(message))
        }
