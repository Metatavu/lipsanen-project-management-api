package fi.metatavu.lipsanen.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.api.model.ChangeProposalStatus
import fi.metatavu.lipsanen.api.spec.ChangeProposalsApi
import fi.metatavu.lipsanen.exceptions.TaskOutsideMilestoneException
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.tasks.TaskController
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.*

/**
 * Change proposals API implementation
 */
@RequestScoped
@WithSession
class ChangeProposalsApiImpl : ChangeProposalsApi, AbstractApi() {

    @Inject
    lateinit var proposalController: ChangeProposalController

    @Inject
    lateinit var changeProposalTranslator: ChangeProposalTranslator

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listChangeProposals(
        projectId: UUID?,
        milestoneId: UUID?,
        taskId: UUID?,
        first: Int?,
        max: Int?
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        if (projectId == null && (milestoneId != null || taskId != null)) {
            return@withCoroutineScope createBadRequest("Project id cannot be used with milestone or task id")
        }

        val projectFilter = if (projectId != null) {
            val (project, errorResponse) = getProjectAccessRights(projectId, userId)
            if (errorResponse != null) return@withCoroutineScope errorResponse
            listOf(project!!)
        } else {
            if (isAdmin()) {
                null
            } else {
                val user = userController.findUserByKeycloakId(userId) ?: return@withCoroutineScope createInternalServerError("Failed to find a user")
                projectController.listProjectsForUser(user).first
            }
        }

        val taskFilter = if (taskId != null) {
            taskController.find(taskId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK, taskId)
            )
        } else null

        val milestoneFilter = if (milestoneId != null) {
            milestoneController.find(milestoneId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(MILESTONE, milestoneId)
            )
        } else null

        val (changeProposals, count) = proposalController.listChangeProposals(
            projectFilter = projectFilter,
            milestoneFilter = milestoneFilter,
            taskFilter = taskFilter,
            first = first,
            max = max
        )
        createOk(changeProposalTranslator.translate(changeProposals), count)
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun createChangeProposal(
        changeProposal: ChangeProposal
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val task = taskController.find(changeProposal.taskId) ?: return@withCoroutineScope createBadRequest(
            createNotFoundMessage(TASK, changeProposal.taskId)
        )

        getProjectAccessRights(task.milestone.project, userId).second?.let { return@withCoroutineScope it }

        val createdProposal = proposalController.create(task, changeProposal, userId)
        createOk(changeProposalTranslator.translate(createdProposal))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findChangeProposal(
        changeProposalId: UUID
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val proposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )

        getProjectAccessRights(proposal.task.milestone.project, userId).second?.let { return@withCoroutineScope it }
        createOk(changeProposalTranslator.translate(proposal))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateChangeProposal(
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val foundProposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        getProjectAccessRights(foundProposal.task.milestone.project, userId).second?.let { return@withCoroutineScope it }

        if (foundProposal.status != ChangeProposalStatus.PENDING) {
            return@withCoroutineScope createBadRequest("Only pending proposals can be updated")
        }

        if (changeProposal.taskId != foundProposal.task.id) {
            return@withCoroutineScope createBadRequest("Proposal cannot be reassigned to other task")
        }

        hasProposalEditingRights(userId, foundProposal)?.let { return@withCoroutineScope it }

        if (changeProposal.status != foundProposal.status && !isAdmin()) {
            return@withCoroutineScope createForbidden("Status can be changed only by admin")
        }
        try {
            val updatedProposal = proposalController.update(foundProposal, changeProposal, userId)
            createOk(changeProposalTranslator.translate(updatedProposal))
        } catch (e: TaskOutsideMilestoneException) {
            Panache.currentTransaction().awaitSuspending().markForRollback()
            return@withCoroutineScope createBadRequest(e.message!!)
        }

    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteChangeProposal(
        changeProposalId: UUID
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val proposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        getProjectAccessRights(proposal.task.milestone.project, userId).second?.let { return@withCoroutineScope it }

        hasProposalEditingRights(userId, proposal)?.let { return@withCoroutineScope it }

        proposalController.delete(proposal)

        createNoContent()
    }

    /**
     * Checks if the user has rights to edit the proposal (assuming user already has access to the project)
     *
     * @param userId user id
     * @param proposal proposal
     * @return response if the user does not have rights, null otherwise
     */
    private fun hasProposalEditingRights(
        userId: UUID,
        proposal: ChangeProposalEntity
    ): Response? {
        if (!isAdmin() && !isProjectOwner() && proposal.creatorId != userId) {
            return createForbidden(FORBIDDEN)
        }

        return null
    }
}