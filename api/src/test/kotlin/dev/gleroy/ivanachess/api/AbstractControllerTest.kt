package dev.gleroy.ivanachess.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dev.gleroy.ivanachess.api.security.AuthenticationService
import dev.gleroy.ivanachess.api.security.Jwt
import dev.gleroy.ivanachess.api.security.UserDetailsAdapter
import dev.gleroy.ivanachess.api.user.User
import dev.gleroy.ivanachess.dto.ErrorDto
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.servlet.http.Cookie

internal abstract class AbstractControllerTest {
    protected val simpleUser = User(
        pseudo = "simple",
        email = "simple@ivanachess.loc",
        creationDate = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
        bcryptPassword = "\$2y\$12\$0jk/kpEJfuuVJShpgeZhYuTYAVj5sau2W2qtFTMMIwPctmLWVXHSS"
    )

    @MockBean
    protected lateinit var authService: AuthenticationService

    @Autowired
    protected lateinit var props: Properties

    @Autowired
    protected lateinit var mapper: ObjectMapper

    @Autowired
    protected lateinit var mvc: MockMvc

    abstract inner class Paginated {
        @Test
        fun `should return validation_error if page params are negative`() {
            val responseBody = mvc.request(method, path) {
                param(ApiConstants.QueryParams.Page, "-1")
                param(ApiConstants.QueryParams.PageSize, "-1")
            }
                .andDo { print() }
                .andExpect { status { isBadRequest() } }
                .andReturn()
                .response
                .contentAsByteArray
            mapper.readValue<ErrorDto.Validation>(responseBody) shouldBe ErrorDto.Validation(
                errors = setOf(
                    tooLowNumberInvalidParameter(ApiConstants.QueryParams.Page, 1),
                    tooLowNumberInvalidParameter(ApiConstants.QueryParams.PageSize, 1)
                )
            )
        }

        protected abstract val method: HttpMethod
        protected abstract val path: String
    }

    protected fun missingParameterInvalidParameter(parameter: String) = ErrorDto.InvalidParameter(
        parameter = parameter,
        reason = "must not be null"
    )

    protected fun stringSizeInvalidParameter(parameter: String, min: Int, max: Int) = ErrorDto.InvalidParameter(
        parameter = parameter,
        reason = "size must be between $min and $max"
    )

    protected fun tooHighNumberInvalidParameter(parameter: String, max: Int) = ErrorDto.InvalidParameter(
        parameter = parameter,
        reason = "must be less than or equal to $max"
    )

    protected fun tooLowNumberInvalidParameter(parameter: String, min: Int) = ErrorDto.InvalidParameter(
        parameter = parameter,
        reason = "must be greater than or equal to $min"
    )

    protected fun withAuthentication(user: User = simpleUser, invocationsNb: Int = 1, block: (Jwt) -> Unit) {
        val jwt = Jwt(
            pseudo = user.pseudo,
            expirationDate = OffsetDateTime.now().plusDays(1),
            token = "token"
        )
        whenever(authService.parseJwt(jwt.token)).thenReturn(jwt)
        whenever(authService.loadUserByUsername(user.pseudo)).thenReturn(UserDetailsAdapter(user))
        block(jwt)
        verify(authService, times(invocationsNb)).parseJwt(jwt.token)
        verify(authService, times(invocationsNb)).loadUserByUsername(user.pseudo)
    }

    protected fun MockHttpServletRequestDsl.authenticationCookie(jwt: Jwt) {
        cookie(Cookie(props.auth.cookie.name, jwt.token))
    }

    protected fun MockHttpServletRequestDsl.authenticationHeader(jwt: Jwt) {
        header(props.auth.header.name, "${props.auth.header.valuePrefix}${jwt.token}")
    }
}
