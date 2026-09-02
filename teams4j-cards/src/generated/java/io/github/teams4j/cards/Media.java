// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Displays a media player for audio or video content.
 *
 * <p>Since Adaptive Cards 1.1.
 *
 * @param requires A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
 * @param id A unique identifier associated with the item.
 * @param isVisible If `false`, this item will be removed from the visual tree.
 * @param fallback Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
 * @param height Specifies the height of the element.
 * @param separator When `true`, draw a separating line at the top of the element.
 * @param spacing Controls the amount of spacing between this element and the preceding element.
 * @param type Must be `Media`
 * @param sources Array of media sources to attempt to play.
 * @param poster URL of an image to display before playing. Supports data URI in version 1.2+. If poster is omitted, the Media element will either use a default poster (controlled by the host application) or will attempt to automatically pull the poster from the target video service when the source URL points to a video from a Web provider such as YouTube.
 * @param altText Alternate text describing the audio or video.
 * @param captionSources Array of captions sources for the media element to provide.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Media")
public record Media(@JsonProperty("requires") @Nullable Map<String, String> requires,
        @JsonProperty("id") @Nullable String id,
        @JsonProperty("isVisible") @Nullable Boolean isVisible,
        @JsonProperty("fallback") @Nullable ElementFallback fallback,
        @JsonProperty("height") @Nullable BlockElementHeight height,
        @JsonProperty("separator") @Nullable Boolean separator,
        @JsonProperty("spacing") @Nullable Spacing spacing,
        @JsonProperty("type") @Nullable String type,
        @JsonProperty("sources") @Nullable List<MediaSource> sources,
        @JsonProperty("poster") @Nullable String poster,
        @JsonProperty("altText") @Nullable String altText,
        @JsonProperty("captionSources") @Nullable List<CaptionSource> captionSources) implements CardItem, CardElement, ToggleableItem {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Media";

    public Media {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        requires = requires == null ? null : Map.copyOf(requires);
        sources = sources == null ? null : List.copyOf(sources);
        captionSources = captionSources == null ? null : List.copyOf(captionSources);
    }

    /**
     * Creates a builder for {@link Media}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Media}.
     */
    public static final class Builder {
        @Nullable
        private Map<String, String> requires;

        @Nullable
        private String id;

        @Nullable
        private Boolean isVisible;

        @Nullable
        private ElementFallback fallback;

        @Nullable
        private BlockElementHeight height;

        @Nullable
        private Boolean separator;

        @Nullable
        private Spacing spacing;

        @Nullable
        private String type = Media.TYPE;

        @Nullable
        private List<MediaSource> sources;

        @Nullable
        private String poster;

        @Nullable
        private String altText;

        @Nullable
        private List<CaptionSource> captionSources;

        /**
         * A series of key/value pairs indicating features that the item requires with corresponding minimum version. When a feature is missing or of insufficient version, fallback is triggered.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder requires(@Nullable Map<String, String> requires) {
            this.requires = requires;
            return this;
        }

        /**
         * A unique identifier associated with the item.
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * If `false`, this item will be removed from the visual tree.
         *
         * <p>Schema default: {@code true}.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder isVisible(@Nullable Boolean isVisible) {
            this.isVisible = isVisible;
            return this;
        }

        /**
         * Describes what to do when an unknown element is encountered or the requires of this or any children can't be met.
         *
         * <p>Since Adaptive Cards 1.2.
         */
        public Builder fallback(@Nullable ElementFallback fallback) {
            this.fallback = fallback;
            return this;
        }

        /**
         * Specifies the height of the element.
         *
         * <p>Since Adaptive Cards 1.1.
         */
        public Builder height(@Nullable BlockElementHeight height) {
            this.height = height;
            return this;
        }

        /**
         * When `true`, draw a separating line at the top of the element.
         */
        public Builder separator(@Nullable Boolean separator) {
            this.separator = separator;
            return this;
        }

        /**
         * Controls the amount of spacing between this element and the preceding element.
         */
        public Builder spacing(@Nullable Spacing spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Must be `Media`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * Array of media sources to attempt to play.
         */
        public Builder sources(@Nullable List<MediaSource> sources) {
            this.sources = sources;
            return this;
        }

        /**
         * Appends to {@code sources}.
         */
        public Builder addSource(MediaSource... values) {
            List<MediaSource> merged = new ArrayList<>(this.sources == null ? List.of() : this.sources);
            merged.addAll(List.of(values));
            this.sources = merged;
            return this;
        }

        /**
         * URL of an image to display before playing. Supports data URI in version 1.2+. If poster is omitted, the Media element will either use a default poster (controlled by the host application) or will attempt to automatically pull the poster from the target video service when the source URL points to a video from a Web provider such as YouTube.
         */
        public Builder poster(@Nullable String poster) {
            this.poster = poster;
            return this;
        }

        /**
         * Alternate text describing the audio or video.
         */
        public Builder altText(@Nullable String altText) {
            this.altText = altText;
            return this;
        }

        /**
         * Array of captions sources for the media element to provide.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder captionSources(@Nullable List<CaptionSource> captionSources) {
            this.captionSources = captionSources;
            return this;
        }

        /**
         * Appends to {@code captionSources}.
         */
        public Builder addCaptionSource(CaptionSource... values) {
            List<CaptionSource> merged = new ArrayList<>(this.captionSources == null ? List.of() : this.captionSources);
            merged.addAll(List.of(values));
            this.captionSources = merged;
            return this;
        }

        /**
         * Builds the {@link Media}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Media build() {
            Objects.requireNonNull(sources, "sources is required");
            return new Media(requires, id, isVisible, fallback, height, separator, spacing, type, sources, poster, altText, captionSources);
        }
    }
}
