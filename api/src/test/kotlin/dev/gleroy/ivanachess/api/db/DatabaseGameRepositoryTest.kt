@file:Suppress("ClassName", "SqlResolve")

package dev.gleroy.ivanachess.api.db

import dev.gleroy.ivanachess.api.GameSummary
import dev.gleroy.ivanachess.api.Page
import dev.gleroy.ivanachess.core.Game
import dev.gleroy.ivanachess.core.Move
import dev.gleroy.ivanachess.core.Piece
import dev.gleroy.ivanachess.core.Position
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.throwable.shouldHaveMessage
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZoneOffset
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class DatabaseGameRepositoryTest {
    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var repository: DatabaseGameRepository

    private lateinit var gameSummaries: List<GameSummary>
    private lateinit var gameSummary: GameSummary

    @BeforeEach
    fun beforeEach() {
        gameSummaries = (0 until 100)
            .map { repository.save().atUtc() }
            .reversed()
        gameSummary = gameSummaries.first()
    }

    @Suppress("SqlWithoutWhere")
    @AfterEach
    fun afterEach() {
        jdbcTemplate.update(
            "DELETE FROM \"${DatabaseConstants.Game.TableName}\"",
            ComparableMapSqlParameterSource()
        )
    }

    @Nested
    inner class exists {
        @Test
        fun `should return false`() {
            repository.exists(UUID.randomUUID()).shouldBeFalse()
        }

        @Test
        fun `should return true`() {
            repository.exists(gameSummary.id).shouldBeTrue()
        }
    }

    @Nested
    inner class getAll {
        @Test
        fun `should throw exception if page is 0`() {
            val exception = assertThrows<IllegalArgumentException> { repository.getAll(0, 1) }
            exception shouldHaveMessage "page must be strictly positive"
        }

        @Test
        fun `should throw exception if size is 0`() {
            val exception = assertThrows<IllegalArgumentException> { repository.getAll(1, 0) }
            exception shouldHaveMessage "size must be strictly positive"
        }

        @Test
        fun `should return first page`() {
            val page = 1
            val size = 3
            repository.getAll(page, size) shouldBe Page(
                content = gameSummaries.subList(gameSummaries.size - size, gameSummaries.size).reversed(),
                number = page,
                totalItems = gameSummaries.size,
                totalPages = 34
            )
        }

        @Test
        fun `should return last page`() {
            val page = 34
            val size = 3
            repository.getAll(page, size) shouldBe Page(
                content = gameSummaries.subList(0, 1),
                number = page,
                totalItems = gameSummaries.size,
                totalPages = 34
            )
        }
    }

    @Nested
    inner class getById {
        @Test
        fun `should return null`() {
            repository.getById(UUID.randomUUID()).shouldBeNull()
        }

        @Test
        fun `should return game`() {
            repository.getById(gameSummary.id) shouldBe gameSummary
        }
    }

    @Nested
    inner class getByToken {
        @Test
        fun `should return null`() {
            repository.getByToken(UUID.randomUUID()).shouldBeNull()
        }

        @Test
        fun `should return game with white token`() {
            repository.getByToken(gameSummary.whiteToken) shouldBe gameSummary
        }

        @Test
        fun `should return game with black token`() {
            repository.getByToken(gameSummary.blackToken) shouldBe gameSummary
        }
    }

    @Nested
    inner class getMoves {
        @Test
        fun `should return moves`() {
            val moves = listOf(
                Move.Simple.fromCoordinates("E2", "E4"),
                Move.Simple.fromCoordinates("E7", "E5"),
                Move.Simple.fromCoordinates("F2", "F4"),
                Move.Simple.fromCoordinates("H7", "H6"),
                Move.Simple.fromCoordinates("F4", "E5"),
                Move.Simple.fromCoordinates("F7", "F6"),
                Move.Simple.fromCoordinates("E5", "F6"),
                Move.Simple.fromCoordinates("G7", "G5"),
                Move.Simple.fromCoordinates("F6", "F7"),
                Move.Simple.fromCoordinates("E8", "E7"),
                Move.Promotion(
                    from = Position.fromCoordinates("F7"),
                    to = Position.fromCoordinates("G8"),
                    promotion = Piece.Queen(Piece.Color.White)
                )
            )
            repository.save(gameSummary, moves)
            repository.getMoves(gameSummary.id) shouldBe moves
        }
    }

    @Nested
    inner class save {
        @Test
        fun `should create new game`() {
            val gameInfo = repository.save().atUtc()
            repository.getById(gameInfo.id) shouldBe gameInfo
        }

        @Test
        fun `should update game`() {
            val game = Game(
                moves = listOf(
                    Move.Simple.fromCoordinates("E2", "E4"),
                    Move.Simple.fromCoordinates("E7", "E5"),
                    Move.Simple.fromCoordinates("F2", "F4"),
                    Move.Simple.fromCoordinates("H7", "H6"),
                    Move.Simple.fromCoordinates("F4", "E5"),
                    Move.Simple.fromCoordinates("F7", "F6"),
                    Move.Simple.fromCoordinates("E5", "F6"),
                    Move.Simple.fromCoordinates("G7", "G5"),
                    Move.Simple.fromCoordinates("F6", "F7"),
                    Move.Simple.fromCoordinates("E8", "E7"),
                    Move.Promotion(
                        from = Position.fromCoordinates("F7"),
                        to = Position.fromCoordinates("G8"),
                        promotion = Piece.Queen(Piece.Color.White)
                    )
                )
            )
            val gameSummary = gameSummary.copy(
                turnColor = game.turnColor,
                state = game.state
            )
            repository.save(gameSummary, game.moves)
            repository.getById(gameSummary.id) shouldBe gameSummary
        }
    }

    private fun GameSummary.atUtc() = copy(creationDate = creationDate.withOffsetSameInstant(ZoneOffset.UTC))
}