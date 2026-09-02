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

/** `ImplementationsOf.Action`, dispatched on the `type` discriminator. */
public object CardActionSerializer : CardSerializer<CardAction>("CardAction") {

    override fun toJson(value: CardAction): JsonElement = when (value) {
        is ActionExecute -> ActionExecuteSerializer.toJson(value)
        is ActionOpenUrl -> ActionOpenUrlSerializer.toJson(value)
        is ActionShowCard -> ActionShowCardSerializer.toJson(value)
        is ActionSubmit -> ActionSubmitSerializer.toJson(value)
        is ActionToggleVisibility -> ActionToggleVisibilitySerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "Action.Execute" to Unit,
        "Action.OpenUrl" to Unit,
        "Action.ShowCard" to Unit,
        "Action.Submit" to Unit,
        "Action.ToggleVisibility" to Unit,
    )

    override fun fromJson(element: JsonElement): CardAction? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "Action.Execute" -> ActionExecuteSerializer.fromJson(element)
            "Action.OpenUrl" -> ActionOpenUrlSerializer.fromJson(element)
            "Action.ShowCard" -> ActionShowCardSerializer.fromJson(element)
            "Action.Submit" -> ActionSubmitSerializer.fromJson(element)
            "Action.ToggleVisibility" -> ActionToggleVisibilitySerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a CardAction; known types are ${byType.keys}",
            )
        }
    }
}

/** `ImplementationsOf.Item`, dispatched on the `type` discriminator. */
public object CardItemSerializer : CardSerializer<CardItem>("CardItem") {

    override fun toJson(value: CardItem): JsonElement = when (value) {
        is ActionExecute -> ActionExecuteSerializer.toJson(value)
        is ActionOpenUrl -> ActionOpenUrlSerializer.toJson(value)
        is ActionShowCard -> ActionShowCardSerializer.toJson(value)
        is ActionSubmit -> ActionSubmitSerializer.toJson(value)
        is ActionToggleVisibility -> ActionToggleVisibilitySerializer.toJson(value)
        is ActionSet -> ActionSetSerializer.toJson(value)
        is Column -> ColumnSerializer.toJson(value)
        is ColumnSet -> ColumnSetSerializer.toJson(value)
        is Container -> ContainerSerializer.toJson(value)
        is FactSet -> FactSetSerializer.toJson(value)
        is Image -> ImageSerializer.toJson(value)
        is ImageSet -> ImageSetSerializer.toJson(value)
        is InputChoiceSet -> InputChoiceSetSerializer.toJson(value)
        is InputDate -> InputDateSerializer.toJson(value)
        is InputNumber -> InputNumberSerializer.toJson(value)
        is InputText -> InputTextSerializer.toJson(value)
        is InputTime -> InputTimeSerializer.toJson(value)
        is InputToggle -> InputToggleSerializer.toJson(value)
        is Media -> MediaSerializer.toJson(value)
        is RichTextBlock -> RichTextBlockSerializer.toJson(value)
        is Table -> TableSerializer.toJson(value)
        is TextBlock -> TextBlockSerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "Action.Execute" to Unit,
        "Action.OpenUrl" to Unit,
        "Action.ShowCard" to Unit,
        "Action.Submit" to Unit,
        "Action.ToggleVisibility" to Unit,
        "ActionSet" to Unit,
        "Column" to Unit,
        "ColumnSet" to Unit,
        "Container" to Unit,
        "FactSet" to Unit,
        "Image" to Unit,
        "ImageSet" to Unit,
        "Input.ChoiceSet" to Unit,
        "Input.Date" to Unit,
        "Input.Number" to Unit,
        "Input.Text" to Unit,
        "Input.Time" to Unit,
        "Input.Toggle" to Unit,
        "Media" to Unit,
        "RichTextBlock" to Unit,
        "Table" to Unit,
        "TextBlock" to Unit,
    )

