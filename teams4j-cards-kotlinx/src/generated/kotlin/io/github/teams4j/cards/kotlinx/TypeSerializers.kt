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

/** `Action.Execute`. */
public object ActionExecuteSerializer : CardSerializer<ActionExecute>("ActionExecute") {

    override fun toJson(value: ActionExecute): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.iconUrl()?.let { put("iconUrl", JsonPrimitive(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.style()?.let { put("style", ActionStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ActionFallbackSerializer.toJson(it)) }
        value.tooltip()?.let { put("tooltip", JsonPrimitive(it)) }
        value.isEnabled()?.let { put("isEnabled", JsonPrimitive(it)) }
        value.mode()?.let { put("mode", ActionModeSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.verb()?.let { put("verb", JsonPrimitive(it)) }
        value.data()?.let { put("data", CardValueSerializer.toJson(it)) }
        value.associatedInputs()?.let { put("associatedInputs", AssociatedInputsSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): ActionExecute? {
        val obj = element.asObject() ?: return null
        return ActionExecute(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("title")?.let { it.asString() },
            obj.at("iconUrl")?.let { it.asString() },
            obj.at("id")?.let { it.asString() },
            obj.at("style")?.let { ActionStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ActionFallbackSerializer.fromJson(it) },
            obj.at("tooltip")?.let { it.asString() },
            obj.at("isEnabled")?.let { it.asBoolean() },
            obj.at("mode")?.let { ActionModeSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("verb")?.let { it.asString() },
            obj.at("data")?.let { CardValueSerializer.fromJson(it) },
            obj.at("associatedInputs")?.let { AssociatedInputsSerializer.fromJson(it) },
        )
    }
}

/** `Action.OpenUrl`. */
public object ActionOpenUrlSerializer : CardSerializer<ActionOpenUrl>("ActionOpenUrl") {

    override fun toJson(value: ActionOpenUrl): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.iconUrl()?.let { put("iconUrl", JsonPrimitive(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.style()?.let { put("style", ActionStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ActionFallbackSerializer.toJson(it)) }
        value.tooltip()?.let { put("tooltip", JsonPrimitive(it)) }
        value.isEnabled()?.let { put("isEnabled", JsonPrimitive(it)) }
        value.mode()?.let { put("mode", ActionModeSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.url()?.let { put("url", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): ActionOpenUrl? {
        val obj = element.asObject() ?: return null
        return ActionOpenUrl(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("title")?.let { it.asString() },
            obj.at("iconUrl")?.let { it.asString() },
            obj.at("id")?.let { it.asString() },
            obj.at("style")?.let { ActionStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ActionFallbackSerializer.fromJson(it) },
            obj.at("tooltip")?.let { it.asString() },
            obj.at("isEnabled")?.let { it.asBoolean() },
            obj.at("mode")?.let { ActionModeSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("url")?.let { it.asString() },
        )
    }
}

/** `Action.ShowCard`. */
public object ActionShowCardSerializer : CardSerializer<ActionShowCard>("ActionShowCard") {

    override fun toJson(value: ActionShowCard): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.iconUrl()?.let { put("iconUrl", JsonPrimitive(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.style()?.let { put("style", ActionStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ActionFallbackSerializer.toJson(it)) }
        value.tooltip()?.let { put("tooltip", JsonPrimitive(it)) }
        value.isEnabled()?.let { put("isEnabled", JsonPrimitive(it)) }
        value.mode()?.let { put("mode", ActionModeSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.card()?.let { put("card", AdaptiveCardSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): ActionShowCard? {
        val obj = element.asObject() ?: return null
        return ActionShowCard(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("title")?.let { it.asString() },
            obj.at("iconUrl")?.let { it.asString() },
            obj.at("id")?.let { it.asString() },
            obj.at("style")?.let { ActionStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ActionFallbackSerializer.fromJson(it) },
            obj.at("tooltip")?.let { it.asString() },
            obj.at("isEnabled")?.let { it.asBoolean() },
            obj.at("mode")?.let { ActionModeSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("card")?.let { AdaptiveCardSerializer.fromJson(it) },
        )
    }
}

/** `Action.Submit`. */
public object ActionSubmitSerializer : CardSerializer<ActionSubmit>("ActionSubmit") {

    override fun toJson(value: ActionSubmit): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.iconUrl()?.let { put("iconUrl", JsonPrimitive(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.style()?.let { put("style", ActionStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ActionFallbackSerializer.toJson(it)) }
        value.tooltip()?.let { put("tooltip", JsonPrimitive(it)) }
        value.isEnabled()?.let { put("isEnabled", JsonPrimitive(it)) }
        value.mode()?.let { put("mode", ActionModeSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.data()?.let { put("data", CardValueSerializer.toJson(it)) }
        value.associatedInputs()?.let { put("associatedInputs", AssociatedInputsSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): ActionSubmit? {
        val obj = element.asObject() ?: return null
        return ActionSubmit(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("title")?.let { it.asString() },
            obj.at("iconUrl")?.let { it.asString() },
            obj.at("id")?.let { it.asString() },
            obj.at("style")?.let { ActionStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ActionFallbackSerializer.fromJson(it) },
            obj.at("tooltip")?.let { it.asString() },
            obj.at("isEnabled")?.let { it.asBoolean() },
            obj.at("mode")?.let { ActionModeSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("data")?.let { CardValueSerializer.fromJson(it) },
            obj.at("associatedInputs")?.let { AssociatedInputsSerializer.fromJson(it) },
        )
    }
}

/** `Action.ToggleVisibility`. */
public object ActionToggleVisibilitySerializer : CardSerializer<ActionToggleVisibility>("ActionToggleVisibility") {

    override fun toJson(value: ActionToggleVisibility): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.iconUrl()?.let { put("iconUrl", JsonPrimitive(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.style()?.let { put("style", ActionStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ActionFallbackSerializer.toJson(it)) }
        value.tooltip()?.let { put("tooltip", JsonPrimitive(it)) }
        value.isEnabled()?.let { put("isEnabled", JsonPrimitive(it)) }
        value.mode()?.let { put("mode", ActionModeSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.targetElements()?.let { put("targetElements", buildJsonArray { it.forEach { e -> add(TargetElementSerializer.toJson(e)) } }) }
    }

    override fun fromJson(element: JsonElement): ActionToggleVisibility? {
        val obj = element.asObject() ?: return null
        return ActionToggleVisibility(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("title")?.let { it.asString() },
            obj.at("iconUrl")?.let { it.asString() },
            obj.at("id")?.let { it.asString() },
            obj.at("style")?.let { ActionStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ActionFallbackSerializer.fromJson(it) },
            obj.at("tooltip")?.let { it.asString() },
            obj.at("isEnabled")?.let { it.asBoolean() },
            obj.at("mode")?.let { ActionModeSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("targetElements")?.let { it.asArray()?.mapNotNull { e -> TargetElementSerializer.fromJson(e) } },
        )
    }
}

/** `TargetElement`. */
public object TargetElementSerializer : CardSerializer<TargetElement>("TargetElement") {

    override fun toJson(value: TargetElement): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.elementId()?.let { put("elementId", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): TargetElement? {
        // The bare-string form: "x" means { "elementId": "x" }.
        element.asString()?.let { return TargetElement.fromShorthand(it) }
        val obj = element.asObject() ?: return null
        return TargetElement(
            obj.at("type")?.let { it.asString() },
            obj.at("elementId")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
        )
    }
}

/** `AdaptiveCard`. */
public object AdaptiveCardSerializer : CardSerializer<AdaptiveCard>("AdaptiveCard") {

    override fun toJson(value: AdaptiveCard): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.version()?.let { put("version", JsonPrimitive(it)) }
        value.refresh()?.let { put("refresh", RefreshSerializer.toJson(it)) }
        value.authentication()?.let { put("authentication", AuthenticationSerializer.toJson(it)) }
        value.body()?.let { put("body", buildJsonArray { it.forEach { e -> add(CardElementSerializer.toJson(e)) } }) }
        value.actions()?.let { put("actions", buildJsonArray { it.forEach { e -> add(CardActionSerializer.toJson(e)) } }) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.fallbackText()?.let { put("fallbackText", JsonPrimitive(it)) }
        value.backgroundImage()?.let { put("backgroundImage", BackgroundImageSerializer.toJson(it)) }
        value.metadata()?.let { put("metadata", MetadataSerializer.toJson(it)) }
        value.minHeight()?.let { put("minHeight", JsonPrimitive(it)) }
        value.rtl()?.let { put("rtl", JsonPrimitive(it)) }
        value.speak()?.let { put("speak", JsonPrimitive(it)) }
        value.lang()?.let { put("lang", JsonPrimitive(it)) }
        value.verticalContentAlignment()?.let { put("verticalContentAlignment", VerticalContentAlignmentSerializer.toJson(it)) }
        value.`$schema`()?.let { put("\$schema", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): AdaptiveCard? {
        val obj = element.asObject() ?: return null
        return AdaptiveCard(
            obj.at("type")?.let { it.asString() },
            obj.at("version")?.let { it.asString() },
            obj.at("refresh")?.let { RefreshSerializer.fromJson(it) },
            obj.at("authentication")?.let { AuthenticationSerializer.fromJson(it) },
            obj.at("body")?.let { it.asArray()?.mapNotNull { e -> CardElementSerializer.fromJson(e) } },
            obj.at("actions")?.let { it.asArray()?.mapNotNull { e -> CardActionSerializer.fromJson(e) } },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("fallbackText")?.let { it.asString() },
            obj.at("backgroundImage")?.let { BackgroundImageSerializer.fromJson(it) },
            obj.at("metadata")?.let { MetadataSerializer.fromJson(it) },
            obj.at("minHeight")?.let { it.asString() },
            obj.at("rtl")?.let { it.asBoolean() },
            obj.at("speak")?.let { it.asString() },
            obj.at("lang")?.let { it.asString() },
            obj.at("verticalContentAlignment")?.let { VerticalContentAlignmentSerializer.fromJson(it) },
            obj.at("\$schema")?.let { it.asString() },
        )
    }
}

/** `ActionSet`. */
public object ActionSetSerializer : CardSerializer<ActionSet>("ActionSet") {

    override fun toJson(value: ActionSet): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.actions()?.let { put("actions", buildJsonArray { it.forEach { e -> add(CardActionSerializer.toJson(e)) } }) }
    }

    override fun fromJson(element: JsonElement): ActionSet? {
        val obj = element.asObject() ?: return null
        return ActionSet(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("actions")?.let { it.asArray()?.mapNotNull { e -> CardActionSerializer.fromJson(e) } },
        )
    }
}

/** `CaptionSource`. */
public object CaptionSourceSerializer : CardSerializer<CaptionSource>("CaptionSource") {

    override fun toJson(value: CaptionSource): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.mimeType()?.let { put("mimeType", JsonPrimitive(it)) }
        value.url()?.let { put("url", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): CaptionSource? {
        val obj = element.asObject() ?: return null
        return CaptionSource(
            obj.at("type")?.let { it.asString() },
            obj.at("mimeType")?.let { it.asString() },
            obj.at("url")?.let { it.asString() },
            obj.at("label")?.let { it.asString() },
        )
    }
}

/** `Column`. */
public object ColumnSerializer : CardSerializer<Column>("Column") {

    override fun toJson(value: Column): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.items()?.let { put("items", buildJsonArray { it.forEach { e -> add(CardElementSerializer.toJson(e)) } }) }
        value.backgroundImage()?.let { put("backgroundImage", BackgroundImageSerializer.toJson(it)) }
        value.bleed()?.let { put("bleed", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ColumnFallbackSerializer.toJson(it)) }
        value.minHeight()?.let { put("minHeight", JsonPrimitive(it)) }
        value.rtl()?.let { put("rtl", JsonPrimitive(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.style()?.let { put("style", ContainerStyleSerializer.toJson(it)) }
        value.verticalContentAlignment()?.let { put("verticalContentAlignment", VerticalContentAlignmentSerializer.toJson(it)) }
        value.width()?.let { put("width", DimensionSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): Column? {
        val obj = element.asObject() ?: return null
        return Column(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("type")?.let { it.asString() },
            obj.at("items")?.let { it.asArray()?.mapNotNull { e -> CardElementSerializer.fromJson(e) } },
            obj.at("backgroundImage")?.let { BackgroundImageSerializer.fromJson(it) },
            obj.at("bleed")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ColumnFallbackSerializer.fromJson(it) },
            obj.at("minHeight")?.let { it.asString() },
            obj.at("rtl")?.let { it.asBoolean() },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("style")?.let { ContainerStyleSerializer.fromJson(it) },
            obj.at("verticalContentAlignment")?.let { VerticalContentAlignmentSerializer.fromJson(it) },
            obj.at("width")?.let { DimensionSerializer.fromJson(it) },
        )
    }
}

/** `ColumnSet`. */
public object ColumnSetSerializer : CardSerializer<ColumnSet>("ColumnSet") {

    override fun toJson(value: ColumnSet): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.columns()?.let { put("columns", buildJsonArray { it.forEach { e -> add(ColumnSerializer.toJson(e)) } }) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.style()?.let { put("style", ContainerStyleSerializer.toJson(it)) }
        value.bleed()?.let { put("bleed", JsonPrimitive(it)) }
        value.minHeight()?.let { put("minHeight", JsonPrimitive(it)) }
        value.horizontalAlignment()?.let { put("horizontalAlignment", HorizontalAlignmentSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): ColumnSet? {
        val obj = element.asObject() ?: return null
        return ColumnSet(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("columns")?.let { it.asArray()?.mapNotNull { e -> ColumnSerializer.fromJson(e) } },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("style")?.let { ContainerStyleSerializer.fromJson(it) },
            obj.at("bleed")?.let { it.asBoolean() },
            obj.at("minHeight")?.let { it.asString() },
            obj.at("horizontalAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
        )
    }
}

/** `Container`. */
public object ContainerSerializer : CardSerializer<Container>("Container") {

    override fun toJson(value: Container): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.items()?.let { put("items", buildJsonArray { it.forEach { e -> add(CardElementSerializer.toJson(e)) } }) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.style()?.let { put("style", ContainerStyleSerializer.toJson(it)) }
        value.verticalContentAlignment()?.let { put("verticalContentAlignment", VerticalContentAlignmentSerializer.toJson(it)) }
        value.bleed()?.let { put("bleed", JsonPrimitive(it)) }
        value.backgroundImage()?.let { put("backgroundImage", BackgroundImageSerializer.toJson(it)) }
        value.minHeight()?.let { put("minHeight", JsonPrimitive(it)) }
        value.rtl()?.let { put("rtl", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): Container? {
        val obj = element.asObject() ?: return null
        return Container(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("items")?.let { it.asArray()?.mapNotNull { e -> CardElementSerializer.fromJson(e) } },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("style")?.let { ContainerStyleSerializer.fromJson(it) },
            obj.at("verticalContentAlignment")?.let { VerticalContentAlignmentSerializer.fromJson(it) },
            obj.at("bleed")?.let { it.asBoolean() },
            obj.at("backgroundImage")?.let { BackgroundImageSerializer.fromJson(it) },
            obj.at("minHeight")?.let { it.asString() },
            obj.at("rtl")?.let { it.asBoolean() },
        )
    }
}

/** `Fact`. */
public object FactSerializer : CardSerializer<Fact>("Fact") {

    override fun toJson(value: Fact): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): Fact? {
        val obj = element.asObject() ?: return null
        return Fact(
            obj.at("type")?.let { it.asString() },
            obj.at("title")?.let { it.asString() },
            obj.at("value")?.let { it.asString() },
        )
    }
}

/** `FactSet`. */
public object FactSetSerializer : CardSerializer<FactSet>("FactSet") {

    override fun toJson(value: FactSet): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.facts()?.let { put("facts", buildJsonArray { it.forEach { e -> add(FactSerializer.toJson(e)) } }) }
    }

    override fun fromJson(element: JsonElement): FactSet? {
        val obj = element.asObject() ?: return null
        return FactSet(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("facts")?.let { it.asArray()?.mapNotNull { e -> FactSerializer.fromJson(e) } },
        )
    }
}

/** `Image`. */
public object ImageSerializer : CardSerializer<Image>("Image") {

    override fun toJson(value: Image): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.url()?.let { put("url", JsonPrimitive(it)) }
        value.altText()?.let { put("altText", JsonPrimitive(it)) }
        value.backgroundColor()?.let { put("backgroundColor", JsonPrimitive(it)) }
        value.height()?.let { put("height", JsonPrimitive(it)) }
        value.horizontalAlignment()?.let { put("horizontalAlignment", HorizontalAlignmentSerializer.toJson(it)) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.size()?.let { put("size", ImageSizeSerializer.toJson(it)) }
        value.style()?.let { put("style", ImageStyleSerializer.toJson(it)) }
        value.width()?.let { put("width", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
    }

    override fun fromJson(element: JsonElement): Image? {
        val obj = element.asObject() ?: return null
        return Image(
            obj.at("type")?.let { it.asString() },
            obj.at("url")?.let { it.asString() },
            obj.at("altText")?.let { it.asString() },
            obj.at("backgroundColor")?.let { it.asString() },
            obj.at("height")?.let { it.asString() },
            obj.at("horizontalAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("size")?.let { ImageSizeSerializer.fromJson(it) },
            obj.at("style")?.let { ImageStyleSerializer.fromJson(it) },
            obj.at("width")?.let { it.asString() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
        )
    }
}

/** `ImageSet`. */
public object ImageSetSerializer : CardSerializer<ImageSet>("ImageSet") {

    override fun toJson(value: ImageSet): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.images()?.let { put("images", buildJsonArray { it.forEach { e -> add(ImageSerializer.toJson(e)) } }) }
        value.imageSize()?.let { put("imageSize", ImageSizeSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): ImageSet? {
        val obj = element.asObject() ?: return null
        return ImageSet(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("images")?.let { it.asArray()?.mapNotNull { e -> ImageSerializer.fromJson(e) } },
            obj.at("imageSize")?.let { ImageSizeSerializer.fromJson(it) },
        )
    }
}

/** `TextRun`. */
public object TextRunSerializer : CardSerializer<TextRun>("TextRun") {

    override fun toJson(value: TextRun): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.text()?.let { put("text", JsonPrimitive(it)) }
        value.color()?.let { put("color", ColorsSerializer.toJson(it)) }
        value.fontType()?.let { put("fontType", FontTypeSerializer.toJson(it)) }
        value.highlight()?.let { put("highlight", JsonPrimitive(it)) }
        value.isSubtle()?.let { put("isSubtle", JsonPrimitive(it)) }
        value.italic()?.let { put("italic", JsonPrimitive(it)) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.size()?.let { put("size", FontSizeSerializer.toJson(it)) }
        value.strikethrough()?.let { put("strikethrough", JsonPrimitive(it)) }
        value.underline()?.let { put("underline", JsonPrimitive(it)) }
        value.weight()?.let { put("weight", FontWeightSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): TextRun? {
        // The bare-string form: "x" means { "text": "x" }.
        element.asString()?.let { return TextRun.fromShorthand(it) }
        val obj = element.asObject() ?: return null
        return TextRun(
            obj.at("type")?.let { it.asString() },
            obj.at("text")?.let { it.asString() },
            obj.at("color")?.let { ColorsSerializer.fromJson(it) },
            obj.at("fontType")?.let { FontTypeSerializer.fromJson(it) },
            obj.at("highlight")?.let { it.asBoolean() },
            obj.at("isSubtle")?.let { it.asBoolean() },
            obj.at("italic")?.let { it.asBoolean() },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("size")?.let { FontSizeSerializer.fromJson(it) },
            obj.at("strikethrough")?.let { it.asBoolean() },
            obj.at("underline")?.let { it.asBoolean() },
            obj.at("weight")?.let { FontWeightSerializer.fromJson(it) },
        )
    }
}

/** `Data.Query`. */
public object DataQuerySerializer : CardSerializer<DataQuery>("DataQuery") {

    override fun toJson(value: DataQuery): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.dataset()?.let { put("dataset", JsonPrimitive(it)) }
        value.count()?.let { put("count", JsonPrimitive(it)) }
        value.skip()?.let { put("skip", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): DataQuery? {
        val obj = element.asObject() ?: return null
        return DataQuery(
            obj.at("type")?.let { it.asString() },
            obj.at("dataset")?.let { it.asString() },
            obj.at("count")?.let { it.asNumber() },
            obj.at("skip")?.let { it.asNumber() },
        )
    }
}

/** `Input.Choice`. */
public object InputChoiceSerializer : CardSerializer<InputChoice>("InputChoice") {

    override fun toJson(value: InputChoice): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputChoice? {
        val obj = element.asObject() ?: return null
        return InputChoice(
            obj.at("type")?.let { it.asString() },
            obj.at("title")?.let { it.asString() },
            obj.at("value")?.let { it.asString() },
        )
    }
}

/** `Input.ChoiceSet`. */
public object InputChoiceSetSerializer : CardSerializer<InputChoiceSet>("InputChoiceSet") {

    override fun toJson(value: InputChoiceSet): JsonElement = buildJsonObject {
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.errorMessage()?.let { put("errorMessage", JsonPrimitive(it)) }
        value.isRequired()?.let { put("isRequired", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
        value.labelPosition()?.let { put("labelPosition", InputLabelPositionSerializer.toJson(it)) }
        value.labelWidth()?.let { put("labelWidth", DimensionSerializer.toJson(it)) }
        value.inputStyle()?.let { put("inputStyle", InputStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.choices()?.let { put("choices", buildJsonArray { it.forEach { e -> add(InputChoiceSerializer.toJson(e)) } }) }
        value.choicesData()?.let { put("choices.data", DataQuerySerializer.toJson(it)) }
        value.isMultiSelect()?.let { put("isMultiSelect", JsonPrimitive(it)) }
        value.style()?.let { put("style", ChoiceInputStyleSerializer.toJson(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
        value.placeholder()?.let { put("placeholder", JsonPrimitive(it)) }
        value.wrap()?.let { put("wrap", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputChoiceSet? {
        val obj = element.asObject() ?: return null
        return InputChoiceSet(
            obj.at("id")?.let { it.asString() },
            obj.at("errorMessage")?.let { it.asString() },
            obj.at("isRequired")?.let { it.asBoolean() },
            obj.at("label")?.let { it.asString() },
            obj.at("labelPosition")?.let { InputLabelPositionSerializer.fromJson(it) },
            obj.at("labelWidth")?.let { DimensionSerializer.fromJson(it) },
            obj.at("inputStyle")?.let { InputStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("type")?.let { it.asString() },
            obj.at("choices")?.let { it.asArray()?.mapNotNull { e -> InputChoiceSerializer.fromJson(e) } },
            obj.at("choices.data")?.let { DataQuerySerializer.fromJson(it) },
            obj.at("isMultiSelect")?.let { it.asBoolean() },
            obj.at("style")?.let { ChoiceInputStyleSerializer.fromJson(it) },
            obj.at("value")?.let { it.asString() },
            obj.at("placeholder")?.let { it.asString() },
            obj.at("wrap")?.let { it.asBoolean() },
        )
    }
}

/** `Input.Date`. */
public object InputDateSerializer : CardSerializer<InputDate>("InputDate") {

    override fun toJson(value: InputDate): JsonElement = buildJsonObject {
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.errorMessage()?.let { put("errorMessage", JsonPrimitive(it)) }
        value.isRequired()?.let { put("isRequired", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
        value.labelPosition()?.let { put("labelPosition", InputLabelPositionSerializer.toJson(it)) }
        value.labelWidth()?.let { put("labelWidth", DimensionSerializer.toJson(it)) }
        value.inputStyle()?.let { put("inputStyle", InputStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.max()?.let { put("max", JsonPrimitive(it)) }
        value.min()?.let { put("min", JsonPrimitive(it)) }
        value.placeholder()?.let { put("placeholder", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputDate? {
        val obj = element.asObject() ?: return null
        return InputDate(
            obj.at("id")?.let { it.asString() },
            obj.at("errorMessage")?.let { it.asString() },
            obj.at("isRequired")?.let { it.asBoolean() },
            obj.at("label")?.let { it.asString() },
            obj.at("labelPosition")?.let { InputLabelPositionSerializer.fromJson(it) },
            obj.at("labelWidth")?.let { DimensionSerializer.fromJson(it) },
            obj.at("inputStyle")?.let { InputStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("type")?.let { it.asString() },
            obj.at("max")?.let { it.asString() },
            obj.at("min")?.let { it.asString() },
            obj.at("placeholder")?.let { it.asString() },
            obj.at("value")?.let { it.asString() },
        )
    }
}

/** `Input.Number`. */
public object InputNumberSerializer : CardSerializer<InputNumber>("InputNumber") {

    override fun toJson(value: InputNumber): JsonElement = buildJsonObject {
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.errorMessage()?.let { put("errorMessage", JsonPrimitive(it)) }
        value.isRequired()?.let { put("isRequired", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
        value.labelPosition()?.let { put("labelPosition", InputLabelPositionSerializer.toJson(it)) }
        value.labelWidth()?.let { put("labelWidth", DimensionSerializer.toJson(it)) }
        value.inputStyle()?.let { put("inputStyle", InputStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.max()?.let { put("max", JsonPrimitive(it)) }
        value.min()?.let { put("min", JsonPrimitive(it)) }
        value.placeholder()?.let { put("placeholder", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputNumber? {
        val obj = element.asObject() ?: return null
        return InputNumber(
            obj.at("id")?.let { it.asString() },
            obj.at("errorMessage")?.let { it.asString() },
            obj.at("isRequired")?.let { it.asBoolean() },
            obj.at("label")?.let { it.asString() },
            obj.at("labelPosition")?.let { InputLabelPositionSerializer.fromJson(it) },
            obj.at("labelWidth")?.let { DimensionSerializer.fromJson(it) },
            obj.at("inputStyle")?.let { InputStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("type")?.let { it.asString() },
            obj.at("max")?.let { it.asNumber() },
            obj.at("min")?.let { it.asNumber() },
            obj.at("placeholder")?.let { it.asString() },
            obj.at("value")?.let { it.asNumber() },
        )
    }
}

/** `Input.Text`. */
public object InputTextSerializer : CardSerializer<InputText>("InputText") {

    override fun toJson(value: InputText): JsonElement = buildJsonObject {
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.errorMessage()?.let { put("errorMessage", JsonPrimitive(it)) }
        value.isRequired()?.let { put("isRequired", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
        value.labelPosition()?.let { put("labelPosition", InputLabelPositionSerializer.toJson(it)) }
        value.labelWidth()?.let { put("labelWidth", DimensionSerializer.toJson(it)) }
        value.inputStyle()?.let { put("inputStyle", InputStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.isMultiline()?.let { put("isMultiline", JsonPrimitive(it)) }
        value.maxLength()?.let { put("maxLength", JsonPrimitive(it)) }
        value.placeholder()?.let { put("placeholder", JsonPrimitive(it)) }
        value.regex()?.let { put("regex", JsonPrimitive(it)) }
        value.style()?.let { put("style", TextInputStyleSerializer.toJson(it)) }
        value.inlineAction()?.let { put("inlineAction", SelectActionSerializer.toJson(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputText? {
        val obj = element.asObject() ?: return null
        return InputText(
            obj.at("id")?.let { it.asString() },
            obj.at("errorMessage")?.let { it.asString() },
            obj.at("isRequired")?.let { it.asBoolean() },
            obj.at("label")?.let { it.asString() },
            obj.at("labelPosition")?.let { InputLabelPositionSerializer.fromJson(it) },
            obj.at("labelWidth")?.let { DimensionSerializer.fromJson(it) },
            obj.at("inputStyle")?.let { InputStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("type")?.let { it.asString() },
            obj.at("isMultiline")?.let { it.asBoolean() },
            obj.at("maxLength")?.let { it.asNumber() },
            obj.at("placeholder")?.let { it.asString() },
            obj.at("regex")?.let { it.asString() },
            obj.at("style")?.let { TextInputStyleSerializer.fromJson(it) },
            obj.at("inlineAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("value")?.let { it.asString() },
        )
    }
}

/** `Input.Time`. */
public object InputTimeSerializer : CardSerializer<InputTime>("InputTime") {

    override fun toJson(value: InputTime): JsonElement = buildJsonObject {
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.errorMessage()?.let { put("errorMessage", JsonPrimitive(it)) }
        value.isRequired()?.let { put("isRequired", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
        value.labelPosition()?.let { put("labelPosition", InputLabelPositionSerializer.toJson(it)) }
        value.labelWidth()?.let { put("labelWidth", DimensionSerializer.toJson(it)) }
        value.inputStyle()?.let { put("inputStyle", InputStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.max()?.let { put("max", JsonPrimitive(it)) }
        value.min()?.let { put("min", JsonPrimitive(it)) }
        value.placeholder()?.let { put("placeholder", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputTime? {
        val obj = element.asObject() ?: return null
        return InputTime(
            obj.at("id")?.let { it.asString() },
            obj.at("errorMessage")?.let { it.asString() },
            obj.at("isRequired")?.let { it.asBoolean() },
            obj.at("label")?.let { it.asString() },
            obj.at("labelPosition")?.let { InputLabelPositionSerializer.fromJson(it) },
            obj.at("labelWidth")?.let { DimensionSerializer.fromJson(it) },
            obj.at("inputStyle")?.let { InputStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("type")?.let { it.asString() },
            obj.at("max")?.let { it.asString() },
            obj.at("min")?.let { it.asString() },
            obj.at("placeholder")?.let { it.asString() },
            obj.at("value")?.let { it.asString() },
        )
    }
}

/** `Input.Toggle`. */
public object InputToggleSerializer : CardSerializer<InputToggle>("InputToggle") {

    override fun toJson(value: InputToggle): JsonElement = buildJsonObject {
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.errorMessage()?.let { put("errorMessage", JsonPrimitive(it)) }
        value.isRequired()?.let { put("isRequired", JsonPrimitive(it)) }
        value.label()?.let { put("label", JsonPrimitive(it)) }
        value.labelPosition()?.let { put("labelPosition", InputLabelPositionSerializer.toJson(it)) }
        value.labelWidth()?.let { put("labelWidth", DimensionSerializer.toJson(it)) }
        value.inputStyle()?.let { put("inputStyle", InputStyleSerializer.toJson(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
        value.valueOff()?.let { put("valueOff", JsonPrimitive(it)) }
        value.valueOn()?.let { put("valueOn", JsonPrimitive(it)) }
        value.wrap()?.let { put("wrap", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): InputToggle? {
        val obj = element.asObject() ?: return null
        return InputToggle(
            obj.at("id")?.let { it.asString() },
            obj.at("errorMessage")?.let { it.asString() },
            obj.at("isRequired")?.let { it.asBoolean() },
            obj.at("label")?.let { it.asString() },
            obj.at("labelPosition")?.let { InputLabelPositionSerializer.fromJson(it) },
            obj.at("labelWidth")?.let { DimensionSerializer.fromJson(it) },
            obj.at("inputStyle")?.let { InputStyleSerializer.fromJson(it) },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("type")?.let { it.asString() },
            obj.at("title")?.let { it.asString() },
            obj.at("value")?.let { it.asString() },
            obj.at("valueOff")?.let { it.asString() },
            obj.at("valueOn")?.let { it.asString() },
            obj.at("wrap")?.let { it.asBoolean() },
        )
    }
}

/** `Media`. */
public object MediaSerializer : CardSerializer<Media>("Media") {

    override fun toJson(value: Media): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.sources()?.let { put("sources", buildJsonArray { it.forEach { e -> add(MediaSourceSerializer.toJson(e)) } }) }
        value.poster()?.let { put("poster", JsonPrimitive(it)) }
        value.altText()?.let { put("altText", JsonPrimitive(it)) }
        value.captionSources()?.let { put("captionSources", buildJsonArray { it.forEach { e -> add(CaptionSourceSerializer.toJson(e)) } }) }
    }

    override fun fromJson(element: JsonElement): Media? {
        val obj = element.asObject() ?: return null
        return Media(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("sources")?.let { it.asArray()?.mapNotNull { e -> MediaSourceSerializer.fromJson(e) } },
            obj.at("poster")?.let { it.asString() },
            obj.at("altText")?.let { it.asString() },
            obj.at("captionSources")?.let { it.asArray()?.mapNotNull { e -> CaptionSourceSerializer.fromJson(e) } },
        )
    }
}

/** `MediaSource`. */
public object MediaSourceSerializer : CardSerializer<MediaSource>("MediaSource") {

    override fun toJson(value: MediaSource): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.mimeType()?.let { put("mimeType", JsonPrimitive(it)) }
        value.url()?.let { put("url", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): MediaSource? {
        val obj = element.asObject() ?: return null
        return MediaSource(
            obj.at("type")?.let { it.asString() },
            obj.at("mimeType")?.let { it.asString() },
            obj.at("url")?.let { it.asString() },
        )
    }
}

/** `RichTextBlock`. */
public object RichTextBlockSerializer : CardSerializer<RichTextBlock>("RichTextBlock") {

    override fun toJson(value: RichTextBlock): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.inlines()?.let { put("inlines", buildJsonArray { it.forEach { e -> add(InlineSerializer.toJson(e)) } }) }
        value.horizontalAlignment()?.let { put("horizontalAlignment", HorizontalAlignmentSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): RichTextBlock? {
        val obj = element.asObject() ?: return null
        return RichTextBlock(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("inlines")?.let { it.asArray()?.mapNotNull { e -> InlineSerializer.fromJson(e) } },
            obj.at("horizontalAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
        )
    }
}

/** `Table`. */
public object TableSerializer : CardSerializer<Table>("Table") {

    override fun toJson(value: Table): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.columns()?.let { put("columns", buildJsonArray { it.forEach { e -> add(TableColumnDefinitionSerializer.toJson(e)) } }) }
        value.rows()?.let { put("rows", buildJsonArray { it.forEach { e -> add(TableRowSerializer.toJson(e)) } }) }
        value.firstRowAsHeader()?.let { put("firstRowAsHeader", JsonPrimitive(it)) }
        value.showGridLines()?.let { put("showGridLines", JsonPrimitive(it)) }
        value.gridStyle()?.let { put("gridStyle", ContainerStyleSerializer.toJson(it)) }
        value.horizontalCellContentAlignment()?.let { put("horizontalCellContentAlignment", HorizontalAlignmentSerializer.toJson(it)) }
        value.verticalCellContentAlignment()?.let { put("verticalCellContentAlignment", VerticalAlignmentSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): Table? {
        val obj = element.asObject() ?: return null
        return Table(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("columns")?.let { it.asArray()?.mapNotNull { e -> TableColumnDefinitionSerializer.fromJson(e) } },
            obj.at("rows")?.let { it.asArray()?.mapNotNull { e -> TableRowSerializer.fromJson(e) } },
            obj.at("firstRowAsHeader")?.let { it.asBoolean() },
            obj.at("showGridLines")?.let { it.asBoolean() },
            obj.at("gridStyle")?.let { ContainerStyleSerializer.fromJson(it) },
            obj.at("horizontalCellContentAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
            obj.at("verticalCellContentAlignment")?.let { VerticalAlignmentSerializer.fromJson(it) },
        )
    }
}

/** `TableCell`. */
public object TableCellSerializer : CardSerializer<TableCell>("TableCell") {

    override fun toJson(value: TableCell): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.items()?.let { put("items", buildJsonArray { it.forEach { e -> add(CardElementSerializer.toJson(e)) } }) }
        value.selectAction()?.let { put("selectAction", SelectActionSerializer.toJson(it)) }
        value.style()?.let { put("style", ContainerStyleSerializer.toJson(it)) }
        value.verticalContentAlignment()?.let { put("verticalContentAlignment", VerticalContentAlignmentSerializer.toJson(it)) }
        value.bleed()?.let { put("bleed", JsonPrimitive(it)) }
        value.backgroundImage()?.let { put("backgroundImage", BackgroundImageSerializer.toJson(it)) }
        value.minHeight()?.let { put("minHeight", JsonPrimitive(it)) }
        value.rtl()?.let { put("rtl", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): TableCell? {
        val obj = element.asObject() ?: return null
        return TableCell(
            obj.at("type")?.let { it.asString() },
            obj.at("items")?.let { it.asArray()?.mapNotNull { e -> CardElementSerializer.fromJson(e) } },
            obj.at("selectAction")?.let { SelectActionSerializer.fromJson(it) },
            obj.at("style")?.let { ContainerStyleSerializer.fromJson(it) },
            obj.at("verticalContentAlignment")?.let { VerticalContentAlignmentSerializer.fromJson(it) },
            obj.at("bleed")?.let { it.asBoolean() },
            obj.at("backgroundImage")?.let { BackgroundImageSerializer.fromJson(it) },
            obj.at("minHeight")?.let { it.asString() },
            obj.at("rtl")?.let { it.asBoolean() },
        )
    }
}

/** `TableColumnDefinition`. */
public object TableColumnDefinitionSerializer : CardSerializer<TableColumnDefinition>("TableColumnDefinition") {

    override fun toJson(value: TableColumnDefinition): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.width()?.let { put("width", DimensionSerializer.toJson(it)) }
        value.horizontalCellContentAlignment()?.let { put("horizontalCellContentAlignment", HorizontalAlignmentSerializer.toJson(it)) }
        value.verticalCellContentAlignment()?.let { put("verticalCellContentAlignment", VerticalAlignmentSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): TableColumnDefinition? {
        val obj = element.asObject() ?: return null
        return TableColumnDefinition(
            obj.at("type")?.let { it.asString() },
            obj.at("width")?.let { DimensionSerializer.fromJson(it) },
            obj.at("horizontalCellContentAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
            obj.at("verticalCellContentAlignment")?.let { VerticalAlignmentSerializer.fromJson(it) },
        )
    }
}

/** `TableRow`. */
public object TableRowSerializer : CardSerializer<TableRow>("TableRow") {

    override fun toJson(value: TableRow): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.cells()?.let { put("cells", buildJsonArray { it.forEach { e -> add(TableCellSerializer.toJson(e)) } }) }
        value.style()?.let { put("style", ContainerStyleSerializer.toJson(it)) }
        value.horizontalCellContentAlignment()?.let { put("horizontalCellContentAlignment", HorizontalAlignmentSerializer.toJson(it)) }
        value.verticalCellContentAlignment()?.let { put("verticalCellContentAlignment", VerticalAlignmentSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): TableRow? {
        val obj = element.asObject() ?: return null
        return TableRow(
            obj.at("type")?.let { it.asString() },
            obj.at("cells")?.let { it.asArray()?.mapNotNull { e -> TableCellSerializer.fromJson(e) } },
            obj.at("style")?.let { ContainerStyleSerializer.fromJson(it) },
            obj.at("horizontalCellContentAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
            obj.at("verticalCellContentAlignment")?.let { VerticalAlignmentSerializer.fromJson(it) },
        )
    }
}

/** `TextBlock`. */
public object TextBlockSerializer : CardSerializer<TextBlock>("TextBlock") {

    override fun toJson(value: TextBlock): JsonElement = buildJsonObject {
        value.requires()?.let { put("requires", buildJsonObject { it.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.isVisible()?.let { put("isVisible", JsonPrimitive(it)) }
        value.fallback()?.let { put("fallback", ElementFallbackSerializer.toJson(it)) }
        value.height()?.let { put("height", BlockElementHeightSerializer.toJson(it)) }
        value.separator()?.let { put("separator", JsonPrimitive(it)) }
        value.spacing()?.let { put("spacing", SpacingSerializer.toJson(it)) }
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.text()?.let { put("text", JsonPrimitive(it)) }
        value.color()?.let { put("color", ColorsSerializer.toJson(it)) }
        value.fontType()?.let { put("fontType", FontTypeSerializer.toJson(it)) }
        value.horizontalAlignment()?.let { put("horizontalAlignment", HorizontalAlignmentSerializer.toJson(it)) }
        value.isSubtle()?.let { put("isSubtle", JsonPrimitive(it)) }
        value.maxLines()?.let { put("maxLines", JsonPrimitive(it)) }
        value.size()?.let { put("size", FontSizeSerializer.toJson(it)) }
        value.weight()?.let { put("weight", FontWeightSerializer.toJson(it)) }
        value.wrap()?.let { put("wrap", JsonPrimitive(it)) }
        value.style()?.let { put("style", TextBlockStyleSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): TextBlock? {
        val obj = element.asObject() ?: return null
        return TextBlock(
            obj.at("requires")?.let { it.asObject()?.entries?.mapNotNull { (k, v) -> v.asString()?.let { k to it } }?.toMap() },
            obj.at("id")?.let { it.asString() },
            obj.at("isVisible")?.let { it.asBoolean() },
            obj.at("fallback")?.let { ElementFallbackSerializer.fromJson(it) },
            obj.at("height")?.let { BlockElementHeightSerializer.fromJson(it) },
            obj.at("separator")?.let { it.asBoolean() },
            obj.at("spacing")?.let { SpacingSerializer.fromJson(it) },
            obj.at("type")?.let { it.asString() },
            obj.at("text")?.let { it.asString() },
            obj.at("color")?.let { ColorsSerializer.fromJson(it) },
            obj.at("fontType")?.let { FontTypeSerializer.fromJson(it) },
            obj.at("horizontalAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
            obj.at("isSubtle")?.let { it.asBoolean() },
            obj.at("maxLines")?.let { it.asNumber() },
            obj.at("size")?.let { FontSizeSerializer.fromJson(it) },
            obj.at("weight")?.let { FontWeightSerializer.fromJson(it) },
            obj.at("wrap")?.let { it.asBoolean() },
            obj.at("style")?.let { TextBlockStyleSerializer.fromJson(it) },
        )
    }
}

/** `AuthCardButton`. */
public object AuthCardButtonSerializer : CardSerializer<AuthCardButton>("AuthCardButton") {

    override fun toJson(value: AuthCardButton): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.title()?.let { put("title", JsonPrimitive(it)) }
        value.image()?.let { put("image", JsonPrimitive(it)) }
        value.value()?.let { put("value", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): AuthCardButton? {
        val obj = element.asObject() ?: return null
        return AuthCardButton(
            obj.at("type")?.let { it.asString() },
            obj.at("title")?.let { it.asString() },
            obj.at("image")?.let { it.asString() },
            obj.at("value")?.let { it.asString() },
        )
    }
}

/** `Authentication`. */
public object AuthenticationSerializer : CardSerializer<Authentication>("Authentication") {

    override fun toJson(value: Authentication): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.text()?.let { put("text", JsonPrimitive(it)) }
        value.connectionName()?.let { put("connectionName", JsonPrimitive(it)) }
        value.tokenExchangeResource()?.let { put("tokenExchangeResource", TokenExchangeResourceSerializer.toJson(it)) }
        value.buttons()?.let { put("buttons", buildJsonArray { it.forEach { e -> add(AuthCardButtonSerializer.toJson(e)) } }) }
    }

    override fun fromJson(element: JsonElement): Authentication? {
        val obj = element.asObject() ?: return null
        return Authentication(
            obj.at("type")?.let { it.asString() },
            obj.at("text")?.let { it.asString() },
            obj.at("connectionName")?.let { it.asString() },
            obj.at("tokenExchangeResource")?.let { TokenExchangeResourceSerializer.fromJson(it) },
            obj.at("buttons")?.let { it.asArray()?.mapNotNull { e -> AuthCardButtonSerializer.fromJson(e) } },
        )
    }
}

/** `BackgroundImage`. */
public object BackgroundImageSerializer : CardSerializer<BackgroundImage>("BackgroundImage") {

    override fun toJson(value: BackgroundImage): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.url()?.let { put("url", JsonPrimitive(it)) }
        value.fillMode()?.let { put("fillMode", ImageFillModeSerializer.toJson(it)) }
        value.horizontalAlignment()?.let { put("horizontalAlignment", HorizontalAlignmentSerializer.toJson(it)) }
        value.verticalAlignment()?.let { put("verticalAlignment", VerticalAlignmentSerializer.toJson(it)) }
    }

    override fun fromJson(element: JsonElement): BackgroundImage? {
        // The bare-string form: "x" means { "url": "x" }.
        element.asString()?.let { return BackgroundImage.fromShorthand(it) }
        val obj = element.asObject() ?: return null
        return BackgroundImage(
            obj.at("type")?.let { it.asString() },
            obj.at("url")?.let { it.asString() },
            obj.at("fillMode")?.let { ImageFillModeSerializer.fromJson(it) },
            obj.at("horizontalAlignment")?.let { HorizontalAlignmentSerializer.fromJson(it) },
            obj.at("verticalAlignment")?.let { VerticalAlignmentSerializer.fromJson(it) },
        )
    }
}

/** `Metadata`. */
public object MetadataSerializer : CardSerializer<Metadata>("Metadata") {

    override fun toJson(value: Metadata): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.webUrl()?.let { put("webUrl", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): Metadata? {
        val obj = element.asObject() ?: return null
        return Metadata(
            obj.at("type")?.let { it.asString() },
            obj.at("webUrl")?.let { it.asString() },
        )
    }
}

/** `Refresh`. */
public object RefreshSerializer : CardSerializer<Refresh>("Refresh") {

    override fun toJson(value: Refresh): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.action()?.let { put("action", ActionExecuteSerializer.toJson(it)) }
        value.expires()?.let { put("expires", JsonPrimitive(it)) }
        value.userIds()?.let { put("userIds", buildJsonArray { it.forEach { e -> add(JsonPrimitive(e)) } }) }
    }

    override fun fromJson(element: JsonElement): Refresh? {
        val obj = element.asObject() ?: return null
        return Refresh(
            obj.at("type")?.let { it.asString() },
            obj.at("action")?.let { ActionExecuteSerializer.fromJson(it) },
            obj.at("expires")?.let { it.asString() },
            obj.at("userIds")?.let { it.asArray()?.mapNotNull { e -> e.asString() } },
        )
    }
}

/** `TokenExchangeResource`. */
public object TokenExchangeResourceSerializer : CardSerializer<TokenExchangeResource>("TokenExchangeResource") {

    override fun toJson(value: TokenExchangeResource): JsonElement = buildJsonObject {
        value.type()?.let { put("type", JsonPrimitive(it)) }
        value.id()?.let { put("id", JsonPrimitive(it)) }
        value.uri()?.let { put("uri", JsonPrimitive(it)) }
        value.providerId()?.let { put("providerId", JsonPrimitive(it)) }
    }

    override fun fromJson(element: JsonElement): TokenExchangeResource? {
        val obj = element.asObject() ?: return null
        return TokenExchangeResource(
            obj.at("type")?.let { it.asString() },
            obj.at("id")?.let { it.asString() },
            obj.at("uri")?.let { it.asString() },
            obj.at("providerId")?.let { it.asString() },
        )
    }
}

