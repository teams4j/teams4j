// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Defines how a card can be refreshed by making a request to the target Bot.
 *
 * <p>Since Adaptive Cards 1.4.
 *
 * @param type Must be `Refresh`
 * @param action The action to be executed to refresh the card. Clients can run this refresh action automatically or can provide an affordance for users to trigger it manually.
 * @param expires A timestamp that informs a Host when the card content has expired, and that it should trigger a refresh as appropriate. The format is ISO-8601 Instant format. E.g., 2022-01-01T12:00:00Z
 * @param userIds A list of user Ids informing the client for which users should the refresh action should be run automatically. Some clients will not run the refresh action automatically unless this property is specified. Some clients may ignore this property and always run the refresh action automatically.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("Refresh")
public record Refresh(@JsonProperty("type") @Nullable String type,
        @JsonProperty("action") @Nullable ActionExecute action,
        @JsonProperty("expires") @Nullable String expires,
        @JsonProperty("userIds") @Nullable List<String> userIds) {
    /**
     * The Adaptive Cards type discriminator for this element.
     */
    public static final String TYPE = "Refresh";

    public Refresh {
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be " + TYPE + " but was " + type);
        }
        userIds = userIds == null ? null : List.copyOf(userIds);
    }

    /**
     * Creates a builder for {@link Refresh}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Refresh}.
     */
    public static final class Builder {
        @Nullable
        private String type;

        @Nullable
        private ActionExecute action;

        @Nullable
        private String expires;

        @Nullable
        private List<String> userIds;

        /**
         * Must be `Refresh`
         */
        public Builder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        /**
         * The action to be executed to refresh the card. Clients can run this refresh action automatically or can provide an affordance for users to trigger it manually.
         */
        public Builder action(@Nullable ActionExecute action) {
            this.action = action;
            return this;
        }

        /**
         * A timestamp that informs a Host when the card content has expired, and that it should trigger a refresh as appropriate. The format is ISO-8601 Instant format. E.g., 2022-01-01T12:00:00Z
         *
         * <p>Since Adaptive Cards 1.6.
         */
        public Builder expires(@Nullable String expires) {
            this.expires = expires;
            return this;
        }

        /**
         * A list of user Ids informing the client for which users should the refresh action should be run automatically. Some clients will not run the refresh action automatically unless this property is specified. Some clients may ignore this property and always run the refresh action automatically.
         */
        public Builder userIds(@Nullable List<String> userIds) {
            this.userIds = userIds;
            return this;
        }

        /**
         * Appends to {@code userIds}.
         */
        public Builder addUserId(String... values) {
            List<String> merged = new ArrayList<>(this.userIds == null ? List.of() : this.userIds);
            merged.addAll(List.of(values));
            this.userIds = merged;
            return this;
        }

        /**
         * Builds the {@link Refresh}.
         *
         * @throws NullPointerException if a property the schema requires was not set
         */
        public Refresh build() {
            return new Refresh(type, action, expires, userIds);
        }
    }
}
