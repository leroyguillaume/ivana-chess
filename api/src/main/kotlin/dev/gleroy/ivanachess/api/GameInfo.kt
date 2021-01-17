package dev.gleroy.ivanachess.api

import dev.gleroy.ivanachess.core.Game
import java.util.*

/**
 * Game information.
 *
 * @param whiteToken Token for white player.
 * @param blackToken Token for black token.
 * @param game Game.
 */
data class GameInfo(
    val whiteToken: UUID = UUID.randomUUID(),
    val blackToken: UUID = UUID.randomUUID(),
    val game: Game = Game()
)