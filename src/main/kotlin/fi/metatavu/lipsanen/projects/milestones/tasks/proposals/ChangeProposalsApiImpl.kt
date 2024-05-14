package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.api.spec.ChangeProposalsApi
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
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

/**
 * Change proposals API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class ChangeProposalsApiImpl : ChangeProposalsApi, AbstractApi() {

    @Inject
    lateinit var proposalController: ChangeProposalController

    @Inject
    lateinit var proposalTranslator: ProposalTranslator

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun listChangeProposals(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        first: Int?,
        max: Int?
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )

        val (changeProposals, count) = proposalController.listChangeProposals(task, first, max)
        createOk(proposalTranslator.translate(changeProposals), count)
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createChangeProposal(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposal: ChangeProposal
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (changeProposal.taskProposal.taskId != taskId) {
            return@async createBadRequest("Task id in task proposal does not match the task id in the path")
        }

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )
        val createdProposal = proposalController.create(task, changeProposal, userId)
        createOk(proposalTranslator.translate(createdProposal))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun findChangeProposal(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )
        val proposal = proposalController.find(task, changeProposalId) ?: return@async createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )

        createOk(proposalTranslator.translate(proposal))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    @WithTransaction
    override fun updateChangeProposal(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (changeProposal.taskProposal.taskId != taskId) {
            return@async createBadRequest("Task id in task proposal does not match the task id in the path")
        }

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )
        val foundProposal = proposalController.find(task, changeProposalId) ?: return@async createNotFound(
            createNotFoundMessage(
                CHANGE_PROPOSAL,
                changeProposalId
            )
        )
        hasProposalEditingRights(userId, foundProposal)?.let { return@async it }

        val updatedProposal = proposalController.update(foundProposal, changeProposal, userId)

        createOk(proposalTranslator.translate(updatedProposal))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteChangeProposal(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val (projectMilestone, errorResponse) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
        if (errorResponse != null) return@async errorResponse

        val task = taskController.find(projectMilestone!!.first, taskId) ?: return@async createNotFound(
            createNotFoundMessage(TASK, taskId)
        )
        val proposal = proposalController.find(task, changeProposalId) ?: return@async createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        hasProposalEditingRights(userId, proposal)?.let { return@async it }

        proposalController.delete(proposal)

        createNoContent()
    }.asUni()

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
        if (!isAdmin() && proposal.creatorId != userId) {
            return createForbidden(FORBIDDEN)
        }

        return null
    }
}