package dev.gleroy.ivanachess.core

import dev.gleroy.ivanachess.game.Move
import java.util.*

/**
 * Game repository.
 */
interface GameRepository : EntityRepository<GameEntity> {
    /**
     * Fetch list of moves since the begin of the game.
     *
     * @param id Game entity ID.
     * @return List of moves since the begin of the game.
     */
    fun fetchMoves(id: UUID): List<Move>

    /**
     * Save game entity.
     *
     * @param id Game entity ID.
     * @param moves List of moves since the begin of the game.
     */
    fun saveMoves(id: UUID, moves: List<Move>)
}
