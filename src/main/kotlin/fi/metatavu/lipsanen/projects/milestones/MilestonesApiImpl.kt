package fi.metatavu.lipsanen.projects.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.api.spec.ProjectMilestonesApi
import fi.metatavu.lipsanen.projects.ProjectController
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

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun listProjectMilestones(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val project = projectController.findProject(projectId) ?: return@async createNotFound(
            createNotFoundMessage(PROJECT, projectId)
        )

        if (!isAdmin() && !projectController.hasAccessToProject(project, userId)) {
            return@async createForbidden(NO_PROJECT_RIGHTS)
        }

        val milestones = milestoneController.list(project)
        createOk(milestoneTranslator.translate(milestones))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createProjectMilestone(projectId: UUID, milestone: Milestone): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val project = projectController.findProject(projectId) ?: return@async createNotFound(
                createNotFoundMessage(PROJECT, projectId)
            )

            if (!projectController.hasAccessToProject(project, userId)) {
                return@async createForbidden(NO_PROJECT_RIGHTS)
            }
            if (!isAdmin() && !projectController.isInPlanningStage(project)) {
                return@async createBadRequest(WRONG_PROJECT_STAGE)
            }

            if(milestone.endDate.isBefore(milestone.startDate)) {
                return@async createBadRequest("End date cannot be before start date")
            }

            val createdMilestone = milestoneController.create(
                project = project,
                milestone = milestone,
                userId = userId
            )
            createOk(milestoneTranslator.translate(createdMilestone))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME)
    override fun findProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val project = projectController.findProject(projectId) ?: return@async createNotFound(
                createNotFoundMessage(PROJECT, projectId)
            )
            if (!isAdmin() && !projectController.hasAccessToProject(project, userId)) { //if it is just a user without access to project
                return@async createForbidden(NO_PROJECT_RIGHTS)
            }
            val milestone = milestoneController.find(project, milestoneId) ?: return@async createNotFound(
                createNotFoundMessage(MILESTONE, milestoneId)
            )

            if (!milestoneController.partOfSameProject(project, milestone)) {
                return@async createNotFound(createNotFoundMessage(MILESTONE, milestoneId))
            }

            createOk(milestoneTranslator.translate(milestone))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun updateProjectMilestone(projectId: UUID, milestoneId: UUID, milestone: Milestone): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val project = projectController.findProject(projectId) ?: return@async createNotFound(
                createNotFoundMessage(PROJECT, projectId)
            )
            if (!projectController.hasAccessToProject(project, userId)) {
                return@async createForbidden(NO_PROJECT_RIGHTS)
            }

            if (!isAdmin() && !projectController.isInPlanningStage(project)) {
                return@async createBadRequest(WRONG_PROJECT_STAGE)
            }

            val foundMilestone = milestoneController.find(project, milestoneId) ?: return@async createNotFound(
                createNotFoundMessage(MILESTONE, milestoneId)
            )

            if (!milestoneController.partOfSameProject(project, foundMilestone)) {
                return@async createNotFound(createNotFoundMessage(MILESTONE, milestoneId))
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
                userId = userId
            )
            createOk(milestoneTranslator.translate(updatedMilestone))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val project = projectController.findProject(projectId) ?: return@async createNotFound(
                createNotFoundMessage(PROJECT, projectId)
            )
            val milestone = milestoneController.find(project, milestoneId) ?: return@async createNotFound(
                createNotFoundMessage(MILESTONE, milestoneId)
            )
            if (!projectController.hasAccessToProject(project, userId)) {
                return@async createForbidden(NO_PROJECT_RIGHTS)
            }

            if (!isAdmin() && !projectController.isInPlanningStage(project)) {
                return@async createBadRequest(WRONG_PROJECT_STAGE)
            }

            milestoneController.delete(milestone)
            createNoContent()
        }.asUni()

}