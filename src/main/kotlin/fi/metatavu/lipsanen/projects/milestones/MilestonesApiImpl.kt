package fi.metatavu.lipsanen.projects.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.api.spec.ProjectMilestonesApi
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
    override fun listProjectMilestones(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val ( project, response ) = getProjectAccessRights(projectId, userId)
        response?.let { return@async response}

        val milestones = milestoneController.list(project!!)
        createOk(milestoneTranslator.translate(milestones))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun createProjectMilestone(projectId: UUID, milestone: Milestone): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val ( project, response ) = getProjectAccessRights(projectId, userId)
            response?.let { return@async response }

            if(milestone.endDate.isBefore(milestone.startDate)) {
                return@async createBadRequest("End date cannot be before start date")
            }

            val createdMilestone = milestoneController.create(
                project = project!!,
                milestone = milestone,
                userId = userId
            )
            createOk(milestoneTranslator.translate(createdMilestone))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val ( projectMilestone, response ) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            response?.let { return@async response}

            createOk(milestoneTranslator.translate(projectMilestone!!.first))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateProjectMilestone(projectId: UUID, milestoneId: UUID, milestone: Milestone): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val ( projectMilestone, response ) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            response?.let { return@async response}
            val ( foundMilestone, foundProject ) = projectMilestone!!

            val isProjectPlanningStage = projectController.isInPlanningStage(foundProject)

            if (!isAdmin() && !isProjectOwner() && isProjectPlanningStage) {
                return@async createBadRequest(INVALID_PROJECT_STATE)
            }

            if (milestone.endDate.isBefore(milestone.startDate)) {
                return@async createBadRequest("End date cannot be before start date")
            }

            val milestoneTasks = taskController.list(foundMilestone)
            milestoneController.checkForUpdateRestrictions(existingMilestone = foundMilestone, newMilestone = milestone, tasks = milestoneTasks)
                ?.let { return@async createBadRequest(it)}
            val updatedMilestone = milestoneController.update(
                existingMilestone = foundMilestone,
                milestoneTasks = milestoneTasks,
                updateData = milestone,
                isProjectPlanningStage = isProjectPlanningStage,
                userId = userId
            )
            createOk(milestoneTranslator.translate(updatedMilestone))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val ( projectMilestone, response ) = getProjectMilestoneAccessRights(projectId, milestoneId, userId)
            response?.let { return@async response}
            val ( foundMilestone, _ ) = projectMilestone!!

            milestoneController.delete(foundMilestone)
            createNoContent()
        }.asUni()

}