@file:Suppress("ClassName")

package dev.gleroy.ivanachess.core

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class StringBoardSerializerTest {
    private val serializer = StringBoardSerializer()

    @Nested
    inner class serialize {
        @Test
        fun `should return serialized board`() {
            val expected = String(javaClass.getResourceAsStream("/board/initial.txt").readAllBytes())
            val str = String(serializer.serialize(Board.Initial))
            str shouldBe expected
        }
    }
}
