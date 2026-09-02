// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The data populated in the event payload for fetching dynamic choices, sent to the card-author to help identify the dataset from which choices might be fetched to be displayed in the dropdown. It might contain auxillary data to limit the maximum number of choices that can be sent and to support pagination.
 *
 * @param type Must be `Data.Query`
 * @param dataset The dataset to be queried to get the choices.
 * @param count The maximum number of choices that should be returned by the query. It can be ignored if the card-author wants to send a different number.
 * @param skip The number of choices to be skipped in the list of choices returned by the query. It can be ignored if the card-author does not want pagination.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Data.Query")
public record DataQuery(@JsonProperty("type") @Nullable String type,
        @JsonProperty("dataset") @Nullable String dataset,
        @JsonProperty("count") @Nullable Number count,
        @JsonProperty("skip") @Nullable Number skip) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Data.Query";

    public DataQuery {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
    }

    /**
     * Creates a builder for {@link DataQuery}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link DataQuery}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private String dataset;

        @Nullable
        private Number count;

        @Nullable
        private Number skip;

        /**
         * Must be `Data.Query`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The dataset to be queried to get the choices.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder dataset(@Nullable String dataset) {
            this.dataset = dataset;
            return this;
        }

        /**
         * The maximum number of choices that should be returned by the query. It can be ignored if the card-author wants to send a different number.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder count(@Nullable Number count) {
            this.count = count;
            return this;
        }

        /**
         * The number of choices to be skipped in the list of choices returned by the query. It can be ignored if the card-author does not want pagination.
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder skip(@Nullable Number skip) {
            this.skip = skip;
            return this;
        }

        /**
         * Builds the {@link DataQuery}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public DataQuery build() {
            Objects.requireNonNull(dataset, "dataset is required");
            return new DataQuery(type, dataset, count, skip);
        }
    }
}
