/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.io.json
import net.minecraft.client.gui.FontRenderer
import java.awt.Color

/**
 * A container of the values
 */
open class Configurable(
    name: String
) : Value<MutableList<Value<*>>>(
    name, mutableListOf()
) {

    val values: List<Value<*>>
        get() = this.get()

    fun addValue(value: Value<*>) = apply {
        get().add(value)
        value.owner = this
    }

    fun addValues(values: Collection<Value<*>>) = apply {
        get().addAll(values)
        values.forEach { it.owner = this }
    }

    operator fun <T, V : Value<T>> V.unaryPlus() = apply(::addValue)

    override fun toJson(): JsonElement = json {
        for (value in values) {
            if (value.excluded) {
                continue
            }

            value.name to value.toJson()
        }
    }

    override fun fromJsonF(element: JsonElement): MutableList<Value<*>>? {
        element as JsonObject

        val values = get()
        // Set all sub values from the JSON object
        for ((valueName, value) in element.entrySet()) {
            values.find { it.name.equals(valueName, true) }?.fromJson(value)
        }

        return values
    }

    override fun toText(): String {
        TODO("Not yet implemented")
    }

    override fun fromTextF(text: String): MutableList<Value<*>>? {
        TODO("Not yet implemented")
    }

    fun int(
        name: String, value: Int, range: IntRange, suffix: String? = null,
        description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +IntValue(name, value, range, suffix, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun float(
        name: String, value: Float, range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
        suffix: String? = null, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +FloatValue(name, value, range, suffix, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun choices(
        name: String,
        values: Array<String>,
        value: String,
        description: String? = null,
        isSupported: (() -> Boolean)? = null
    ) = +ListValue(name, values.toList(), value, description) { isSupported?.invoke() ?: true }.apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun multiChoices(
        name: String,
        values: Array<String>,
        selectedValues: Array<String> = emptyArray(),
        forceChosenChoices: Array<String> = emptyArray(),
        description: String? = null,
        isSupported: (() -> Boolean)? = null
    ) = +MultiChoiceValue(
        name,
        values.toList(),
        selectedValues.toList(),
        description,
        { isSupported?.invoke() ?: true },
        forceChosenChoices.toList()
    ).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun block(
        name: String, value: Int, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +BlockValue(name, value, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun font(
        name: String, value: FontRenderer, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +FontValue(name, value, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun text(
        name: String, value: String, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +TextValue(name, value, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun boolean(
        name: String, value: Boolean, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +BoolValue(name, value, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun intRange(
        name: String, value: IntRange, range: IntRange, suffix: String? = null,
        description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +IntRangeValue(name, value, range, suffix, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun floatRange(
        name: String, value: ClosedFloatingPointRange<Float>, range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
        suffix: String? = null, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +FloatRangeValue(name, value, range, suffix, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun color(
        name: String, value: Color, rainbow: Boolean = false, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = +ColorValue(name, value, rainbow, description).apply {
        if (isSupported != null) setSupport { isSupported.invoke() }
    }

    fun color(
        name: String, value: Int, rainbow: Boolean = false, description: String? = null, isSupported: (() -> Boolean)? = null
    ) = color(name, Color(value, true), rainbow, description, isSupported)
}