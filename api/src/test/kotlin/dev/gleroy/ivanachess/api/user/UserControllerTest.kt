@file:Suppress("ClassName")

package dev.gleroy.ivanachess.api.user

import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dev.gleroy.ivanachess.api.AbstractControllerTest
import dev.gleroy.ivanachess.api.ApiConstants
import dev.gleroy.ivanachess.api.InvalidRequest
import dev.gleroy.ivanachess.api.PageConverter
import dev.gleroy.ivanachess.api.security.Jwt
import dev.gleroy.ivanachess.dto.ErrorDto
import dev.gleroy.ivanachess.dto.LogInDto
import dev.gleroy.ivanachess.dto.UserDto
import dev.gleroy.ivanachess.dto.UserSubscriptionDto
import io.kotlintest.matchers.numerics.shouldBeZero
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.should
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.request
import java.time.*
import javax.servlet.http.Cookie

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
internal class UserControllerTest : AbstractControllerTest() {
    private val user = User(
        pseudo = "admin",
        email = "admin@ivanachess.loc",
        bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS"
    )
    private val now = Instant.now()

    @MockBean
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @MockBean
    private lateinit var service: UserService

    @MockBean
    private lateinit var clock: Clock

    @Autowired
    private lateinit var pageConverter: PageConverter

    @Autowired
    private lateinit var userConverter: UserConverter

    @Nested
    inner class logIn : WithBody() {
        override val method = HttpMethod.POST
        override val path = "${ApiConstants.User.Path}/${ApiConstants.User.LogInPath}"
        override val requestDto = LogInDto(
            pseudo = "user",
            password = "changeit"
        )
        override val invalidRequests = emptyList<InvalidRequest>()

        private lateinit var jwt: Jwt

        @BeforeEach
        fun beforeEach() {
            jwt = Jwt(
                pseudo = requestDto.pseudo,
                expirationDate = OffsetDateTime.ofInstant(
                    now.plusSeconds(props.auth.validity.toLong()),
                    ZoneOffset.systemDefault()
                ),
                token = "token"
            )
        }

        @Test
        fun `should return unauthorized if bad credentials`() {
            whenever(authService.generateJwt(requestDto.pseudo, requestDto.password))
                .thenThrow(BadCredentialsException(""))

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isUnauthorized() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.Unauthorized>(responseBody).shouldBeInstanceOf<ErrorDto.Unauthorized>()

            verify(authService).generateJwt(requestDto.pseudo, requestDto.password)
        }

        @Test
        fun `should generate JWT`() {
            whenever(authService.generateJwt(requestDto.pseudo, requestDto.password)).thenReturn(jwt)
            whenever(clock.instant()).thenReturn(now)

            val response = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isNoContent() } }
                .andReturn()
                .response
            response.contentLength.shouldBeZero()
            response.getHeaderValue(props.auth.header.name) shouldBe "${props.auth.header.valuePrefix}${jwt.token}"
            response.getCookie(props.auth.cookie.name) should { cookie ->
                cookie.shouldNotBeNull()
                cookie.domain shouldBe props.auth.cookie.domain
                cookie.secure shouldBe props.auth.cookie.secure
                cookie.isHttpOnly shouldBe props.auth.cookie.httpOnly
                cookie.maxAge shouldBe Duration.between(now, jwt.expirationDate).toSeconds().toInt()
                cookie.value shouldBe jwt.token
            }

