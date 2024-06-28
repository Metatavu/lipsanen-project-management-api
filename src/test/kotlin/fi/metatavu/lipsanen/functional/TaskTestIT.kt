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

    @Test
    fun testTaskCreate() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)

        val admin = tb.admin.user.create("admin1", UserRole.ADMIN)
        val testUser1 = tb.admin.user.create("test0", UserRole.USER)
        val testUser2 = tb.admin.user.create("test1", UserRole.USER)

        val taskData = Task(
            name = "Task",
            status = TaskStatus.NOT_STARTED,    //always created as not started
            startDate = "2022-01-01",
            endDate = "2022-01-31",
            milestoneId = milestone.id!!,
            assigneeIds = arrayOf(testUser1.id!!, testUser2.id!!),
            estimatedDuration = 1.5f,
            estimatedReadiness = 10,
            attachmentUrls = arrayOf("https://example.com/attachment1", "https://example.com/attachment2"),

        )

        val task = tb.getUser("admin1@example.com").task.create(projectId = project.id, milestoneId = milestone.id, task = taskData)

        assertNotNull(task)
        assertNotNull(task.id)
        assertEquals(taskData.name, task.name)
        assertEquals(taskData.status, task.status)
        assertEquals(taskData.startDate, task.startDate)
        assertEquals(taskData.endDate, task.endDate)
        assertEquals(taskData.milestoneId, task.milestoneId)
        assertEquals(taskData.assigneeIds!!.toSet(), task.assigneeIds!!.toSet())
        assertEquals(taskData.userRole, task.userRole)
        assertEquals(taskData.estimatedDuration, task.estimatedDuration)
        assertEquals(taskData.estimatedReadiness, task.estimatedReadiness)
        assertEquals(taskData.attachmentUrls!!.toList(), task.attachmentUrls!!.toList())

        val creator = tb.admin.user.findUser(task.metadata!!.creatorId!!)
        assertEquals(admin.id, creator.id)
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
                            status = TaskStatus.NOT_STARTED,
                        ),
                        Task(       //wrong assignee id
                            name = "Milestone",
                            startDate = "2022-01-01",
                            endDate = "2022-01-31",
                            milestoneId = milestone.id,
                            status = TaskStatus.NOT_STARTED,
                            assigneeIds = arrayOf(UUID.randomUUID())
                        ),
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
        val testUser1 = tb.admin.user.create("test0", UserRole.USER).id!!
        val testUser2 = tb.admin.user.create("test1", UserRole.USER).id!!
        val testUser3 = tb.admin.user.create("test2", UserRole.USER).id!!
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
            assigneeIds = arrayOf(testUser1, testUser2),
            userRole = UserRole.USER,
            estimatedDuration = 1.5f,
            estimatedReadiness = 10,
            attachmentUrls = arrayOf("https://example.com/attachment1", "https://example.com/attachment2"),
            milestoneId = milestone.id
        ))
        val taskUpdateData = task.copy(
            name = "Task2",
            startDate = "2022-01-03",
            endDate = "2022-02-01",
            status = TaskStatus.IN_PROGRESS,
            assigneeIds = arrayOf(testUser2, testUser3),
            userRole = UserRole.ADMIN,
            estimatedDuration = 2.0f,
            estimatedReadiness = 20,
            attachmentUrls = arrayOf("https://example.com/attachment1", "https://example.com/attachment3")
        )
        val updatedTask = tb.admin.task.update(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!, taskUpdateData)

        assertNotNull(updatedTask)
        assertEquals(task.id, updatedTask.id)
        assertEquals("Task2", updatedTask.name)
        assertEquals(taskUpdateData.startDate, updatedTask.startDate)
        assertEquals(taskUpdateData.endDate, updatedTask.endDate)

        assertEquals(2, updatedTask.assigneeIds?.size)
        assertEquals(taskUpdateData.assigneeIds!!.toSet(), updatedTask.assigneeIds!!.toSet())
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
        tb.admin.user.assertDeleteFailStatus(409, testUser2)
        tb.admin.user.assertDeleteFailStatus(409, testUser3)
    }

    /*
    Test case for cascade updating the tasks forward:
    Source:
    Day:    1   2   3   4   5   6
    Task:   |t1-|
                |t2-|
                    |t3-----|
                        |t4-----|
    With expected outcome after moving t1 to 3-4
    Day:    1   2   3   4   5   6
    Task:           |t1-|
                        |t2-|
                        |t3-----|
                        |t4-----|
     */
    @Test
    fun tsetUpdateTaskConnectionsForward() = createTestBuilder().use { tb ->
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

        val task = tb.admin.task.create(name = "task1", projectId = project.id, milestoneId = milestone.id!!, startDate = "2022-01-01", endDate = "2022-01-02")
        val task2 = tb.admin.task.create(name = "task2", projectId = project.id, milestoneId = milestone.id, startDate = "2022-01-02", endDate = "2022-01-03")
        val task3 = tb.admin.task.create(name = "task3", projectId = project.id, milestoneId = milestone.id!!, startDate = "2022-01-03", endDate = "2022-01-05")
        val task4 = tb.admin.task.create(name = "task4", projectId = project.id, milestoneId = milestone.id!!, startDate = "2022-01-04", endDate = "2022-01-06")
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = task.id!!,
                targetTaskId = task2.id!!,
                type = TaskConnectionType.FINISH_TO_START
            )
        )
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = task2.id,
                targetTaskId = task3.id!!,
                type = TaskConnectionType.START_TO_START
            )
        )
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = task3.id,
                targetTaskId = task4.id!!,
                type = TaskConnectionType.FINISH_TO_FINISH
            )
        )

        tb.admin.task.update(projectId = project.id, milestoneId = milestone.id, taskId = task.id, task.copy(startDate = "2022-01-03", endDate = "2022-01-04"))

        val foundTask1 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task.id)
        val foundTask2 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task2.id)
        val foundTask3 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task3.id)
        val foundTask4 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task4.id)


        assertEquals("2022-01-03", foundTask1.startDate)
        assertEquals("2022-01-04", foundTask1.endDate)

        assertEquals("2022-01-04", foundTask2.startDate)
        assertEquals("2022-01-05", foundTask2.endDate)

        assertEquals("2022-01-04", foundTask3.startDate)
        assertEquals("2022-01-06", foundTask3.endDate)

        assertEquals("2022-01-04", foundTask4.startDate)
        assertEquals("2022-01-06", foundTask4.endDate)

        // test moving to invalid dates (cascade affects the milestone)
        tb.admin.task.assertUpdateFail(
            expectedStatus = 400,
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            task = task.copy(startDate = "2022-01-30", endDate = "2022-01-31")
        )
    }

    /*
    Test case for cascade updating the tasks backwards:
    Source:
    Day:    1   2   3   4   5   6
    Task:   |t1-|
                |t2-|
                    |t3-----|
                        |t4-----|
    With expected outcome after moving t4 to 2-4
    Day:    1   2   3   4   5   6
    Task:   |t1-|
                |t2-|
                |t3-----|
                |t4-----|
     */
    @Test
    fun testUpdateTaskConnectionsBackwards() = createTestBuilder().use { tb ->
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

        val task = tb.admin.task.create(name = "task1", projectId = project.id, milestoneId = milestone.id!!, startDate = "2022-01-01", endDate = "2022-01-02")
        val task2 = tb.admin.task.create(name = "task2", projectId = project.id, milestoneId = milestone.id, startDate = "2022-01-02", endDate = "2022-01-03")
        val task3 = tb.admin.task.create(name = "task3", projectId = project.id, milestoneId = milestone.id, startDate = "2022-01-03", endDate = "2022-01-05")
        val task4 = tb.admin.task.create(name = "task4", projectId = project.id, milestoneId = milestone.id, startDate = "2022-01-04", endDate = "2022-01-06")
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = task.id!!,
                targetTaskId = task2.id!!,
                type = TaskConnectionType.FINISH_TO_START
            )
        )
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = task2.id,
                targetTaskId = task3.id!!,
                type = TaskConnectionType.START_TO_START
            )
        )
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = task3.id,
                targetTaskId = task4.id!!,
                type = TaskConnectionType.FINISH_TO_FINISH
            )
        )

        tb.admin.task.update(projectId = project.id, milestoneId = milestone.id, taskId = task4.id, task4.copy(startDate = "2022-01-02", endDate = "2022-01-04"))

        val foundTask1 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task.id)
        val foundTask2 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task2.id)
        val foundTask3 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task3.id)
        val foundTask4 = tb.admin.task.find(projectId = project.id, milestoneId = milestone.id, taskId = task4.id)


        assertEquals("2022-01-01", foundTask1.startDate)
        assertEquals("2022-01-02", foundTask1.endDate)

        assertEquals("2022-01-02", foundTask2.startDate)
        assertEquals("2022-01-03", foundTask2.endDate)

        assertEquals("2022-01-02", foundTask3.startDate)
        assertEquals("2022-01-04", foundTask3.endDate)

        assertEquals("2022-01-02", foundTask4.startDate)
        assertEquals("2022-01-04", foundTask4.endDate)
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