package dev.gleroy.ivanachess.api

import dev.gleroy.ivanachess.core.*
import org.springframework.stereotype.Service
import java.util.*

/**
 * Default implementation of user service.
 *
 * @param repository User repository.
 */
@Service
class DefaultUserService(
    override val repository: UserRepository
) : AbstractSearchableEntityService<User>(), UserService {
    override fun create(pseudo: String, email: String, bcryptPassword: String, role: User.Role): User {
        if (repository.existsWithPseudo(pseudo)) {
            throw UserPseudoAlreadyUsedException(pseudo).apply { logger.debug(message) }
        }
        if (repository.existsWithEmail(email)) {
            throw UserEmailAlreadyUsedException(email).apply { logger.debug(message) }
        }
        val user = repository.save(
            entity = User(
                pseudo = pseudo,
                email = email,
                bcryptPassword = bcryptPassword
            )
        )
        logger.info("New user '$pseudo' (${user.id}) created")
        return user
    }

    override fun delete(id: UUID) {
        val user = getById(id)
        if (user.role == User.Role.SuperAdmin) {
            throw NotAllowedException("User '${user.pseudo}' (${user.id}) can't be deleted because it is super admin")
                .apply { logger.debug(message) }
        }
        repository.delete(id)
        logger.info("User '${user.pseudo}' (${user.id}) deleted")
    }

    override fun existsWithEmail(email: String, excluding: Set<UUID>) = repository.existsWithEmail(email, excluding)

    override fun existsWithPseudo(pseudo: String, excluding: Set<UUID>) = repository.existsWithPseudo(pseudo, excluding)

    override fun getByEmail(email: String) = repository.fetchByEmail(email)
        ?: throw EntityNotFoundException("User with email '$email' does not exist").apply { logger.debug(message) }

    override fun getByPseudo(pseudo: String) = repository.fetchByPseudo(pseudo)
        ?: throw EntityNotFoundException("User with pseudo '$pseudo' does not exist").apply { logger.debug(message) }

    override fun update(id: UUID, email: String?, bcryptPassword: String?, role: User.Role?): User {
        if (email != null && repository.existsWithEmail(email, setOf(id))) {
            throw UserEmailAlreadyUsedException(email).apply { logger.debug(message) }
        }
        val user = getById(id)
        return repository.save(
            entity = user.copy(
                email = email ?: user.email,
                bcryptPassword = bcryptPassword ?: user.bcryptPassword,
                role = role ?: user.role,
            )
        ).apply { logger.info("User '$pseudo' ($id) updated") }
    }
}
