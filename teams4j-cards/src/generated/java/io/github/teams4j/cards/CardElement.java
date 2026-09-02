// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.jspecify.annotations.Nullable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ActionSet.class, name = "ActionSet"),
        @JsonSubTypes.Type(value = ColumnSet.class, name = "ColumnSet"),
        @JsonSubTypes.Type(value = Container.class, name = "Container"),
        @JsonSubTypes.Type(value = FactSet.class, name = "FactSet"),
        @JsonSubTypes.Type(value = Image.class, name = "Image"),
        @JsonSubTypes.Type(value = ImageSet.class, name = "ImageSet"),
        @JsonSubTypes.Type(value = InputChoiceSet.class, name = "Input.ChoiceSet"),
        @JsonSubTypes.Type(value = InputDate.class, name = "Input.Date"),
        @JsonSubTypes.Type(value = InputNumber.class, name = "Input.Number"),
        @JsonSubTypes.Type(value = InputText.class, name = "Input.Text"),
        @JsonSubTypes.Type(value = InputTime.class, name = "Input.Time"),
        @JsonSubTypes.Type(value = InputToggle.class, name = "Input.Toggle"),
        @JsonSubTypes.Type(value = Media.class, name = "Media"),
        @JsonSubTypes.Type(value = RichTextBlock.class, name = "RichTextBlock"),
        @JsonSubTypes.Type(value = Table.class, name = "Table"),
        @JsonSubTypes.Type(value = TextBlock.class, name = "TextBlock")
})
public sealed interface CardElement permits ActionSet, ColumnSet, Container, FactSet, Image, ImageSet, InputChoiceSet, InputDate, InputNumber, InputText, InputTime, InputToggle, Media, RichTextBlock, Table, TextBlock {
    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    @Nullable
    Map<String, String> requires();

    /**
     * A unique identifier associated with the item.
     */
    @Nullable
    String id();

    /**
     * If `false`, this item will be removed from the visual tree.
     */
    @Nullable
    Boolean isVisible();

    /**
     * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
     */
    @Nullable
    ElementFallback fallback();

    /**
     * When `true`, draw a separating line at the top of the element.
     */
    @Nullable
    Boolean separator();

    /**
     * Controls the amount of spacing between this element and the preceding element.
     */
    @Nullable
    Spacing spacing();

    /**
     * Must be `ActionSet`
     */
    @Nullable
    String type();
}
