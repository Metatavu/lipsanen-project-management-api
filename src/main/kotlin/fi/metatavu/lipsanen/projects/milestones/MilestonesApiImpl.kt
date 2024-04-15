package fi.metatavu.lipsanen.projects.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.api.spec.ProjectMilestonesApi
import fi.metatavu.lipsanen.rest.AbstractApi
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.core.Response
import java.util.*

class MilestonesApiImpl: ProjectMilestonesApi, AbstractApi() {
    override fun createProjectMilestone(projectId: UUID, milestone: Milestone): Uni<Response> {
        TODO("Not yet implemented")
    }

    override fun deleteProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> {
        TODO("Not yet implemented")
    }

    override fun findProjectMilestone(projectId: UUID, milestoneId: UUID): Uni<Response> {
        TODO("Not yet implemented")
    }

    override fun listProjectMilestones(projectId: UUID): Uni<Response> {
        TODO("Not yet implemented")
    }

    override fun updateProjectMilestone(projectId: UUID, milestoneId: UUID, milestone: Milestone): Uni<Response> {
        TODO("Not yet implemented")
    }
}