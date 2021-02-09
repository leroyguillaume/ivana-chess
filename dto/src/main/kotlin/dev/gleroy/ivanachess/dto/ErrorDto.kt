package dev.gleroy.ivanachess.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Game not found error code.
 */
private const val GameNotFoundCode = "game_not_found"

/**
 * Invalid content type error code.
 */
private const val InvalidContentTypeCode = "invalid_content_type"

/**
 * Invalid move code.
 */
private const val InvalidMoveCode = "invalid_move"

/**
 * Invalid parameter error code.
 */
private const val InvalidParameterCode = "invalid_parameter"

/**
 * Invalid player code.
 */
private const val InvalidPlayerCode = "invalid_player"

/**
 * Invalid request body error code.
 */
private const val InvalidRequestBodyCode = "invalid_request_body"

/**
 * Method not allowed error code.
 */
private const val MethodNotAllowedCode = "method_not_allowed"

/**
 * Not found error code.
 */
private const val NotFoundCode = "not_found"

/**
 * Validation error code.
 */
private const val ValidationErrorCode = "validation_error"

/**
 * Error DTO.
 */
@JsonTypeInfo(
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    use = JsonTypeInfo.Id.NAME,
    property = "code"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ErrorDto.GameNotFound::class, name = GameNotFoundCode),
    JsonSubTypes.Type(value = ErrorDto.InvalidContentType::class, name = InvalidContentTypeCode),
    JsonSubTypes.Type(value = ErrorDto.InvalidMove::class, name = InvalidMoveCode),
    JsonSubTypes.Type(value = ErrorDto.InvalidParameter::class, name = InvalidParameterCode),
    JsonSubTypes.Type(value = ErrorDto.InvalidPlayer::class, name = InvalidPlayerCode),
    JsonSubTypes.Type(value = ErrorDto.InvalidRequestBody::class, name = InvalidRequestBodyCode),
    JsonSubTypes.Type(value = ErrorDto.MethodNotAllowed::class, name = MethodNotAllowedCode),
    JsonSubTypes.Type(value = ErrorDto.NotFound::class, name = NotFoundCode),
    JsonSubTypes.Type(value = ErrorDto.Validation::class, name = ValidationErrorCode),
)
sealed class ErrorDto {
    /**
     * Game not found.
     */
    object GameNotFound : ErrorDto() {
        override val code = GameNotFoundCode
    }

    /**
     * Invalid content type.
     */
    object InvalidContentType : ErrorDto() {
        override val code = InvalidContentTypeCode
    }

    /**
     * Invalid move.
     *
     * @param reason Error message.
     */
    data class InvalidMove(
        val reason: String
    ) : ErrorDto() {
        override val code = InvalidMoveCode
    }

    /**
     * Invalid parameter.
     *
     * @param parameter Parameter name.
     * @param reason Error message.
     */
    data class InvalidParameter(
        val parameter: String,
        val reason: String
    ) : ErrorDto() {
        override val code = InvalidParameterCode
    }

    /**
     * Invalid player.
     */
    object InvalidPlayer : ErrorDto() {
        override val code = InvalidPlayerCode
    }

    /**
     * Invalid request body.
     */
    object InvalidRequestBody : ErrorDto() {
        override val code = InvalidRequestBodyCode
    }

    /**
     * Method not allowed.
     */
    object MethodNotAllowed : ErrorDto() {
        override val code = MethodNotAllowedCode
    }

    /**
     * Not found..
     */
    object NotFound : ErrorDto() {
        override val code = NotFoundCode
    }

    /**
     * Validation error DTO.
     *
     * @param errors Errors.
     */
    data class Validation(
        val errors: Set<ErrorDto>
    ) : ErrorDto() {
        override val code = ValidationErrorCode
    }

    /**
     * Error code.
     */
    abstract val code: String
}