            verify(authService).generateJwt(requestDto.pseudo, requestDto.password)
            verify(clock).instant()
        }
    }

    @Nested
    inner class logOut {
        @Test
        fun `should return unauthorized`() {
            val responseBody = mvc.request(
                method = HttpMethod.DELETE,
                urlTemplate = "${ApiConstants.User.Path}/${ApiConstants.User.LogOutPath}"
            )
                .andDo { print() }
                .andExpect { status { isUnauthorized() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.Unauthorized>(responseBody).shouldBeInstanceOf<ErrorDto.Unauthorized>()
        }

        @Test
        fun `should delete cookie (with header auth)`() {
            shouldDeleteAuthenticationCookie { authenticationHeader(it) }
        }

        @Test
        fun `should delete cookie (with cookie auth)`() {
            shouldDeleteAuthenticationCookie { authenticationCookie(it) }
        }

        private fun shouldDeleteAuthenticationCookie(auth: MockHttpServletRequestDsl.(Jwt) -> Unit) =
            withAuthentication(simpleUser) { jwt ->
                val response = mvc.request(
                    method = HttpMethod.DELETE,
                    urlTemplate = "${ApiConstants.User.Path}/${ApiConstants.User.LogOutPath}"
                ) { auth(jwt) }
                    .andDo { print() }
                    .andExpect { status { isNoContent() } }
                    .andReturn()
                    .response
                response.contentLength.shouldBeZero()
                response.getCookie(props.auth.cookie.name).shouldBeAuthenticationCookie("", 0)
            }
    }

    @Nested
    inner class signUp : WithBody() {
        private val bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS"

        override val method = HttpMethod.POST
        override val path = "${ApiConstants.User.Path}/${ApiConstants.User.SignUpPath}"
        override val requestDto = UserSubscriptionDto(
            pseudo = "   ${user.pseudo}   ",
            email = " ${user.email}   ",
            password = "admin"
        )
        override val invalidRequests = listOf(
            InvalidRequest(
                requestDto = UserSubscriptionDto(
                    pseudo = " ",
                    email = " user@ivanachess.loc   ",
                    password = ""
                ),
                responseDto = ErrorDto.Validation(
                    errors = setOf(
                        stringSizeInvalidParameter(
                            parameter = "pseudo",
                            min = UserSubscriptionDto.PseudoMinLength,
                            max = UserSubscriptionDto.PseudoMaxLength
                        ),
                        stringSizeInvalidParameter(
                            parameter = "password",
                            min = UserSubscriptionDto.PasswordMinLength,
                            max = UserSubscriptionDto.PasswordMaxLength
                        ),
                    )
                )
            ),
            InvalidRequest(
                requestDto = UserSubscriptionDto(
                    pseudo = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    email = " user@ivanachess.loc   ",
                    password = "changeit"
                ),
                responseDto = ErrorDto.Validation(
                    errors = setOf(
                        stringSizeInvalidParameter(
                            parameter = "pseudo",
                            min = UserSubscriptionDto.PseudoMinLength,
                            max = UserSubscriptionDto.PseudoMaxLength
                        )
                    )
                )
            ),
            InvalidRequest(
                requestDto = UserSubscriptionDto(
                    pseudo = "admin",
                    email = "email",
                    password = "changeit"
                ),
                responseDto = ErrorDto.Validation(
                    errors = setOf(
                        ErrorDto.InvalidParameter(
                            parameter = "email",
                            reason = "must be a well-formed email address"
                        )
                    )
                )
            )
        )

        private lateinit var userDto: UserDto

        @BeforeEach
        fun beforeEach() {
            userDto = userConverter.convert(user)
        }

        @Test
        fun `should return forbidden if user is authenticated`() = withAuthentication(simpleUser) { jwt ->
            val responseBody = mvc.request(method, path) {
                authenticationHeader(jwt)
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isForbidden() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.Forbidden>(responseBody).shouldBeInstanceOf<ErrorDto.Forbidden>()
        }

        @Test
        fun `should return pseudo_already_used if pseudo is already used`() {
            whenever(passwordEncoder.encode(requestDto.password)).thenReturn(bcryptPassword)
            whenever(service.create(user.pseudo, user.email, bcryptPassword))
                .thenThrow(UserPseudoAlreadyUsedException(user.pseudo))

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isConflict() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.UserPseudoAlreadyUsed>(responseBody) shouldBe ErrorDto.UserPseudoAlreadyUsed(
                pseudo = user.pseudo
            )

            verify(passwordEncoder).encode(requestDto.password)
            verify(service).create(user.pseudo, user.email, bcryptPassword)
        }

        @Test
        fun `should return email_already_used if email is already used`() {
            whenever(passwordEncoder.encode(requestDto.password)).thenReturn(bcryptPassword)
            whenever(service.create(user.pseudo, user.email, bcryptPassword))
                .thenThrow(UserEmailAlreadyUsedException(user.email))

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isConflict() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.UserEmailAlreadyUsed>(responseBody) shouldBe ErrorDto.UserEmailAlreadyUsed(
                email = user.email
            )

            verify(passwordEncoder).encode(requestDto.password)
            verify(service).create(user.pseudo, user.email, bcryptPassword)
        }

        @Test
        fun `should return create user`() {
            whenever(passwordEncoder.encode(requestDto.password)).thenReturn(bcryptPassword)
            whenever(service.create(user.pseudo, user.email, bcryptPassword)).thenReturn(user)

            val responseBody = mvc.request(method, path) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsBytes(requestDto)
            }
                .andDo { print() }
                .andExpect { status { isCreated() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<UserDto>(responseBody) shouldBe userDto.atUtc()

            verify(passwordEncoder).encode(requestDto.password)
            verify(service).create(user.pseudo, user.email, bcryptPassword)
        }
    }

    private fun Cookie?.shouldBeAuthenticationCookie(value: String, maxAge: Int) {
        shouldNotBeNull()
        domain shouldBe props.auth.cookie.domain
        secure shouldBe props.auth.cookie.secure
        isHttpOnly shouldBe props.auth.cookie.httpOnly
        this.maxAge shouldBe maxAge
        this.value shouldBe value
    }

    private fun UserDto.atUtc() = copy(creationDate = creationDate.withOffsetSameInstant(ZoneOffset.UTC))
}
