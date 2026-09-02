// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Collects [CardElement] values for a list-valued property.
 */
@CardDsl
public open class CardElementScope internal constructor() {

    internal val values: MutableList<CardElement> = mutableListOf()

    /** Appends a [ActionSet]. */
    public fun actionSet(block: ActionSetDsl.() -> Unit) {
        values += ActionSetDsl().apply(block).build()
    }

    /** Appends a [ColumnSet]. */
    public fun columnSet(block: ColumnSetDsl.() -> Unit) {
        values += ColumnSetDsl().apply(block).build()
    }

    /** Appends a [Container]. */
    public fun container(block: ContainerDsl.() -> Unit) {
        values += ContainerDsl().apply(block).build()
    }

    /** Appends a [FactSet]. */
    public fun factSet(block: FactSetDsl.() -> Unit) {
        values += FactSetDsl().apply(block).build()
    }

    /** Appends a [Image]. */
    public fun image(block: ImageDsl.() -> Unit) {
        values += ImageDsl().apply(block).build()
    }

    /** Same, with `url` set. */
    public fun image(url: String, block: ImageDsl.() -> Unit = {}) {
        values += ImageDsl()
            .apply {
                this.url = url
            }
            .apply(block)
            .build()
    }

    /** Appends a [ImageSet]. */
    public fun imageSet(block: ImageSetDsl.() -> Unit) {
        values += ImageSetDsl().apply(block).build()
    }

    /** Appends a [InputChoiceSet]. */
    public fun inputChoiceSet(block: InputChoiceSetDsl.() -> Unit) {
        values += InputChoiceSetDsl().apply(block).build()
    }

    /** Same, with `id` set. */
    public fun inputChoiceSet(id: String, block: InputChoiceSetDsl.() -> Unit = {}) {
        values += InputChoiceSetDsl()
            .apply {
                this.id = id
            }
            .apply(block)
            .build()
    }

    /** Appends a [InputDate]. */
    public fun inputDate(block: InputDateDsl.() -> Unit) {
        values += InputDateDsl().apply(block).build()
    }

    /** Same, with `id` set. */
    public fun inputDate(id: String, block: InputDateDsl.() -> Unit = {}) {
        values += InputDateDsl()
            .apply {
                this.id = id
            }
            .apply(block)
            .build()
    }

    /** Appends a [InputNumber]. */
    public fun inputNumber(block: InputNumberDsl.() -> Unit) {
        values += InputNumberDsl().apply(block).build()
    }

    /** Same, with `id` set. */
    public fun inputNumber(id: String, block: InputNumberDsl.() -> Unit = {}) {
        values += InputNumberDsl()
            .apply {
                this.id = id
            }
            .apply(block)
            .build()
    }

    /** Appends a [InputText]. */
    public fun inputText(block: InputTextDsl.() -> Unit) {
        values += InputTextDsl().apply(block).build()
    }

    /** Same, with `id` set. */
    public fun inputText(id: String, block: InputTextDsl.() -> Unit = {}) {
        values += InputTextDsl()
            .apply {
                this.id = id
            }
            .apply(block)
            .build()
    }

    /** Appends a [InputTime]. */
    public fun inputTime(block: InputTimeDsl.() -> Unit) {
        values += InputTimeDsl().apply(block).build()
    }

    /** Same, with `id` set. */
    public fun inputTime(id: String, block: InputTimeDsl.() -> Unit = {}) {
        values += InputTimeDsl()
            .apply {
                this.id = id
            }
            .apply(block)
            .build()
    }

    /** Appends a [InputToggle]. */
    public fun inputToggle(block: InputToggleDsl.() -> Unit) {
        values += InputToggleDsl().apply(block).build()
    }

    /** Same, with `id`, `title` set. */
    public fun inputToggle(id: String, title: String, block: InputToggleDsl.() -> Unit = {}) {
        values += InputToggleDsl()
            .apply {
                this.id = id
                this.title = title
            }
            .apply(block)
            .build()
    }

    /** Appends a [Media]. */
    public fun media(block: MediaDsl.() -> Unit) {
        values += MediaDsl().apply(block).build()
    }

    /** Appends a [RichTextBlock]. */
    public fun richTextBlock(block: RichTextBlockDsl.() -> Unit) {
        values += RichTextBlockDsl().apply(block).build()
    }

    /** Appends a [Table]. */
    public fun table(block: TableDsl.() -> Unit) {
        values += TableDsl().apply(block).build()
    }

    /** Appends a [TextBlock]. */
    public fun textBlock(block: TextBlockDsl.() -> Unit) {
        values += TextBlockDsl().apply(block).build()
    }

    /** Same, with `text` set. */
    public fun textBlock(text: String, block: TextBlockDsl.() -> Unit = {}) {
        values += TextBlockDsl()
            .apply {
                this.text = text
                this.wrap = true
            }
            .apply(block)
            .build()
    }

    /** Appends already-built values; the escape hatch to the Java builders. */
    public fun add(vararg items: CardElement) {
        values += items
    }
}
