package fi.metatavu.lipsanen.users

import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.api.spec.UsersApi
import fi.metatavu.lipsanen.companies.CompanyController
import fi.metatavu.lipsanen.positions.JobPositionController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.tasks.TaskAssigneeRepository
import fi.metatavu.lipsanen.tasks.TaskController
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.*

/**
 * Users API implementation
 */
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
    override fun listUsers(companyId: UUID?, keycloakId: UUID?, projectId: UUID?, jobPositionId: UUID?, first: Int?, max: Int?, includeRoles: Boolean?): Uni<Response> =withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized("Unauthorized")
        val companyFilter = if (companyId != null) {
            companyController.find(companyId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(COMPANY, companyId))
        } else null

        val projectFilter = if (projectId != null) {
            val project = projectController.findProject(projectId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(PROJECT, projectId))
            getProjectAccessRights(projectId, userId).second?.let { return@withCoroutineScope it }
            listOf(project)
        } else {
            if (isAdmin() || isUserManagementAdmin() || isProjectOwner()) {
                null
            } else {
                val user = userController.findUserByKeycloakId(userId) ?: return@withCoroutineScope createInternalServerError("Failed to find user")
                userController.listUserProjects(user).map { it.project }
            }
        }

        val jobPosition = if (jobPositionId != null) {
            jobPositionController.findJobPosition(jobPositionId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(JOB_POSITION, jobPositionId))
        } else null

        val ( users, count ) = userController.listUsers(companyFilter, projectFilter, jobPosition, keycloakId, first, max)
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
    override fun findUser(userId: UUID, includeRoles: Boolean?): Uni<Response> = withCoroutineScope {
        val logggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized("Unauthorized")
        val foundUser = userController.findUser(userId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(USER, userId))

        if (!isUserManagementAdmin() && !isAdmin() && !isProjectOwner()) {
            val currentUser = userController.findUserByKeycloakId(logggedInUserId) ?: return@withCoroutineScope createInternalServerError("Failed to find user")
            val userProjects = userController.listUserProjects(currentUser).map { it.project.id }
            val isInSameProject = userController.listUserProjects(foundUser).map { it.project.id }.intersect(userProjects.toSet()).isNotEmpty()
            if (foundUser.keycloakId != logggedInUserId && !isInSameProject) {
                return@withCoroutineScope createNotFound("Unauthorized")
            }
        }

        val foundUserRepresentation = userController.findKeycloakUser(foundUser.keycloakId) ?: return@withCoroutineScope createInternalServerError("Failed to find user")
        createOk(userTranslator.translate(
            UserFullRepresentation(
            userEntity = foundUser,
            userRepresentation = foundUserRepresentation
        ), includeRoles))
    }

    @RolesAllowed(UserRole.USER_MANAGEMENT_ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateUser(userId: UUID, user: User): Uni<Response> = withCoroutineScope {
        val logggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized("Unauthorized")
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

        val updatedUser = if (isUserManagementAdmin()) {
            userController.updateUser(
                existingUser = existingUser,
                updateData = user,
                projects = projects,
                company = company,
                jobPosition = jobPosition
            ) ?: return@withCoroutineScope createInternalServerError("Failed to update user")
        } else if (isProjectOwner()) {
            val projectOwnerUser = userController.findUserByKeycloakId(logggedInUserId) ?: return@withCoroutineScope createInternalServerError("Failed to find user")
            if (!canAssignToProjects(projectOwnerUser, existingUser, projects?.map { it.id } ?: emptyList())) {
                return@withCoroutineScope createForbidden("Forbidden")
            }
            userController.assignUserToProjects(
                user = existingUser,
                newProjects = projects ?: emptyList(),
            )
            UserFullRepresentation(
                userRepresentation = userController.findKeycloakUser(existingUser.keycloakId)!!,
                userEntity = existingUser
            )
        } else {
            return@withCoroutineScope createForbidden("Forbidden")
        }

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

    /**
     * Checks if project owner can assign user to projects.
     * It can assign users to the projects that he/she is project owner of.
     * He cannot un-assing user from the projects that he/she is not project owner of.
     *
     * @param updatingUser user who is updating the user
     * @param updatableUser user who is being updated
     * @param newUserProjectIds new projects that user is being assigned to
     * @return true if project owner can assign user to projects, false otherwise
     */
    private suspend fun canAssignToProjects(updatingUser: UserEntity, updatableUser: UserEntity, newUserProjectIds: List<UUID>): Boolean {
        val currentUserProjectIds = userController.listUserProjects(updatableUser).map { it.project.id }
        val projectOwnerProjectIds = userController.listUserProjects(updatingUser).map { it.project.id }

        val projectsToAddIds = newUserProjectIds.filter { !currentUserProjectIds.contains(it) }
        val projectsToRemoveIds = currentUserProjectIds.filter { !newUserProjectIds.contains(it) }

        if (projectsToRemoveIds.any { !projectOwnerProjectIds.contains(it) }) {
            return false
        }

        if (projectsToAddIds.any { !projectOwnerProjectIds.contains(it) }) {
            return false
        }

        return true
    }
}