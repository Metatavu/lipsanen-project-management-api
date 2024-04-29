package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.ProjectMilestonesApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.Milestone
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test builder resource for milestone API
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class MilestoneTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Milestone, ProjectMilestonesApi>(testBuilder, apiClient) {

    private val milestoneToProjectId = mutableMapOf<UUID, UUID>()

    override fun clean(p0: Milestone?) {
        p0?.let { api.deleteProjectMilestone(milestoneToProjectId[it.id]!!, it.id!!) }
    }

    override fun getApi(): ProjectMilestonesApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return ProjectMilestonesApi(ApiTestSettings.apiBasePath)
    }

    fun create(projectId: UUID, milestone: Milestone): Milestone {
        val created = addClosable(api.createProjectMilestone(projectId, milestone))
        milestoneToProjectId[created.id!!] = projectId
        return created
    }

    fun create(projectId: UUID): Milestone {
        return create(
            projectId, Milestone(
                name = "Milestone",
                startDate = "2022-01-01",
                endDate = "2022-01-31"
            )
        )
    }

    fun findProjectMilestone(projectId: UUID, projectMilestoneId: UUID): Milestone {
        return api.findProjectMilestone(projectId, projectMilestoneId)
    }

    fun listProjectMilestones(projectId: UUID): Array<Milestone> {
        return api.listProjectMilestones(projectId)
    }

    fun updateProjectMilestone(projectId: UUID, projectMilestoneId: UUID, milestone: Milestone): Milestone {
        return api.updateProjectMilestone(projectId, projectMilestoneId, milestone)
    }

    fun deleteProjectMilestone(projectId: UUID, projectMilestoneId: UUID) {
        api.deleteProjectMilestone(projectId, projectMilestoneId)
        removeCloseable { closable: Any ->
            if (closable !is Milestone) {
                return@removeCloseable false
            }

            closable.id == projectMilestoneId
        }
    }

    fun assertCreateFail(
        expectedStatus: Int,
        projectId: UUID,
        milestone: Milestone
    ) {
        try {
            api.createProjectMilestone(projectId, milestone)
            fail(String.format("Expected create to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertFindFail(
        expectedStatus: Int,
        projectId: UUID,
        projectMilestoneId: UUID
    ) {
        try {
            api.findProjectMilestone(projectId, projectMilestoneId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID
    ) {
        try {
            api.listProjectMilestones(projectId)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        projectMilestoneId: UUID,
        milestone: Milestone? = null
    ) {
        try {
            api.updateProjectMilestone(
                projectId, projectMilestoneId, milestone ?: Milestone(
                    name = "Updated milestone",
                    startDate = "2022-02-01",
                    endDate = "2022-02-28"
                )
            )
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        projectId: UUID,
        projectMilestoneId: UUID
    ) {
        try {
            api.deleteProjectMilestone(projectId, projectMilestoneId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }
}