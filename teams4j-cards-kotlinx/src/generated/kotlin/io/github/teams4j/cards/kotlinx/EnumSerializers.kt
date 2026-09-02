// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/** `ActionMode`, matched case-insensitively. */
public object ActionModeSerializer : CardSerializer<ActionMode>("ActionMode") {

    private val byJson: Map<String, ActionMode> = mapOf(
        "primary" to ActionMode.PRIMARY,
        "secondary" to ActionMode.SECONDARY,
    )

    private val toWire: Map<ActionMode, String> = mapOf(
        ActionMode.PRIMARY to "primary",
        ActionMode.SECONDARY to "secondary",
    )

    override fun toJson(value: ActionMode): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ActionMode? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `ActionStyle`, matched case-insensitively. */
public object ActionStyleSerializer : CardSerializer<ActionStyle>("ActionStyle") {

    private val byJson: Map<String, ActionStyle> = mapOf(
        "default" to ActionStyle.DEFAULT,
        "positive" to ActionStyle.POSITIVE,
        "destructive" to ActionStyle.DESTRUCTIVE,
    )

    private val toWire: Map<ActionStyle, String> = mapOf(
        ActionStyle.DEFAULT to "default",
        ActionStyle.POSITIVE to "positive",
        ActionStyle.DESTRUCTIVE to "destructive",
    )

