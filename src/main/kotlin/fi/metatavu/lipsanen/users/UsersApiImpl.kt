package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.api.spec.UsersApi
import fi.metatavu.lipsanen.companies.CompanyController
import fi.metatavu.lipsanen.positions.JobPositionController
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskAssigneeRepository
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

/**
 * Users API implementation
 */
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
    lateinit var taskController: TaskController

    @Inject
    lateinit var taskAssigneeRepository: TaskAssigneeRepository

    @Inject
    lateinit var jobPositionController: JobPositionController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME, UserRole.ADMIN.NAME)
    override fun listUsers(companyId: UUID?, keycloakId: UUID?, projectId: UUID?, first: Int?, max: Int?, includeRoles: Boolean?): Uni<Response> =withCoroutineScope {
        val companyFilter = if (companyId != null) {
            companyController.find(companyId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(COMPANY, companyId))
        } else null
        val projectFilter = if (projectId != null) {
            projectController.findProject(projectId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(PROJECT, projectId))
        } else null

        val ( users, count ) = userController.listUsers(companyFilter, projectFilter, keycloakId, first, max)
        createOk(users.map { userTranslator.translate(it, includeRoles) }, count)
    }

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    @WithTransaction
    override fun createUser(user: User): Uni<Response> =withCoroutineScope {
        val existingUsers = userController.countUserByEmail(user.email)
        if (existingUsers > 0) {
            return@withCoroutineScope createConflict("User with given email ${user.email} already exists!")
        }
        val projects = user.projectIds?.map {
            projectController.findProject(it) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(PROJECT, it))
        }
        val company = user.companyId?.let { companyController.find(it) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(COMPANY, it)) }
        val jobPosition = if (user.jobPositionId != null) {
            jobPositionController.findJobPosition(user.jobPositionId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(JOB_POSITION, user.jobPositionId))
        } else null

        val createdUser = userController.createUser(user, company, jobPosition, projects) ?: return@withCoroutineScope createInternalServerError("Failed to create user")
        createOk(userTranslator.translate(createdUser))
    }

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME, UserRole.ADMIN.NAME)
    override fun findUser(userId: UUID, includeRoles: Boolean?): Uni<Response> =withCoroutineScope {
        val foundUser = userController.findUser(userId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(USER, userId))
        val foundUserRepresentation = userController.findKeycloakUser(foundUser.keycloakId) ?: return@withCoroutineScope createInternalServerError("Failed to find user")
        createOk(userTranslator.translate(
            UserFullRepresentation(
            userEntity = foundUser,
            userRepresentation = foundUserRepresentation
        ), includeRoles))
    }

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    @WithTransaction
    override fun updateUser(userId: UUID, user: User): Uni<Response> =withCoroutineScope {
        val existingUser = userController.findUser(userId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(USER, userId))
        val projects = user.projectIds?.map {
            projectController.findProject(it) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(PROJECT, it))
        }
        val company = user.companyId?.let { companyController.find(it) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(COMPANY, it)) }
        val jobPosition = if (user.jobPositionId != existingUser.jobPosition?.id) {
            if (user.jobPositionId != null) {
                jobPositionController.findJobPosition(user.jobPositionId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(JOB_POSITION, user.jobPositionId))
            } else null
        } else existingUser.jobPosition

        val updatedUser = userController.updateUser(
            existingUser = existingUser,
            updateData = user,
            projects = projects,
            company = company,
            jobPosition = jobPosition
        ) ?: return@withCoroutineScope createInternalServerError("Failed to update user")
        createOk(userTranslator.translate(updatedUser))
    }

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME)
    @WithTransaction
    override fun deleteUser(userId: UUID): Uni<Response> =withCoroutineScope {
        val user = userController.findUser(userId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(USER, userId))

        val tasksInProgressWithUserAsAssignee = taskAssigneeRepository.listByAssignee(user = user).map { it.task }.filter { it.status == TaskStatus.IN_PROGRESS }
        if (tasksInProgressWithUserAsAssignee.isNotEmpty()) {
            return@withCoroutineScope createConflict("User is assigned to tasks that are in progress: ${tasksInProgressWithUserAsAssignee.joinToString { it.id.toString() }}")
        }
        val tasksInProgressWithUserAsDependent = taskController.list(dependentUser = user).filter { it.status == TaskStatus.IN_PROGRESS }
        if (tasksInProgressWithUserAsDependent.isNotEmpty()) {
            return@withCoroutineScope createConflict("User is dependent in tasks that are in progress: ${tasksInProgressWithUserAsDependent.joinToString { it.id.toString() }}")
        }
        userController.deleteUser(user)
        createNoContent()
    }
}