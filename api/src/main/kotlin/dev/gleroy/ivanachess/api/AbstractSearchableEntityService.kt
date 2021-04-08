package dev.gleroy.ivanachess.api

import java.util.*

/**
 * Abstract implementation of searchable entity service.
 *
 * @param E Type of entity.
 */
abstract class AbstractSearchableEntityService<E : SearchableEntity> :
    AbstractEntityService<E>(), SearchableEntityService<E> {

    abstract override val repository: SearchableEntityRepository<E>

    override fun search(
        term: String,
        fields: Set<SearchableEntityField<E>>,
        pageOpts: PageOptions<E>,
        excluding: Set<UUID>
    ) = repository.search(term, fields, pageOpts, excluding)
}