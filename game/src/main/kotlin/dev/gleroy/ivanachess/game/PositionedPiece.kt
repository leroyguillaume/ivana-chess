package dev.gleroy.ivanachess.game

/**
 * Positioned piece.
 *
 * @param piece Piece.
 * @param pos Position.
 */
data class PositionedPiece(
    val piece: Piece,
    val pos: Position
) {
    override fun toString() = "$pos=$piece"
}
