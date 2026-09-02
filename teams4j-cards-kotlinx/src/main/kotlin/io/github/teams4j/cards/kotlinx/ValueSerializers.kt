package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.ActionFallback
import io.github.teams4j.cards.CardValue
import io.github.teams4j.cards.Column
import io.github.teams4j.cards.ColumnFallback
import io.github.teams4j.cards.Dimension
import io.github.teams4j.cards.ElementFallback
import io.github.teams4j.cards.FallbackDrop
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.math.BigDecimal

/*
 * The model types whose JSON shape no annotation and no generated mapping can describe: an open
 * value, a size that is a number or a string, and a fallback that is a replacement or the word
 * "drop".
 *
 * These are the same five the Jackson binding writes by hand, for the same reason. Which of two
 * shapes is in front of you is a question about the token, and a generator has nothing to say
 * about it.
 *
 * A block comment rather than KDoc: it describes the file, and a KDoc attached to no declaration
 * documents nothing.
 */

/** The open value at `Action.Submit.data` and `Action.Execute.data`. */
public object CardValueSerializer : CardSerializer<CardValue>("CardValue") {
    override fun toJson(value: CardValue): JsonElement =
        when (value) {
            is CardValue.Str -> JsonPrimitive(value.value())
            is CardValue.Num -> JsonPrimitive(value.value())
            is CardValue.Bool -> JsonPrimitive(value.value())
            is CardValue.Arr -> buildJsonArray { value.values().forEach { add(toJson(it)) } }
            is CardValue.Obj -> buildJsonObject { value.entries().forEach { (k, v) -> put(k, toJson(v)) } }
            is CardValue.Null -> JsonNull
        }

    override fun fromJson(element: JsonElement): CardValue =
        when (element) {
            is JsonNull -> CardValue.NULL
            is JsonPrimitive ->
                when {
                    element.isString -> CardValue.of(element.content)
                    element.booleanOrNull != null -> CardValue.of(element.booleanOrNull!!)
                    else -> CardValue.of(BigDecimal(element.content))
                }
            is JsonArray -> CardValue.array(element.map { fromJson(it) })
            is JsonObject -> CardValue.`object`(element.mapValues { (_, v) -> fromJson(v) })
        }
}

/** A width or a label width: a number, or a string such as `"auto"` or `"50px"`. */
public object DimensionSerializer : CardSerializer<Dimension>("Dimension") {
    override fun toJson(value: Dimension): JsonElement =
        when (value) {
            is Dimension.Numeric -> JsonPrimitive(value.value())
            is Dimension.Text -> JsonPrimitive(value.value())
        }

    override fun fromJson(element: JsonElement): Dimension? {
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive is JsonNull) {
            return null
        }
        if (primitive.isString) {
            return Dimension.Text(primitive.content)
        }
        // Read leniently, as the Jackson binding does: an unquoted token that is not a number still
        // has a text form, and keeping it beats refusing a card Teams itself would render.
        return runCatching { Dimension.Numeric(BigDecimal(primitive.content)) }
            .getOrElse { Dimension.Text(primitive.content) }
    }
}

/**
 * The three fallback positions.
 *
 * Each is a replacement or the shared [FallbackDrop.DROP]; only what may stand in differs, so the
 * work is written once and bound to the three content serializers.
 */
private class FallbackSerializer<T : Any, C : Any>(
    name: String,
    private val content: () -> CardSerializer<C>,
    private val wrap: (C) -> T,
    private val unwrap: (T) -> C?,
) : CardSerializer<T>(name) {
    override fun toJson(value: T): JsonElement {
        if (value is FallbackDrop) {
            return JsonPrimitive(FallbackDrop.JSON)
        }
        val replacement = unwrap(value) ?: return JsonNull
        return content().toJson(replacement)
    }

    @Suppress("UNCHECKED_CAST")
    override fun fromJson(element: JsonElement): T? {
        val primitive = element as? JsonPrimitive
        if (primitive != null) {
            if (primitive.isString && primitive.content.trim().equals(FallbackDrop.JSON, ignoreCase = true)) {
                // Safe: FallbackDrop implements all three fallback interfaces, and T is one of them.
                return FallbackDrop.DROP as T
            }
            return null
        }
        return content().fromJson(element)?.let(wrap)
    }
}

/** A fallback for an element: a replacement element, or drop. */
public val ElementFallbackSerializer: CardSerializer<ElementFallback> =
    FallbackSerializer(
        "ElementFallback",
        { CardElementSerializer },
        ElementFallback::of,
        { (it as? ElementFallback.Replacement)?.element() },
    )

/** A fallback for an action: a replacement action, or drop. */
public val ActionFallbackSerializer: CardSerializer<ActionFallback> =
    FallbackSerializer(
        "ActionFallback",
        { CardActionSerializer },
        ActionFallback::of,
        { (it as? ActionFallback.Replacement)?.action() },
    )

/** A fallback for a column: a replacement column, or drop. */
public val ColumnFallbackSerializer: CardSerializer<ColumnFallback> =
    FallbackSerializer<ColumnFallback, Column>(
        "ColumnFallback",
        { ColumnSerializer },
        ColumnFallback::of,
        { (it as? ColumnFallback.Replacement)?.column() },
    )
