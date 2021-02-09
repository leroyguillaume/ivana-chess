@file:Suppress("ClassName")

package dev.gleroy.ivanachess.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dev.gleroy.ivanachess.core.*
import dev.gleroy.ivanachess.dto.*
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
internal class GameControllerTest : AbstractControllerTest() {
    private val gameInfo = GameInfo()

    @MockBean
    private lateinit var service: GameService

    @Autowired
    private lateinit var pageConverter: PageConverter

    @Autowired
    private lateinit var gameInfoConverter: GameInfoConverter

    @Autowired
    private lateinit var asciiBoardSerializer: AsciiBoardSerializer

    @LocalServerPort
    private var serverPort: Int = 0

    private lateinit var wsClient: WebSocketStompClient
    private lateinit var wsSession: StompSession

    @BeforeEach
    fun beforeEach() {
        wsClient = WebSocketStompClient(SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))).apply {
            messageConverter = MappingJackson2MessageConverter().apply { objectMapper = mapper }
        }
        wsSession = wsClient.connect(
            "ws://localhost:$serverPort$WebSocketPath",
            object : StompSessionHandlerAdapter() {}
        ).get(1, TimeUnit.SECONDS)
    }

    @Nested
    inner class asciiBoard {
        @Test
        fun `should return game_not_found if game does not exist`() {
            whenever(service.getById(gameInfo.id)).thenThrow(PlayException.GameIdNotFound(gameInfo.id))

            val responseBody = mvc.get("$GameApiPath/${gameInfo.id}$BoardAsciiPath")
                .andDo { print() }
                .andExpect { status { isNotFound() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.GameNotFound>(responseBody).shouldBeInstanceOf<ErrorDto.GameNotFound>()

            verify(service).getById(gameInfo.id)
        }

        @Test
        fun `should return ascii board representation`() {
            whenever(service.getById(gameInfo.id)).thenReturn(gameInfo)

            val responseBody = mvc.get("$GameApiPath/${gameInfo.id}$BoardAsciiPath")
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    header {
                        string(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")
                    }
                }
                .andReturn()
                .response
                .contentAsByteArray
            responseBody shouldBe asciiBoardSerializer.serialize(gameInfo.game.board)

            verify(service).getById(gameInfo.id)
        }
    }

    @Nested
    inner class create {
        private lateinit var gameDto: GameDto

        @BeforeEach
        fun beforeEach() {
            gameDto = gameInfoConverter.convert(gameInfo)
        }

        @Test
        fun `should create new game`() {
            whenever(service.create()).thenReturn(gameInfo)

            val responseBody = mvc.post(GameApiPath)
                .andDo { print() }
                .andExpect { status { isCreated() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<GameDto>(responseBody) shouldBe gameDto

            verify(service).create()
        }
    }

    @Nested
    inner class get {
        private lateinit var gameDto: GameDto

        @BeforeEach
        fun beforeEach() {
            gameDto = gameInfoConverter.convert(gameInfo)
        }

        @Test
        fun `should return game_not_found if game does not exist`() {
            whenever(service.getById(gameInfo.id)).thenThrow(PlayException.GameIdNotFound(gameInfo.id))

            val responseBody = mvc.get("$GameApiPath/${gameDto.id}")
                .andDo { print() }
                .andExpect { status { isNotFound() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.GameNotFound>(responseBody).shouldBeInstanceOf<ErrorDto.GameNotFound>()

            verify(service).getById(gameInfo.id)
        }

        @Test
        fun `should return game`() {
            whenever(service.getById(gameInfo.id)).thenReturn(gameInfo)

            val responseBody = mvc.get("$GameApiPath/${gameDto.id}")
                .andDo { print() }
                .andExpect { status { isOk() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<GameDto>(responseBody) shouldBe gameDto

            verify(service).getById(gameInfo.id)
        }
    }

    @Nested
    inner class getAll : AbstractControllerTest.Paginated() {
        override val method = HttpMethod.GET
        override val path = GameApiPath

        private val pageNb = 2
        private val size = 4
        private val page = Page(
            content = listOf(GameInfo()),
            number = pageNb,
            totalItems = 5,
            totalPages = 6
        )
        private val responseDto = pageConverter.convert(page) { gameInfoConverter.convert(it) }

        @Test
        fun `should return page`() {
            whenever(service.getAll(pageNb, size)).thenReturn(page)

            val responseBody = mvc.request(method, path) {
                param(PageParam, "$pageNb")
                param(SizeParam, "$size")
            }
                .andDo { print() }
                .andExpect { status { isOk() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<PageDto<GameDto>>(responseBody) shouldBe responseDto

            verify(service).getAll(pageNb, size)
        }
    }

    @Nested
    inner class play : AbstractControllerTest.WithBody() {
        private val move = Move.Simple.fromCoordinates("A2", "A4")

        private lateinit var gameDto: GameDto

        override val method = HttpMethod.PUT
        override val path = "$GameApiPath/${gameInfo.whiteToken}$PlayPath"
        override val requestDto = MoveDto.from(move)
        override val invalidRequests = listOf(
            InvalidRequest(
                requestDto = MoveDto.Simple(
                    from = PositionDto(Position.Min - 1, Position.Min - 1),
                    to = PositionDto(Position.Min - 1, Position.Min - 1)
                ),
                responseDto = ErrorDto.Validation(
                    errors = setOf(
                        ErrorDto.InvalidParameter(
                            parameter = "from.col",
                            reason = "must be greater than or equal to ${Position.Min}"
                        ),
                        ErrorDto.InvalidParameter(
                            parameter = "from.row",
                            reason = "must be greater than or equal to ${Position.Min}"
                        ),
                        ErrorDto.InvalidParameter(
                            parameter = "to.col",
                            reason = "must be greater than or equal to ${Position.Min}"
                        ),
                        ErrorDto.InvalidParameter(
                            parameter = "to.row",
                            reason = "must be greater than or equal to ${Position.Min}"
                        )
                    )
                )
            ),
            InvalidRequest(
                requestDto = MoveDto.Simple(
                    from = PositionDto(Position.Max + 1, Position.Max + 1),
                    to = PositionDto(Position.Max + 1, Position.Max + 1)
                ),
                responseDto = ErrorDto.Validation(
                    errors = setOf(
                        ErrorDto.InvalidParameter(
                            parameter = "from.col",
                            reason = "must be less than or equal to ${Position.Max}"
                        ),
                        ErrorDto.InvalidParameter(
                            parameter = "from.row",
                            reason = "must be less than or equal to ${Position.Max}"
                        ),
                        ErrorDto.InvalidParameter(
                            parameter = "to.col",
                            reason = "must be less than or equal to ${Position.Max}"
                        ),
                        ErrorDto.InvalidParameter(
                            parameter = "to.row",
                            reason = "must be less than or equal to ${Position.Max}"
                        )
                    )
                )
            ),
            InvalidRequest(
                requestDto = NullableMoveDto.Simple(),
                responseDto = ErrorDto.Validation(
                    errors = setOf(
                        ErrorDto.InvalidParameter(
                            parameter = "from",
                            reason = "must not be null"
                        )
                    )
                )
            )
        )

        private lateinit var blockingQueue: ArrayBlockingQueue<GameDto>

        @BeforeEach
        fun beforeEach() {
            gameDto = gameInfoConverter.convert(gameInfo)
            blockingQueue = ArrayBlockingQueue(1)
        }

        @Test
        fun `should return game_not_found if game does not exist`() {
            val token = gameInfo.whiteToken
            whenever(service.getByToken(token)).thenThrow(PlayException.GameTokenNotFound(token))

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isNotFound() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.GameNotFound>(responseBody).shouldBeInstanceOf<ErrorDto.GameNotFound>()

            verify(service).getByToken(token)
        }

        @Test
        fun `should return invalid_move if move is invalid`() {
            val token = gameInfo.whiteToken
            val exception = PlayException.InvalidMove(
                id = gameInfo.id,
                token = token,
                color = Piece.Color.White,
                move = move,
                cause = InvalidMoveException("Invalid move")
            )
            whenever(service.getByToken(token)).thenReturn(gameInfo)
            whenever(service.play(gameInfo, exception.token, move)).thenThrow(exception)

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isPreconditionFailed() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.InvalidMove>(responseBody) shouldBe ErrorDto.InvalidMove(exception.cause.message)

            verify(service).getByToken(token)
            verify(service).play(gameInfo, exception.token, move)
        }

        @Test
        fun `should return invalid_player if player is invalid`() {
            val token = gameInfo.whiteToken
            val exception = PlayException.InvalidPlayer(
                id = gameInfo.id,
                token = token,
                color = Piece.Color.White
            )
            whenever(service.getByToken(token)).thenReturn(gameInfo)
            whenever(service.play(gameInfo, exception.token, move)).thenThrow(exception)

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isPreconditionFailed() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.InvalidPlayer>(responseBody).shouldBeInstanceOf<ErrorDto.InvalidPlayer>()

            verify(service).getByToken(token)
            verify(service).play(gameInfo, exception.token, move)
        }

        @Test
        fun `should return updated game`() {
            val token = gameInfo.whiteToken
            whenever(service.getByToken(token)).thenReturn(gameInfo)
            whenever(service.play(gameInfo, token, move)).thenReturn(gameInfo)

            wsSession.subscribe("$TopicPath$GameApiPath/${gameInfo.id}", object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders) = GameDto::class.java

                override fun handleFrame(headers: StompHeaders, payload: Any?) {
                    blockingQueue.add(payload as GameDto)
                }
            })

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isOk() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<GameDto>(responseBody) shouldBe gameDto

            blockingQueue.poll(1, TimeUnit.SECONDS) shouldBe gameDto

            verify(service).getByToken(token)
            verify(service).play(gameInfo, token, move)
        }
    }

    private sealed class NullableMoveDto {
        data class Simple(
            override val from: PositionDto? = null,
            override val to: PositionDto? = null
        ) : NullableMoveDto() {
            override val type = MoveDto.Type.Simple
        }

        abstract val type: MoveDto.Type
        abstract val from: PositionDto?
        abstract val to: PositionDto?
    }
}
