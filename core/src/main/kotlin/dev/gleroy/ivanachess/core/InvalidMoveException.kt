package dev.gleroy.ivanachess.core

import dev.gleroy.ivanachess.game.Move
import java.util.*

/**
 * Exception thrown when a player tries to play invalid move.
 *
 * @param id Game ID.
 * @param player Player
 * @param move Move.
 */
data class InvalidMoveException(
    val id: UUID,
    val player: User,
    val move: Move
) : RuntimeException() {
    override val message = "User '${player.pseudo}' (${player.id}) tries to play invalid move $move in game $id"
}
