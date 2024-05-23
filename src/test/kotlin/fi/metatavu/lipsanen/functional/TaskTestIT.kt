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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

/**
 * Tasks API tests
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class TaskTestIT : AbstractFunctionalTest() {

    val userId1 = UUID.fromString("f4c1e6a1-705a-471a-825d-1982b5112ebd")
    val userId2 = UUID.fromString("ef89e98e-6aa3-4511-9b80-ff98bd87fe36")
    val userId3 = UUID.fromString("af6451b7-383c-4771-a0cb-bd16fae402c4")

    @Test
    fun testTaskCreate() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val taskData = Task(
            name = "Task",
            status = TaskStatus.NOT_STARTED,    //always created as not started
            startDate = "2022-01-01",
            endDate = "2022-01-31",
            milestoneId = milestone.id!!,
            assigneeIds = arrayOf(userId2, userId1),
            userRole = UserRole.USER,
            estimatedDuration = "1d",
            estimatedReadiness = "10%",
            attachmentUrls = arrayOf("https://example.com/attachment1", "https://example.com/attachment2"),

        )

        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id, task = taskData)

        assertNotNull(task)
        assertNotNull(task.id)
        assertEquals(taskData.name, task.name)
        assertEquals(taskData.status, task.status)
        assertEquals(taskData.startDate, task.startDate)
        assertEquals(taskData.endDate, task.endDate)
        assertEquals(taskData.milestoneId, task.milestoneId)
        assertEquals(taskData.assigneeIds!!.toList(), task.assigneeIds!!.toList())
        assertEquals(taskData.userRole, task.userRole)
        assertEquals(taskData.estimatedDuration, task.estimatedDuration)
        assertEquals(taskData.estimatedReadiness, task.estimatedReadiness)
        assertEquals(taskData.attachmentUrls!!.toList(), task.attachmentUrls!!.toList())
    }

    @Test
    fun testTaskCreateFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks",
            method = Method.POST,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(task)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = milestone.id
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    values = listOf(
                        Task(
                            name = "Milestone",
                            startDate = "2023-01-01",
                            endDate = "2022-01-31",
                            milestoneId = milestone.id,
                            status = TaskStatus.NOT_STARTED
                        ),
                        Task(       //wrong milestone id
                            name = "Milestone",
                            startDate = "2022-01-01",
                            endDate = "2022-01-31",
                            milestoneId = UUID.randomUUID(),
                            status = TaskStatus.NOT_STARTED
                        ),
                        task.copy(assigneeIds = arrayOf(UUID.randomUUID())), //assignee not found
                    ).map { SimpleInvalidValueProvider(jacksonObjectMapper().writeValueAsString(it)) },
                    expectedStatus = 400
                )
            )
            .build()
            .test()

    }

    @Test
    fun testTaskList() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val project2 = tb.admin.project.create(
            Project(
                name = "Project2",
                status = ProjectStatus.PLANNING
            )
        )
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val milestone2 = tb.admin.milestone.create(projectId = project2.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val tasks = tb.admin.task.list(projectId = project.id, milestoneId = milestone.id)

        assertEquals(1, tasks.size)
        assertEquals(task.id, tasks[0].id)

        val tasks2 = tb.admin.task.list(projectId = project2.id, milestoneId = milestone2.id!!)
        assertEquals(0, tasks2.size)

    }

    @Test
    fun testTaskListFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val project2 = tb.admin.project.create(
            Project(
                name = "Project2",
                status = ProjectStatus.PLANNING
            )
        )
        val milestone = tb.admin.milestone.create(projectId = project.id!!)

        tb.admin.task.create(projectId = project.id!!, milestoneId = milestone.id!!)

        tb.admin.task.assertListFail(
            expectedStatus = 404,
            projectId = project2.id!!,
            milestoneId = milestone.id
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = milestone.id
                )
            )
            .build()
            .test()
    }

    @Test
    fun testTaskFind() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val foundTask = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        assertNotNull(foundTask)
        assertEquals(task.id, foundTask.id)
    }

    @Test
    fun testTaskFindFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = milestone.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = task.id
                )
            )
            .build()
            .test()
    }

    @Test
    fun testTaskUpdate() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(
            projectId = project.id!!, Milestone(
                name = "Milestone",
                startDate = "2022-01-01",
                endDate = "2022-01-31",
                originalStartDate = "2022-01-01",
                originalEndDate = "2022-01-31"
            )
        )
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!, Task(
            name = "Task",
            startDate = "2022-01-01",
            endDate = "2022-01-31",
            status = TaskStatus.NOT_STARTED,
            assigneeIds = arrayOf(userId1, userId2),
            userRole = UserRole.USER,
            estimatedDuration = "1d",
            estimatedReadiness = "10%",
            attachmentUrls = arrayOf("https://example.com/attachment1", "https://example.com/attachment2"),
            milestoneId = milestone.id
        ))
        val taskUpdateData = task.copy(
            name = "Task2",
            startDate = "2022-01-03",
            endDate = "2022-02-01",
            status = TaskStatus.IN_PROGRESS,
            assigneeIds = arrayOf(userId3, userId2),
            userRole = UserRole.ADMIN,
            estimatedDuration = "2d",
            estimatedReadiness = "20%",
            attachmentUrls = arrayOf("https://example.com/attachment1", "https://example.com/attachment3")
        )
        val updatedTask = tb.admin.task.update(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!, taskUpdateData)

        assertNotNull(updatedTask)
        assertEquals(task.id, updatedTask.id)
        assertEquals("Task2", updatedTask.name)
        assertEquals(taskUpdateData.startDate, updatedTask.startDate)
        assertEquals(taskUpdateData.endDate, updatedTask.endDate)

        assertEquals(2, updatedTask.assigneeIds?.size)
        assertEquals(taskUpdateData.assigneeIds!!.toList(), updatedTask.assigneeIds!!.toList())
        assertEquals(taskUpdateData.userRole, updatedTask.userRole)
        assertEquals(taskUpdateData.estimatedDuration, updatedTask.estimatedDuration)
        assertEquals(taskUpdateData.estimatedReadiness, updatedTask.estimatedReadiness)
        assertEquals(2, updatedTask.attachmentUrls?.size)
        assertEquals(taskUpdateData.attachmentUrls!!.toList(), updatedTask.attachmentUrls!!.toList())

        // verify that the end of the milestone extended to the new task length
        val foundMilestone = tb.admin.milestone.findProjectMilestone(projectId = project.id, projectMilestoneId = milestone.id)
        assertEquals(milestone.startDate, foundMilestone.startDate)
        assertEquals(taskUpdateData.endDate, foundMilestone.endDate)

        // Verify that the user cannot be removed
        tb.admin.user.assertDeleteFailStatus(409, userId2)
        tb.admin.user.assertDeleteFailStatus(409, userId3)
    }

    @Test
    fun testTaskUpdateFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()

        val milestoneStart = LocalDate.of(2022, 1, 1)
        val milestoneEnd = LocalDate.of(2022, 1, 31)

        val milestone = tb.admin.milestone.create(
            projectId = project.id!!, Milestone(
                name = "Milestone",
                startDate = milestoneStart.toString(),
                endDate = milestoneEnd.toString(),
                originalStartDate = milestoneStart.toString(),
                originalEndDate = milestoneEnd.toString()
            )
        )

        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val updatedTask =
            tb.admin.task.update(projectId = project.id, milestoneId = milestone.id!!, taskId = task.id!!, task)

        assertNotNull(updatedTask)
        assertEquals(task.id, updatedTask.id)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(task)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = milestone.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = task.id
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    values = listOf(
                        task.copy(
                            milestoneId = UUID.randomUUID()
                        ),

                        ).map { SimpleInvalidValueProvider(jacksonObjectMapper().writeValueAsString(it)) },
                    expectedStatus = 400
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    values = listOf(
                        task.copy(
                            assigneeIds = arrayOf(UUID.randomUUID())
                        )
                    ).map { SimpleInvalidValueProvider(jacksonObjectMapper().writeValueAsString(it)) },
                    expectedStatus = 400
                )
            )
            .build()
            .test()
    }

    @Test
    fun tsetTaskDelete() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        tb.admin.task.delete(projectId = project.id, milestoneId = milestone.id!!, taskId = task.id!!)
    }

    @Test
    fun tsetTaskDeleteFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = milestone.id
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = task.id
                )
            )
            .build()
            .test()
    }
}