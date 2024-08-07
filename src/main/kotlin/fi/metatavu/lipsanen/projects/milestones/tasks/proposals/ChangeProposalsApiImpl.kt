package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.api.spec.ChangeProposalsApi
import fi.metatavu.lipsanen.exceptions.TaskOutsideMilestoneException
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskTranslator
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    lateinit var changeProposalTranslator: ChangeProposalTranslator

    @Inject
    lateinit var taskTranslator: TaskTranslator

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listChangeProposals(
        projectId: UUID,
        milestoneId: UUID?,
        taskId: UUID?,
        first: Int?,
        max: Int?
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val taskFilter = if (taskId != null) {
            taskController.find(project!!, taskId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(TASK, taskId)
            )
        } else null

        val milestoneFilter = if (milestoneId != null) {
            milestoneController.find(project!!, milestoneId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(MILESTONE, milestoneId)
            )
        } else null

        val (changeProposals, count) = proposalController.listChangeProposals(
            project = project!!,
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
        projectId: UUID,
        changeProposal: ChangeProposal
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val (project, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val task = taskController.find(project!!, changeProposal.taskId) ?: return@withCoroutineScope createBadRequest(
            createNotFoundMessage(TASK, changeProposal.taskId)
        )
        val createdProposal = proposalController.create(task, changeProposal, userId)
        createOk(changeProposalTranslator.translate(createdProposal))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findChangeProposal(
        projectId: UUID,
        changeProposalId: UUID
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val (_, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val proposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        if (proposal.task.milestone.project.id != projectId) {
            return@withCoroutineScope createNotFound(createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId))
        }

        createOk(changeProposalTranslator.translate(proposal))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateChangeProposal(
        projectId: UUID,
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val (_, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val foundProposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        if (changeProposal.taskId != foundProposal.task.id) {
            return@withCoroutineScope createBadRequest("Proposal cannot be reassigned to other task")
        }

        if (foundProposal.task.milestone.project.id != projectId) {
            return@withCoroutineScope createNotFound(createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId))
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
        projectId: UUID,
        changeProposalId: UUID
    ): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val (_, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val proposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        if (proposal.task.milestone.project.id != projectId) {
            return@withCoroutineScope createNotFound(createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId))
        }

        hasProposalEditingRights(userId, proposal)?.let { return@withCoroutineScope it }

        proposalController.delete(proposal)

        createNoContent()
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listChangeProposalTasksPreview(projectId: UUID, changeProposalId: UUID): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val (_, errorResponse) = getProjectAccessRights(projectId, userId)
        if (errorResponse != null) return@withCoroutineScope errorResponse

        val proposal = proposalController.find(changeProposalId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(CHANGE_PROPOSAL, changeProposalId)
        )
        hasProposalEditingRights(userId, proposal)?.let { return@withCoroutineScope it }
        try {
            val tasks = proposalController.listChangeProposalTasksPreview(proposal, userId)
            createOk(taskTranslator.translate(tasks))

        } catch (e: TaskOutsideMilestoneException) {
            return@withCoroutineScope createBadRequest(e.message!!)
        }
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