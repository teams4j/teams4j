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
        @JsonSubTypes.Type(value = ActionExecute.class, name = "Action.Execute"),
        @JsonSubTypes.Type(value = ActionOpenUrl.class, name = "Action.OpenUrl"),
        @JsonSubTypes.Type(value = ActionShowCard.class, name = "Action.ShowCard"),
        @JsonSubTypes.Type(value = ActionSubmit.class, name = "Action.Submit"),
        @JsonSubTypes.Type(value = ActionToggleVisibility.class, name = "Action.ToggleVisibility"),
        @JsonSubTypes.Type(value = ActionSet.class, name = "ActionSet"),
        @JsonSubTypes.Type(value = Column.class, name = "Column"),
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
public sealed interface CardItem permits ActionExecute, ActionOpenUrl, ActionShowCard, ActionSubmit, ActionToggleVisibility, ActionSet, Column, ColumnSet, Container, FactSet, Image, ImageSet, InputChoiceSet, InputDate, InputNumber, InputText, InputTime, InputToggle, Media, RichTextBlock, Table, TextBlock {
    /**
     * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
     */
    @Nullable
    Map<String, String> requires();

    /**
     * A unique identifier associated with this Action.
     */
    @Nullable
    String id();

    /**
     * Must be `Action.Execute`
     */
    @Nullable
    String type();
}
