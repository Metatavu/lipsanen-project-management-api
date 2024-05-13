package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.api.spec.UsersApi
import fi.metatavu.lipsanen.companies.CompanyController
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class UsersApiImpl: UsersApi, AbstractApi() {

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var userTranslator: UserTranslator

    @Inject
    lateinit var companyController: CompanyController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    override fun listUsers(companyId: UUID?, first: Int?, max: Int?, includeRoles: Boolean?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val ( users, count ) = userController.listUsers(companyId, first, max)
        createOk(users.map { userTranslator.translate(it, includeRoles) }, count.toLong())
    }.asUni()

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    override fun createUser(user: User): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val existingUsers = userController.countUserByEmail(user.email)
        if (existingUsers > 0) {
            return@async createConflict("User with given email ${user.email} already exists!")
        }
        val keycloakGroupIds = user.projectIds?.map {
            projectController.findProject(it)?.keycloakGroupId ?: return@async createNotFound(createNotFoundMessage(PROJECT, it))
        }
        user.companyId?.let { companyController.find(it) ?: return@async createNotFound(createNotFoundMessage(COMPANY, it)) }
        val createdUser = userController.createUser(user, keycloakGroupIds) ?: return@async createInternalServerError("Failed to create user")
        createOk(userTranslator.translate(createdUser))
    }.asUni()

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    override fun findUser(userId: UUID, includeRoles: Boolean?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundUser = userController.findUser(userId) ?: return@async createNotFound(createNotFoundMessage(USER, userId))
        createOk(userTranslator.translate(foundUser, includeRoles))
    }.asUni()

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    override fun updateUser(userId: UUID, user: User): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val existingUser = userController.findUser(userId) ?: return@async createNotFound(createNotFoundMessage(USER, userId))
        val keycloakGroupIds = user.projectIds?.map {
            projectController.findProject(it)?.keycloakGroupId ?: return@async createNotFound(createNotFoundMessage(PROJECT, it))
        }
        user.companyId?.let { companyController.find(it) ?: return@async createNotFound(createNotFoundMessage(COMPANY, it)) }
        val updatedUser = userController.updateUser(
            userId = userId,
            existingUser = existingUser,
            updateData = user,
            updateGroups = keycloakGroupIds
        ) ?: return@async createInternalServerError("Failed to update user")
        createOk(userTranslator.translate(updatedUser))
    }.asUni()

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    override fun deleteUser(userId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        userController.findUser(userId) ?: return@async createNotFound(createNotFoundMessage(USER, userId))
        userController.deleteUser(userId)
        createNoContent()
    }.asUni()
}