    override fun fromJson(element: JsonElement): CardItem? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "Action.Execute" -> ActionExecuteSerializer.fromJson(element)
            "Action.OpenUrl" -> ActionOpenUrlSerializer.fromJson(element)
            "Action.ShowCard" -> ActionShowCardSerializer.fromJson(element)
            "Action.Submit" -> ActionSubmitSerializer.fromJson(element)
            "Action.ToggleVisibility" -> ActionToggleVisibilitySerializer.fromJson(element)
            "ActionSet" -> ActionSetSerializer.fromJson(element)
            "Column" -> ColumnSerializer.fromJson(element)
            "ColumnSet" -> ColumnSetSerializer.fromJson(element)
            "Container" -> ContainerSerializer.fromJson(element)
            "FactSet" -> FactSetSerializer.fromJson(element)
            "Image" -> ImageSerializer.fromJson(element)
            "ImageSet" -> ImageSetSerializer.fromJson(element)
            "Input.ChoiceSet" -> InputChoiceSetSerializer.fromJson(element)
            "Input.Date" -> InputDateSerializer.fromJson(element)
            "Input.Number" -> InputNumberSerializer.fromJson(element)
            "Input.Text" -> InputTextSerializer.fromJson(element)
            "Input.Time" -> InputTimeSerializer.fromJson(element)
            "Input.Toggle" -> InputToggleSerializer.fromJson(element)
            "Media" -> MediaSerializer.fromJson(element)
            "RichTextBlock" -> RichTextBlockSerializer.fromJson(element)
            "Table" -> TableSerializer.fromJson(element)
            "TextBlock" -> TextBlockSerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a CardItem; known types are ${byType.keys}",
            )
        }
    }
}

/** `ImplementationsOf.ISelectAction`, dispatched on the `type` discriminator. */
public object SelectActionSerializer : CardSerializer<SelectAction>("SelectAction") {

    override fun toJson(value: SelectAction): JsonElement = when (value) {
        is ActionExecute -> ActionExecuteSerializer.toJson(value)
        is ActionOpenUrl -> ActionOpenUrlSerializer.toJson(value)
        is ActionSubmit -> ActionSubmitSerializer.toJson(value)
        is ActionToggleVisibility -> ActionToggleVisibilitySerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "Action.Execute" to Unit,
        "Action.OpenUrl" to Unit,
        "Action.Submit" to Unit,
        "Action.ToggleVisibility" to Unit,
    )

    override fun fromJson(element: JsonElement): SelectAction? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "Action.Execute" -> ActionExecuteSerializer.fromJson(element)
            "Action.OpenUrl" -> ActionOpenUrlSerializer.fromJson(element)
            "Action.Submit" -> ActionSubmitSerializer.fromJson(element)
            "Action.ToggleVisibility" -> ActionToggleVisibilitySerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a SelectAction; known types are ${byType.keys}",
            )
        }
    }
}

/** `ImplementationsOf.Element`, dispatched on the `type` discriminator. */
public object CardElementSerializer : CardSerializer<CardElement>("CardElement") {

    override fun toJson(value: CardElement): JsonElement = when (value) {
        is ActionSet -> ActionSetSerializer.toJson(value)
        is ColumnSet -> ColumnSetSerializer.toJson(value)
        is Container -> ContainerSerializer.toJson(value)
        is FactSet -> FactSetSerializer.toJson(value)
        is Image -> ImageSerializer.toJson(value)
        is ImageSet -> ImageSetSerializer.toJson(value)
        is InputChoiceSet -> InputChoiceSetSerializer.toJson(value)
        is InputDate -> InputDateSerializer.toJson(value)
        is InputNumber -> InputNumberSerializer.toJson(value)
        is InputText -> InputTextSerializer.toJson(value)
        is InputTime -> InputTimeSerializer.toJson(value)
        is InputToggle -> InputToggleSerializer.toJson(value)
        is Media -> MediaSerializer.toJson(value)
        is RichTextBlock -> RichTextBlockSerializer.toJson(value)
        is Table -> TableSerializer.toJson(value)
        is TextBlock -> TextBlockSerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "ActionSet" to Unit,
        "ColumnSet" to Unit,
        "Container" to Unit,
        "FactSet" to Unit,
        "Image" to Unit,
        "ImageSet" to Unit,
        "Input.ChoiceSet" to Unit,
        "Input.Date" to Unit,
        "Input.Number" to Unit,
        "Input.Text" to Unit,
        "Input.Time" to Unit,
        "Input.Toggle" to Unit,
        "Media" to Unit,
        "RichTextBlock" to Unit,
        "Table" to Unit,
        "TextBlock" to Unit,
    )

