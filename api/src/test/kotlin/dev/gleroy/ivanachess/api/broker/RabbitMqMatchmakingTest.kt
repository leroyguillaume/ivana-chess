@file:Suppress("ClassName")

package dev.gleroy.ivanachess.api.broker

import com.fasterxml.jackson.databind.ObjectMapper
import dev.gleroy.ivanachess.api.Properties
import dev.gleroy.ivanachess.api.io.DefaultMatchConverter
import dev.gleroy.ivanachess.core.*
import dev.gleroy.ivanachess.io.MatchmakingMessage
import dev.gleroy.ivanachess.io.WebSocketSender
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate

internal class RabbitMqMatchmakingTest {
    private val props = Properties()
    private val matchConverter = DefaultMatchConverter()
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    private lateinit var gameService: GameService
    private lateinit var userService: UserService
    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var webSocketSender: WebSocketSender
    private lateinit var queue: RabbitMqMatchmaking

    @BeforeEach
    fun beforeEach() {
        gameService = mockk()
        userService = mockk()
        rabbitTemplate = mockk()
        webSocketSender = mockk()
        queue = RabbitMqMatchmaking(
            gameService = gameService,
            userService = userService,
            matchConverter = matchConverter,
            objectMapper = objectMapper,
            rabbitTemplate = rabbitTemplate,
            webSocketSender = webSocketSender,
            props = props,
        )
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(userService, gameService)
    }

    @Nested
    inner class put {
        private val user = User(
            pseudo = "user",
            email = "user@ivanachess.loc",
            bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS",
        )
        private val message = MatchmakingMessage.Join(user.id, user.pseudo)
        private val messageJson = objectMapper.writeValueAsString(message)

        @Test
        fun `should send join message on queue`() {
            every { rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", messageJson) } returns Unit

            queue.put(user)

            verify { rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", messageJson) }
        }
    }

    @Nested
    inner class remove {
        private val user = User(
            pseudo = "user",
            email = "user@ivanachess.loc",
            bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS",
        )
        private val message = MatchmakingMessage.Leave(user.id, user.pseudo)
        private val messageJson = objectMapper.writeValueAsString(message)

        @Test
        fun `should leave message on queue`() {
            every { rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", messageJson) } returns Unit

            queue.remove(user)

            verify { rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", messageJson) }
        }
    }

    @Nested
    inner class handleMessage {
        private val whitePlayer = User(
            pseudo = "white",
            email = "white@ivanachess.loc",
            bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS",
        )
        private val blackPlayer = User(
            pseudo = "black",
            email = "black@ivanachess.loc",
            bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS",
        )
        private val blackPlayerJoinMessageJson = objectMapper.writeValueAsString(
            MatchmakingMessage.Join(
                id = blackPlayer.id,
                pseudo = blackPlayer.pseudo,
            )
        )
        private val blackPlayerLeaveMessageJson = objectMapper.writeValueAsString(
            MatchmakingMessage.Leave(
                id = blackPlayer.id,
                pseudo = blackPlayer.pseudo,
            )
        )
        private val whitePlayerLeaveMessageJson = objectMapper.writeValueAsString(
            MatchmakingMessage.Leave(
                id = whitePlayer.id,
                pseudo = whitePlayer.pseudo,
            )
        )
        private val match = Match(
            entity = GameEntity(
                whitePlayer = whitePlayer,
                blackPlayer = blackPlayer,
            ),
        )
        private val gameRepresentation = matchConverter.convertToRepresentation(match)

        @Test
        fun `should do nothing if message is not valid`() {
            queue.handleMessage("")
            queue.queue.shouldBeEmpty()
        }

        @Test
        fun `should put user ID in queue if message is join and queue is empty`() {
            queue.handleMessage(blackPlayerJoinMessageJson)
            queue.queue.peek() shouldBe blackPlayer.id
        }

        @Test
        fun `should do nothing if message is join and black player does not exist`() {
            queue.queue.put(whitePlayer.id)

            every { userService.getById(blackPlayer.id) } throws EntityNotFoundException("")

            queue.handleMessage(blackPlayerJoinMessageJson)

            verify { userService.getById(blackPlayer.id) }
        }

        @Test
        fun `should put black player in queue if message is join and white player does not exist`() {
            queue.queue.put(whitePlayer.id)

            every { userService.getById(blackPlayer.id) } returns blackPlayer
            every { userService.getById(whitePlayer.id) } throws EntityNotFoundException("")

            queue.handleMessage(blackPlayerJoinMessageJson)
            queue.queue shouldHaveSize 1
            queue.queue.peek() shouldBe blackPlayer.id

            verify { userService.getById(blackPlayer.id) }
            verify { userService.getById(whitePlayer.id) }
        }

        @Test
        fun `should create game if message is join and all players exists`() {
            queue.queue.put(whitePlayer.id)

            every { userService.getById(blackPlayer.id) } returns blackPlayer
            every { userService.getById(whitePlayer.id) } returns whitePlayer
            every { gameService.create(whitePlayer, blackPlayer) } returns match
            every { webSocketSender.sendGame(gameRepresentation) } returns Unit
            every {
                rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", whitePlayerLeaveMessageJson)
            } returns Unit
            every {
                rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", blackPlayerLeaveMessageJson)
            } returns Unit

            queue.handleMessage(blackPlayerJoinMessageJson)
            queue.queue.shouldBeEmpty()

            verify { userService.getById(blackPlayer.id) }
            verify { userService.getById(whitePlayer.id) }
            verify { gameService.create(whitePlayer, blackPlayer) }
            verify { webSocketSender.sendGame(gameRepresentation) }
            verify { rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", whitePlayerLeaveMessageJson) }
            verify { rabbitTemplate.convertAndSend(props.broker.matchmakingExchange, "", blackPlayerLeaveMessageJson) }
        }

        @Test
        fun `should do nothing if user if message is leave and user is not in queue`() {
            queue.queue.put(whitePlayer.id)

            queue.handleMessage(blackPlayerLeaveMessageJson)
            queue.queue.peek() shouldBe whitePlayer.id
        }

        @Test
        fun `should remove user from queue if message is leave and user is in queue`() {
            queue.queue.put(whitePlayer.id)
            queue.queue.put(blackPlayer.id)

            queue.handleMessage(blackPlayerLeaveMessageJson)
            queue.queue.peek() shouldBe whitePlayer.id
        }
    }
}
