// Generated from the Adaptive Cards 1.6.0 schema. DO NOT EDIT.
// Regenerate with: ./gradlew generateModel
package io.github.teams4j.cards.kotlin

import io.github.teams4j.cards.*

/**
 * Builds a [DataQuery].
 *
 * The data populated in the event payload for fetching dynamic choices, sent to the card-author to help identify the dataset from which choices might be fetched to be displayed in the dropdown. It might contain auxillary data to limit the maximum number of choices that can be sent and to support pagination.
 */
@CardDsl
public class DataQueryDsl internal constructor() {

    /**
     * The dataset to be queried to get the choices.
     */
    public var dataset: String? = null

    /**
     * The maximum number of choices that should be returned by the query. It can be ignored if the card-author wants to send a different number.
     */
    public var count: Number? = null

    /**
     * The number of choices to be skipped in the list of choices returned by the query. It can be ignored if the card-author does not want pagination.
     */
    public var skip: Number? = null

    internal fun build(): DataQuery = DataQuery.builder()
        .dataset(dataset)
        .count(count)
        .skip(skip)
        .build()
}