    override fun toJson(value: ActionStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ActionStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `AssociatedInputs`, matched case-insensitively. */
public object AssociatedInputsSerializer : CardSerializer<AssociatedInputs>("AssociatedInputs") {

    private val byJson: Map<String, AssociatedInputs> = mapOf(
        "auto" to AssociatedInputs.AUTO,
        "none" to AssociatedInputs.NONE,
    )

    private val toWire: Map<AssociatedInputs, String> = mapOf(
        AssociatedInputs.AUTO to "Auto",
        AssociatedInputs.NONE to "None",
    )

    override fun toJson(value: AssociatedInputs): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): AssociatedInputs? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `BlockElementHeight`, matched case-insensitively. */
public object BlockElementHeightSerializer : CardSerializer<BlockElementHeight>("BlockElementHeight") {

    private val byJson: Map<String, BlockElementHeight> = mapOf(
        "auto" to BlockElementHeight.AUTO,
        "stretch" to BlockElementHeight.STRETCH,
    )

    private val toWire: Map<BlockElementHeight, String> = mapOf(
        BlockElementHeight.AUTO to "auto",
        BlockElementHeight.STRETCH to "stretch",
    )

    override fun toJson(value: BlockElementHeight): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): BlockElementHeight? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `ChoiceInputStyle`, matched case-insensitively. */
public object ChoiceInputStyleSerializer : CardSerializer<ChoiceInputStyle>("ChoiceInputStyle") {

    private val byJson: Map<String, ChoiceInputStyle> = mapOf(
        "compact" to ChoiceInputStyle.COMPACT,
        "expanded" to ChoiceInputStyle.EXPANDED,
        "filtered" to ChoiceInputStyle.FILTERED,
    )

    private val toWire: Map<ChoiceInputStyle, String> = mapOf(
        ChoiceInputStyle.COMPACT to "compact",
        ChoiceInputStyle.EXPANDED to "expanded",
        ChoiceInputStyle.FILTERED to "filtered",
    )

    override fun toJson(value: ChoiceInputStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ChoiceInputStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `Colors`, matched case-insensitively. */
public object ColorsSerializer : CardSerializer<Colors>("Colors") {

    private val byJson: Map<String, Colors> = mapOf(
        "default" to Colors.DEFAULT,
        "dark" to Colors.DARK,
        "light" to Colors.LIGHT,
        "accent" to Colors.ACCENT,
        "good" to Colors.GOOD,
        "warning" to Colors.WARNING,
        "attention" to Colors.ATTENTION,
    )

    private val toWire: Map<Colors, String> = mapOf(
        Colors.DEFAULT to "default",
        Colors.DARK to "dark",
        Colors.LIGHT to "light",
        Colors.ACCENT to "accent",
        Colors.GOOD to "good",
        Colors.WARNING to "warning",
        Colors.ATTENTION to "attention",
    )

    override fun toJson(value: Colors): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): Colors? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `ContainerStyle`, matched case-insensitively. */
public object ContainerStyleSerializer : CardSerializer<ContainerStyle>("ContainerStyle") {

    private val byJson: Map<String, ContainerStyle> = mapOf(
        "default" to ContainerStyle.DEFAULT,
        "emphasis" to ContainerStyle.EMPHASIS,
        "good" to ContainerStyle.GOOD,
        "attention" to ContainerStyle.ATTENTION,
        "warning" to ContainerStyle.WARNING,
        "accent" to ContainerStyle.ACCENT,
    )

    private val toWire: Map<ContainerStyle, String> = mapOf(
        ContainerStyle.DEFAULT to "default",
        ContainerStyle.EMPHASIS to "emphasis",
        ContainerStyle.GOOD to "good",
        ContainerStyle.ATTENTION to "attention",
        ContainerStyle.WARNING to "warning",
        ContainerStyle.ACCENT to "accent",
    )

    override fun toJson(value: ContainerStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ContainerStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `FontSize`, matched case-insensitively. */
public object FontSizeSerializer : CardSerializer<FontSize>("FontSize") {

    private val byJson: Map<String, FontSize> = mapOf(
        "default" to FontSize.DEFAULT,
        "small" to FontSize.SMALL,
        "medium" to FontSize.MEDIUM,
        "large" to FontSize.LARGE,
        "extralarge" to FontSize.EXTRA_LARGE,
    )

    private val toWire: Map<FontSize, String> = mapOf(
        FontSize.DEFAULT to "default",
        FontSize.SMALL to "small",
        FontSize.MEDIUM to "medium",
        FontSize.LARGE to "large",
        FontSize.EXTRA_LARGE to "extraLarge",
    )

    override fun toJson(value: FontSize): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): FontSize? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `FontType`, matched case-insensitively. */
public object FontTypeSerializer : CardSerializer<FontType>("FontType") {

    private val byJson: Map<String, FontType> = mapOf(
        "default" to FontType.DEFAULT,
        "monospace" to FontType.MONOSPACE,
    )

    private val toWire: Map<FontType, String> = mapOf(
        FontType.DEFAULT to "default",
        FontType.MONOSPACE to "monospace",
    )

    override fun toJson(value: FontType): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): FontType? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `FontWeight`, matched case-insensitively. */
public object FontWeightSerializer : CardSerializer<FontWeight>("FontWeight") {

    private val byJson: Map<String, FontWeight> = mapOf(
        "default" to FontWeight.DEFAULT,
        "lighter" to FontWeight.LIGHTER,
        "bolder" to FontWeight.BOLDER,
    )

    private val toWire: Map<FontWeight, String> = mapOf(
        FontWeight.DEFAULT to "default",
        FontWeight.LIGHTER to "lighter",
        FontWeight.BOLDER to "bolder",
    )

    override fun toJson(value: FontWeight): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): FontWeight? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `HorizontalAlignment`, matched case-insensitively. */
public object HorizontalAlignmentSerializer : CardSerializer<HorizontalAlignment>("HorizontalAlignment") {

    private val byJson: Map<String, HorizontalAlignment> = mapOf(
        "left" to HorizontalAlignment.LEFT,
        "center" to HorizontalAlignment.CENTER,
        "right" to HorizontalAlignment.RIGHT,
    )

    private val toWire: Map<HorizontalAlignment, String> = mapOf(
        HorizontalAlignment.LEFT to "left",
        HorizontalAlignment.CENTER to "center",
        HorizontalAlignment.RIGHT to "right",
    )

    override fun toJson(value: HorizontalAlignment): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): HorizontalAlignment? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `ImageFillMode`, matched case-insensitively. */
public object ImageFillModeSerializer : CardSerializer<ImageFillMode>("ImageFillMode") {

    private val byJson: Map<String, ImageFillMode> = mapOf(
        "cover" to ImageFillMode.COVER,
        "repeathorizontally" to ImageFillMode.REPEAT_HORIZONTALLY,
        "repeatvertically" to ImageFillMode.REPEAT_VERTICALLY,
        "repeat" to ImageFillMode.REPEAT,
    )

    private val toWire: Map<ImageFillMode, String> = mapOf(
        ImageFillMode.COVER to "cover",
        ImageFillMode.REPEAT_HORIZONTALLY to "repeatHorizontally",
        ImageFillMode.REPEAT_VERTICALLY to "repeatVertically",
        ImageFillMode.REPEAT to "repeat",
    )

    override fun toJson(value: ImageFillMode): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ImageFillMode? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `ImageSize`, matched case-insensitively. */
public object ImageSizeSerializer : CardSerializer<ImageSize>("ImageSize") {

    private val byJson: Map<String, ImageSize> = mapOf(
        "auto" to ImageSize.AUTO,
        "stretch" to ImageSize.STRETCH,
        "small" to ImageSize.SMALL,
        "medium" to ImageSize.MEDIUM,
        "large" to ImageSize.LARGE,
    )

    private val toWire: Map<ImageSize, String> = mapOf(
        ImageSize.AUTO to "auto",
        ImageSize.STRETCH to "stretch",
        ImageSize.SMALL to "small",
        ImageSize.MEDIUM to "medium",
        ImageSize.LARGE to "large",
    )

    override fun toJson(value: ImageSize): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ImageSize? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `ImageStyle`, matched case-insensitively. */
public object ImageStyleSerializer : CardSerializer<ImageStyle>("ImageStyle") {

    private val byJson: Map<String, ImageStyle> = mapOf(
        "default" to ImageStyle.DEFAULT,
        "person" to ImageStyle.PERSON,
    )

    private val toWire: Map<ImageStyle, String> = mapOf(
        ImageStyle.DEFAULT to "default",
        ImageStyle.PERSON to "person",
    )

    override fun toJson(value: ImageStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): ImageStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `InputLabelPosition`, matched case-insensitively. */
public object InputLabelPositionSerializer : CardSerializer<InputLabelPosition>("InputLabelPosition") {

    private val byJson: Map<String, InputLabelPosition> = mapOf(
        "inline" to InputLabelPosition.INLINE,
        "above" to InputLabelPosition.ABOVE,
    )

    private val toWire: Map<InputLabelPosition, String> = mapOf(
        InputLabelPosition.INLINE to "inline",
        InputLabelPosition.ABOVE to "above",
    )

    override fun toJson(value: InputLabelPosition): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): InputLabelPosition? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `InputStyle`, matched case-insensitively. */
public object InputStyleSerializer : CardSerializer<InputStyle>("InputStyle") {

    private val byJson: Map<String, InputStyle> = mapOf(
        "revealonhover" to InputStyle.REVEAL_ON_HOVER,
        "default" to InputStyle.DEFAULT,
    )

    private val toWire: Map<InputStyle, String> = mapOf(
        InputStyle.REVEAL_ON_HOVER to "revealOnHover",
        InputStyle.DEFAULT to "default",
    )

    override fun toJson(value: InputStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): InputStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `Spacing`, matched case-insensitively. */
public object SpacingSerializer : CardSerializer<Spacing>("Spacing") {

    private val byJson: Map<String, Spacing> = mapOf(
        "default" to Spacing.DEFAULT,
        "none" to Spacing.NONE,
        "small" to Spacing.SMALL,
        "medium" to Spacing.MEDIUM,
        "large" to Spacing.LARGE,
        "extralarge" to Spacing.EXTRA_LARGE,
        "padding" to Spacing.PADDING,
    )

    private val toWire: Map<Spacing, String> = mapOf(
        Spacing.DEFAULT to "default",
        Spacing.NONE to "none",
        Spacing.SMALL to "small",
        Spacing.MEDIUM to "medium",
        Spacing.LARGE to "large",
        Spacing.EXTRA_LARGE to "extraLarge",
        Spacing.PADDING to "padding",
    )

    override fun toJson(value: Spacing): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): Spacing? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `TextBlockStyle`, matched case-insensitively. */
public object TextBlockStyleSerializer : CardSerializer<TextBlockStyle>("TextBlockStyle") {

    private val byJson: Map<String, TextBlockStyle> = mapOf(
        "default" to TextBlockStyle.DEFAULT,
        "heading" to TextBlockStyle.HEADING,
    )

    private val toWire: Map<TextBlockStyle, String> = mapOf(
        TextBlockStyle.DEFAULT to "default",
        TextBlockStyle.HEADING to "heading",
    )

    override fun toJson(value: TextBlockStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): TextBlockStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `TextInputStyle`, matched case-insensitively. */
public object TextInputStyleSerializer : CardSerializer<TextInputStyle>("TextInputStyle") {

    private val byJson: Map<String, TextInputStyle> = mapOf(
        "text" to TextInputStyle.TEXT,
        "tel" to TextInputStyle.TEL,
        "url" to TextInputStyle.URL,
        "email" to TextInputStyle.EMAIL,
        "password" to TextInputStyle.PASSWORD,
    )

    private val toWire: Map<TextInputStyle, String> = mapOf(
        TextInputStyle.TEXT to "text",
        TextInputStyle.TEL to "tel",
        TextInputStyle.URL to "url",
        TextInputStyle.EMAIL to "email",
        TextInputStyle.PASSWORD to "password",
    )

    override fun toJson(value: TextInputStyle): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): TextInputStyle? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `VerticalAlignment`, matched case-insensitively. */
public object VerticalAlignmentSerializer : CardSerializer<VerticalAlignment>("VerticalAlignment") {

    private val byJson: Map<String, VerticalAlignment> = mapOf(
        "top" to VerticalAlignment.TOP,
        "center" to VerticalAlignment.CENTER,
        "bottom" to VerticalAlignment.BOTTOM,
    )

    private val toWire: Map<VerticalAlignment, String> = mapOf(
        VerticalAlignment.TOP to "top",
        VerticalAlignment.CENTER to "center",
        VerticalAlignment.BOTTOM to "bottom",
    )

    override fun toJson(value: VerticalAlignment): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): VerticalAlignment? = byJson[element.asString()?.lowercase() ?: return null]
}

/** `VerticalContentAlignment`, matched case-insensitively. */
public object VerticalContentAlignmentSerializer : CardSerializer<VerticalContentAlignment>("VerticalContentAlignment") {

    private val byJson: Map<String, VerticalContentAlignment> = mapOf(
        "top" to VerticalContentAlignment.TOP,
        "center" to VerticalContentAlignment.CENTER,
        "bottom" to VerticalContentAlignment.BOTTOM,
    )

    private val toWire: Map<VerticalContentAlignment, String> = mapOf(
        VerticalContentAlignment.TOP to "top",
        VerticalContentAlignment.CENTER to "center",
        VerticalContentAlignment.BOTTOM to "bottom",
    )

    override fun toJson(value: VerticalContentAlignment): JsonElement = JsonPrimitive(toWire[value] ?: value.name)

    // An unrecognised value reads as null rather than failing: the official
    // samples carry deliberately invalid ones to exercise renderer fallback.
    override fun fromJson(element: JsonElement): VerticalContentAlignment? = byJson[element.asString()?.lowercase() ?: return null]
}

