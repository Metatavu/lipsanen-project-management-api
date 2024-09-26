package fi.metatavu.lipsanen.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.api.spec.ProjectMilestonesApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.tasks.TaskController
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

@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class MilestonesApiImpl : ProjectMilestonesApi, AbstractApi() {

    @Inject
    lateinit var milestoneTranslator: MilestoneTranslator

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listProjectMilestones(projectId: UUID): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)

        val ( project, response ) = getProjectAccessRights(projectId, userId)
        response?.let { return@withCoroutineScope response}

        val milestones = milestoneController.list(project!!)
        createOk(milestoneTranslator.translate(milestones))
    }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun createProjectMilestone(projectId: UUID, milestone: Milestone): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val ( project, response ) = getProjectAccessRights(projectId, userId)
            response?.let { return@withCoroutineScope response }

            if(milestone.endDate.isBefore(milestone.startDate)) {
                return@withCoroutineScope createBadRequest("End date cannot be before start date")
            }

            val createdMilestone = milestoneController.create(
                project = project!!,
                milestone = milestone,
                userId = userId
            )
            createOk(milestoneTranslator.translate(createdMilestone))
        }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val ( projectMilestone, response ) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            response?.let { return@withCoroutineScope response}

            createOk(milestoneTranslator.translate(projectMilestone!!.first))
        }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateProjectMilestone(projectId: UUID, milestoneId: UUID, milestone: Milestone): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val ( projectMilestone, response ) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            response?.let { return@withCoroutineScope response}
            val ( foundMilestone, foundProject ) = projectMilestone!!

            val isProjectPlanningStage = projectController.isInPlanningStage(foundProject)

            if (!isAdmin() && !isProjectOwner() && isProjectPlanningStage) {
                return@withCoroutineScope createBadRequest(INVALID_PROJECT_STATE)
            }

            if (milestone.endDate.isBefore(milestone.startDate)) {
                return@withCoroutineScope createBadRequest("End date cannot be before start date")
            }

            val milestoneTasks = taskController.list(foundMilestone)
            milestoneController.checkForUpdateRestrictions(existingMilestone = foundMilestone, newMilestone = milestone, tasks = milestoneTasks)
                ?.let { return@withCoroutineScope createBadRequest(it)}
            val updatedMilestone = milestoneController.update(
                existingMilestone = foundMilestone,
                milestoneTasks = milestoneTasks,
                updateData = milestone,
                isProjectPlanningStage = isProjectPlanningStage,
                userId = userId
            )
            createOk(milestoneTranslator.translate(updatedMilestone))
        }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val ( projectMilestone, response ) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            response?.let { return@withCoroutineScope response}
            val ( foundMilestone, _ ) = projectMilestone!!

            milestoneController.delete(foundMilestone)
            createNoContent()
        }

}