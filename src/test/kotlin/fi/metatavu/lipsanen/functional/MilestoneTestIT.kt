package fi.metatavu.lipsanen.functional

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.invalid.InvalidValueTestScenarioBody
import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.invalid.providers.SimpleInvalidValueProvider
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.*
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for Project Milestones
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class MilestoneTestIT : AbstractFunctionalTest() {

    @Test
    fun listMilestones() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create(Project("Project 1", status = ProjectStatus.INITIATION))
        val project2 = tb.admin.project.create(Project("Project 2", status = ProjectStatus.INITIATION))
        val projectOwnerUser = tb.admin.user.create("project-owner", UserRole.PROJECT_OWNER, project1.id)

        val milestoneLater = tb.admin.milestone.create(
            project1.id!!,
            Milestone(name = "Milestone 1", startDate = "2023-01-01", endDate = "2023-01-31", originalStartDate = "2023-01-02", originalEndDate = "2023-01-30")
        )
        val milestoneEarlier = tb.admin.milestone.create(
            project1.id,
            Milestone(name = "Milestone 2", startDate = "2022-02-01", endDate = "2022-02-28", originalStartDate = "2022-02-02", originalEndDate = "2022-02-27")
        )
        tb.admin.milestone.create(project2.id!!)

        val milestones = tb.admin.milestone.listProjectMilestones(project1.id)
        assertNotNull(milestones)
        assertEquals(2, milestones.size)
        assertEquals(milestoneEarlier.id, milestones[0].id)
        assertEquals(milestoneLater.id, milestones[1].id)
        val milestones2 = tb.admin.milestone.listProjectMilestones(project2.id)
        assertNotNull(milestones2)
        assertEquals(1, milestones2.size)

        // access rights
        tb.getUser(projectOwnerUser.email).milestone.assertListFail(403, project2.id)
        val milestones3 = tb.getUser(projectOwnerUser.email).milestone.listProjectMilestones(project1.id)
        assertEquals(2, milestones3.size)
    }

    @Test
    fun createMilestone() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestoneData = Milestone(
            name = "Milestone",
            startDate = "2022-01-01",
            endDate = "2022-01-31",
            originalStartDate = "2022-01-02",
            originalEndDate = "2022-01-30"
        )
        val milestone = tb.admin.milestone.create(
            projectId = project.id!!,
            milestone = milestoneData
        )

        assertNotNull(milestone)
        assertEquals(milestoneData.name, milestone.name)
        assertEquals(milestoneData.startDate, milestone.startDate)
        assertEquals(milestoneData.endDate, milestone.endDate)
        assertEquals(milestoneData.originalStartDate, milestone.originalStartDate)
        assertEquals(milestoneData.originalEndDate, milestone.originalEndDate)
        assertEquals(milestoneData.originalEndDate, milestone.originalEndDate)
        assertEquals(0, milestone.estimatedReadiness)
        assertNotNull(milestone.id)
        assertNotNull(milestone.metadata)
    }

    @Test
    fun createMilestoneFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create(Project("Project 1", status = ProjectStatus.INITIATION))
        tb.admin.project.updateProject(project.id!!, project.copy(status = ProjectStatus.COMPLETION))
        val milestoneData = Milestone(
            name = "Milestone",
            startDate = "2022-01-01",
            endDate = "2022-01-31",
            originalStartDate = "2022-01-02",
            originalEndDate = "2022-01-30"
        )
        // access rights
        tb.user.milestone.assertCreateFail(403, project.id, milestoneData)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones",
            method = Method.POST,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(milestoneData)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    values = listOf(
                        SimpleInvalidValueProvider(
                            jacksonObjectMapper().writeValueAsString(
                                Milestone(
                                    name = "Milestone",
                                    startDate = "2023-01-01",
                                    endDate = "2022-01-31",
                                    originalStartDate = "2022-01-02",
                                    originalEndDate = "2022-01-30"
                                )
                            )
                        )
                    ),
                    expectedStatus = 400
                )
            )
            .build()
            .test()

    }

    @Test
    fun findMilestone() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(project.id!!)

        val foundMilestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id!!)
        assertNotNull(foundMilestone)
        assertEquals(milestone.id, foundMilestone.id)
    }

    @Test
    fun findMilestoneFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(project.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = project.id.toString(),
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = milestone.id.toString(),
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    /**
     * Tests that when creating tasks after or before the milestone, the milestone's start and end dates are extended
     */
    @Test
    fun extendMilestoneToTasks() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create(Project("Project 1", status = ProjectStatus.INITIATION))
        val milestone = tb.admin.milestone.create(project.id!!,
            Milestone(
                name = "Milestone",
                startDate = "2022-01-01",
                endDate = "2022-01-31",
                originalStartDate = "2022-01-01",
                originalEndDate = "2022-01-31"
            )
        )

        val earlierAndLaterTask = Task(
            name = "Task 1",
            startDate = "2021-12-01",
            endDate = "2022-02-01",
            status = fi.metatavu.lipsanen.test.client.models.TaskStatus.NOT_STARTED,
            milestoneId = milestone.id!!
        )

        val earlierTask = tb.admin.task.create(earlierAndLaterTask)
        val updatedMilestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id)
        assertEquals(earlierTask.startDate, updatedMilestone.startDate)
        assertEquals(earlierTask.endDate, updatedMilestone.endDate)

        // Tests moving
        val updatedTask = tb.admin.task.update(earlierTask.id!!, earlierTask.copy(
            startDate = "2021-11-01",
            endDate = "2022-03-01"
        ))
        val updatedMilestone2 = tb.admin.milestone.findProjectMilestone(project.id, milestone.id)
        assertEquals(updatedTask.startDate, updatedMilestone2.startDate)
        assertEquals(updatedTask.endDate, updatedMilestone2.endDate)
    }


    @Test
    fun updateMilestone() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create(Project("Project 1", status = ProjectStatus.INITIATION))
        val milestone = tb.admin.milestone.create(project.id!!)

        val updateData = Milestone(
            name = "Updated milestone",
            startDate = "2022-02-01",
            endDate = "2022-02-28",
            originalStartDate = "2022-01-01",
            originalEndDate = "2022-01-28",
            estimatedReadiness = 20
        )
        val updatedMilestone = tb.admin.milestone.updateProjectMilestone(
            projectId = project.id,
            projectMilestoneId = milestone.id!!,
            milestone = updateData
        )

        assertNotNull(updatedMilestone)
        assertEquals(updateData.name, updatedMilestone.name)
        assertEquals(updateData.startDate, updatedMilestone.startDate)
        assertEquals(updateData.endDate, updatedMilestone.endDate)
        assertEquals(updateData.originalStartDate, updatedMilestone.originalStartDate)
        assertEquals(updateData.originalEndDate, updatedMilestone.originalEndDate)
    }

    /**
     * Tests that milestone readiness is set and updated based on its tasks
     */
    @Test
    fun updateMilestoneReadiness() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create(Project("Project 1", status = ProjectStatus.INITIATION))
        var milestone = tb.admin.milestone.create(project.id!!,
            Milestone(
                name = "Milestone",
                startDate = "2022-01-01",
                endDate = "2022-01-31",
                originalStartDate = "2022-01-01",
                originalEndDate = "2022-01-31"
            )
        )

        assertEquals(0, milestone.estimatedReadiness)

        var task1 = tb.admin.task.create(
            Task(
                name = "Task 1",
                startDate = "2022-01-02",
                endDate = "2022-01-10",
                status = fi.metatavu.lipsanen.test.client.models.TaskStatus.NOT_STARTED,
                milestoneId = milestone.id!!,
                estimatedReadiness = 20
            )
        )

        milestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id!!)
        assertEquals(20, milestone.estimatedReadiness)

        var task2 = tb.admin.task.create(
            Task(
                name = "Task 2",
                startDate = "2022-01-05",
                endDate = "2022-01-20",
                status = fi.metatavu.lipsanen.test.client.models.TaskStatus.NOT_STARTED,
                milestoneId = milestone.id!!,
                estimatedReadiness = 80
            )
        )

        milestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id!!)
        assertEquals((80+20) / 2, milestone.estimatedReadiness)

        task1 = tb.admin.task.update(task1.id!!, task1.copy(estimatedReadiness = 40))
        milestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id!!)
        assertEquals((40+80) / 2, milestone.estimatedReadiness)

        tb.admin.task.delete(task2.id!!)
        milestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id!!)
        assertEquals(task1.estimatedReadiness, milestone.estimatedReadiness)

        task2 = tb.admin.task.update(task1.id!!, task1.copy(status = TaskStatus.DONE))
        milestone = tb.admin.milestone.findProjectMilestone(project.id, milestone.id!!)
        assertEquals(100, task2.estimatedReadiness)
        assertEquals(task2.estimatedReadiness, milestone.estimatedReadiness)
    }

    @Test
    fun updateMilestoneFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create(Project("Project 1", status = ProjectStatus.INITIATION))
        val milestone = tb.admin.milestone.create(project.id!!,
            Milestone(
                name = "Milestone",
                startDate = "2022-01-01",
                endDate = "2022-01-31",
                originalStartDate = "2022-01-01",
                originalEndDate = "2022-01-31"
            )
        )

        tb.admin.task.create(
            Task(
                name = "Task 1",
                startDate = "2022-01-02",
                endDate = "2022-01-10",
                status = fi.metatavu.lipsanen.test.client.models.TaskStatus.NOT_STARTED,
                milestoneId = milestone.id!!
            )
        )
        tb.admin.task.create(
            Task(
                name = "Task 1",
                startDate = "2022-01-05",
                endDate = "2022-01-20",
                status = fi.metatavu.lipsanen.test.client.models.TaskStatus.NOT_STARTED,
                milestoneId = milestone.id
            )
        )

        tb.admin.milestone.assertUpdateFail(400, project.id, milestone.id, milestone.copy(
            startDate = "2022-01-11"    //this new start date is after the end date of the first task
        ))
        tb.admin.milestone.assertUpdateFail(400, project.id, milestone.id, milestone.copy(
            endDate = "2022-01-04"      //this new end date is before the start date of the second task
        ))

        // access rights
        tb.user.milestone.assertUpdateFail(403, project.id, milestone.id)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(milestone)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = project.id.toString(),
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = milestone.id.toString(),
                    expectedStatus = 404
                )
            )
            .build()
            .test()

        // cannot update due to project status
        tb.admin.project.updateProject(project.id, Project("Updated project", status = ProjectStatus.COMPLETION))
        tb.admin.milestone.assertUpdateFail(400, project.id, milestone.id)

        tb.admin.project.updateProject(project.id, Project("Updated project", status = ProjectStatus.INITIATION))
        return@use
    }

    @Test
    fun deleteMilestone() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(project.id!!)

        tb.admin.milestone.deleteProjectMilestone(project.id, milestone.id!!)
        val listProjectMilestones = tb.admin.milestone.listProjectMilestones(project.id)
        assertEquals(0, listProjectMilestones.size)
    }

    @Test
    fun deleteMilestoneFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(project.id!!)
        val task = tb.admin.task.create(milestoneId = milestone.id!!)

        // access rights
        tb.user.milestone.assertDeleteFail(403, project.id, milestone.id!!)

        // cannot delete due to tasks present
        tb.admin.milestone.assertDeleteFail(400, project.id, milestone.id)
        tb.admin.task.delete(task.id!!)
        tb.admin.milestone.deleteProjectMilestone(project.id, milestone.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = project.id.toString(),
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = milestone.id.toString(),
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

}