    override fun fromJson(element: JsonElement): CardElement? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "ActionSet" -> ActionSetSerializer.fromJson(element)
            "ColumnSet" -> ColumnSetSerializer.fromJson(element)
            "Container" -> ContainerSerializer.fromJson(element)
            "FactSet" -> FactSetSerializer.fromJson(element)
            "Image" -> ImageSerializer.fromJson(element)
            "ImageSet" -> ImageSetSerializer.fromJson(element)
            "Input.ChoiceSet" -> InputChoiceSetSerializer.fromJson(element)
            "Input.Date" -> InputDateSerializer.fromJson(element)
            "Input.Number" -> InputNumberSerializer.fromJson(element)
            "Input.Text" -> InputTextSerializer.fromJson(element)
            "Input.Time" -> InputTimeSerializer.fromJson(element)
            "Input.Toggle" -> InputToggleSerializer.fromJson(element)
            "Media" -> MediaSerializer.fromJson(element)
            "RichTextBlock" -> RichTextBlockSerializer.fromJson(element)
            "Table" -> TableSerializer.fromJson(element)
            "TextBlock" -> TextBlockSerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a CardElement; known types are ${byType.keys}",
            )
        }
    }
}

/** `ImplementationsOf.ToggleableItem`, dispatched on the `type` discriminator. */
public object ToggleableItemSerializer : CardSerializer<ToggleableItem>("ToggleableItem") {

    override fun toJson(value: ToggleableItem): JsonElement = when (value) {
        is ActionSet -> ActionSetSerializer.toJson(value)
        is Column -> ColumnSerializer.toJson(value)
        is ColumnSet -> ColumnSetSerializer.toJson(value)
        is Container -> ContainerSerializer.toJson(value)
        is FactSet -> FactSetSerializer.toJson(value)
        is Image -> ImageSerializer.toJson(value)
        is ImageSet -> ImageSetSerializer.toJson(value)
        is InputChoiceSet -> InputChoiceSetSerializer.toJson(value)
        is InputDate -> InputDateSerializer.toJson(value)
        is InputNumber -> InputNumberSerializer.toJson(value)
        is InputText -> InputTextSerializer.toJson(value)
        is InputTime -> InputTimeSerializer.toJson(value)
        is InputToggle -> InputToggleSerializer.toJson(value)
        is Media -> MediaSerializer.toJson(value)
        is RichTextBlock -> RichTextBlockSerializer.toJson(value)
        is Table -> TableSerializer.toJson(value)
        is TextBlock -> TextBlockSerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "ActionSet" to Unit,
        "Column" to Unit,
        "ColumnSet" to Unit,
        "Container" to Unit,
        "FactSet" to Unit,
        "Image" to Unit,
        "ImageSet" to Unit,
        "Input.ChoiceSet" to Unit,
        "Input.Date" to Unit,
        "Input.Number" to Unit,
        "Input.Text" to Unit,
        "Input.Time" to Unit,
        "Input.Toggle" to Unit,
        "Media" to Unit,
        "RichTextBlock" to Unit,
        "Table" to Unit,
        "TextBlock" to Unit,
    )

