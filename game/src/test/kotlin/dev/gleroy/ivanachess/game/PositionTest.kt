@file:Suppress("ClassName")

package dev.gleroy.ivanachess.game

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.throwable.shouldHaveMessage
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PositionTest {
    @Nested
    inner class all {
        @Test
        fun `should return all positions`() {
            Position.all() shouldHaveSize 64
        }
    }

    @Nested
    inner class fromCoordinates {
        @Test
        fun `should throw exception if coordinates string is invalid`() {
            val exception = assertThrows<IllegalArgumentException> { Position.fromCoordinates("8A") }
            exception shouldHaveMessage "coordinates must match ${Position.CoordinatesRegex.pattern}"
        }

        @Test
        fun `should return B4`() {
            Position.fromCoordinates("B4") shouldBe Position(2, 4)
        }
    }

    @Nested
    inner class constructor {
        @Test
        fun `should throw exception if col lower than 1`() {
            shouldThrowExceptionIfIndexIsOutOfRange("col") { Position(0, 1) }
        }

        @Test
        fun `should throw exception if col greater than 8`() {
            shouldThrowExceptionIfIndexIsOutOfRange("col") { Position(9, 1) }
        }

        @Test
        fun `should throw exception if row lower than 1`() {
            shouldThrowExceptionIfIndexIsOutOfRange("row") { Position(1, 0) }
        }

        @Test
        fun `should throw exception if row greater than 8`() {
            shouldThrowExceptionIfIndexIsOutOfRange("row") { Position(1, 9) }
        }

        private fun shouldThrowExceptionIfIndexIsOutOfRange(propertyName: String, instantiate: () -> Position) {
            val exception = assertThrows<IllegalArgumentException> { instantiate() }
            exception shouldHaveMessage "$propertyName must be between ${Position.Min} and ${Position.Max}"
        }
    }

    @Nested
    inner class relativePosition {
        @Test
        fun `should return null if position col is 0`() {
            Position(1, 1).relativePosition(-1).shouldBeNull()
        }

        @Test
        fun `should return null if position col is 9`() {
            Position(Position.Max, Position.Max).relativePosition(1).shouldBeNull()
        }

        @Test
        fun `should return null if position row is 0`() {
            Position(1, 1).relativePosition(rowOffset = -1).shouldBeNull()
        }

        @Test
        fun `should return null if position row is 9`() {
            Position(Position.Max, Position.Max).relativePosition(rowOffset = 1).shouldBeNull()
        }

        @Test
        fun `should return relative top left position`() {
            Position(1, 1).relativePosition(1, 1) shouldBe Position(2, 2)
        }

        @Test
        fun `should return relative bottom right position`() {
            Position(2, 2).relativePosition(-1, -1) shouldBe Position(1, 1)
        }
    }

    @Nested
    inner class toString {
        @Test
        fun `should return A1`() {
            Position(1, 1).toString() shouldBe "A1"
        }

        @Test
        fun `should return B3`() {
            Position(2, 3).toString() shouldBe "B3"
        }
    }
}
