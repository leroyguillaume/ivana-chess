package dev.gleroy.ivanachess.io

import dev.gleroy.ivanachess.core.CommonEntityField
import javax.validation.constraints.Min

/**
 * Page query parameters.
 *
 * If sort field starts with '-', the sort order will be descending.
 *
 * @param page Page number.
 * @param size Page size.
 * @param sort List of sorts.
 * @param filter List of filters.
 */
data class PageQueryParameters(
    @field:Min(ApiConstants.Constraints.MinPage.toLong())
    val page: Int = 1,

    @field:Min(ApiConstants.Constraints.MinPageSize.toLong())
    val size: Int = 10,

    val sort: List<String> = listOf(
        CommonEntityField.CreationDate.label,
        CommonEntityField.Id.label,
    ),

    val filter: Set<String> = emptySet()
)