    override fun fromJson(element: JsonElement): ToggleableItem? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "ActionSet" -> ActionSetSerializer.fromJson(element)
            "Column" -> ColumnSerializer.fromJson(element)
            "ColumnSet" -> ColumnSetSerializer.fromJson(element)
            "Container" -> ContainerSerializer.fromJson(element)
            "FactSet" -> FactSetSerializer.fromJson(element)
            "Image" -> ImageSerializer.fromJson(element)
            "ImageSet" -> ImageSetSerializer.fromJson(element)
            "Input.ChoiceSet" -> InputChoiceSetSerializer.fromJson(element)
            "Input.Date" -> InputDateSerializer.fromJson(element)
            "Input.Number" -> InputNumberSerializer.fromJson(element)
            "Input.Text" -> InputTextSerializer.fromJson(element)
            "Input.Time" -> InputTimeSerializer.fromJson(element)
            "Input.Toggle" -> InputToggleSerializer.fromJson(element)
            "Media" -> MediaSerializer.fromJson(element)
            "RichTextBlock" -> RichTextBlockSerializer.fromJson(element)
            "Table" -> TableSerializer.fromJson(element)
            "TextBlock" -> TextBlockSerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a ToggleableItem; known types are ${byType.keys}",
            )
        }
    }
}

/** `ImplementationsOf.Inline`, dispatched on the `type` discriminator. */
public object InlineSerializer : CardSerializer<Inline>("Inline") {

    override fun toJson(value: Inline): JsonElement = when (value) {
        is TextRun -> TextRunSerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "TextRun" to Unit,
    )

    override fun fromJson(element: JsonElement): Inline? {
        element.asString()?.let { return TextRunSerializer.fromJson(element) }
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "TextRun" -> TextRunSerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a Inline; known types are ${byType.keys}",
            )
        }
    }
}

/** `ImplementationsOf.Input`, dispatched on the `type` discriminator. */
public object InputSerializer : CardSerializer<Input>("Input") {

    override fun toJson(value: Input): JsonElement = when (value) {
        is InputChoiceSet -> InputChoiceSetSerializer.toJson(value)
        is InputDate -> InputDateSerializer.toJson(value)
        is InputNumber -> InputNumberSerializer.toJson(value)
        is InputText -> InputTextSerializer.toJson(value)
        is InputTime -> InputTimeSerializer.toJson(value)
        is InputToggle -> InputToggleSerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "Input.ChoiceSet" to Unit,
        "Input.Date" to Unit,
        "Input.Number" to Unit,
        "Input.Text" to Unit,
        "Input.Time" to Unit,
        "Input.Toggle" to Unit,
    )

    override fun fromJson(element: JsonElement): Input? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "Input.ChoiceSet" -> InputChoiceSetSerializer.fromJson(element)
            "Input.Date" -> InputDateSerializer.fromJson(element)
            "Input.Number" -> InputNumberSerializer.fromJson(element)
            "Input.Text" -> InputTextSerializer.fromJson(element)
            "Input.Time" -> InputTimeSerializer.fromJson(element)
            "Input.Toggle" -> InputToggleSerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a Input; known types are ${byType.keys}",
            )
        }
    }
}

/** `WebhookAction`, dispatched on the `type` discriminator. */
public object WebhookActionSerializer : CardSerializer<WebhookAction>("WebhookAction") {

    override fun toJson(value: WebhookAction): JsonElement = when (value) {
        is ActionOpenUrl -> ActionOpenUrlSerializer.toJson(value)
        is ActionShowCard -> ActionShowCardSerializer.toJson(value)
        is ActionToggleVisibility -> ActionToggleVisibilitySerializer.toJson(value)
        is ActionExecute -> ActionExecuteSerializer.toJson(value)
    }

    private val byType: Map<String, Unit> = mapOf(
        "Action.OpenUrl" to Unit,
        "Action.ShowCard" to Unit,
        "Action.ToggleVisibility" to Unit,
        "Action.Execute" to Unit,
    )

    override fun fromJson(element: JsonElement): WebhookAction? {
        return when (val type = element.asObject()?.at("type")?.asString()) {
            "Action.OpenUrl" -> ActionOpenUrlSerializer.fromJson(element)
            "Action.ShowCard" -> ActionShowCardSerializer.fromJson(element)
            "Action.ToggleVisibility" -> ActionToggleVisibilitySerializer.fromJson(element)
            "Action.Execute" -> ActionExecuteSerializer.fromJson(element)
            null -> null
            else -> throw SerializationException(
                "$type is not a WebhookAction; known types are ${byType.keys}",
            )
        }
    }
}

