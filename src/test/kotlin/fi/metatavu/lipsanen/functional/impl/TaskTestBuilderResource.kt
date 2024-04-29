package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.TasksApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.Task
import fi.metatavu.lipsanen.test.client.models.TaskStatus
import junit.framework.TestCase.fail
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test builder resource for task API
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class TaskTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Task, TasksApi>(testBuilder, apiClient) {

    private val taskToMilestoneProject = mutableMapOf<UUID, Pair<UUID, UUID>>()

    override fun clean(p0: Task?) {
        p0?.let {
            api.deleteTask(
                projectId = taskToMilestoneProject[it.id]?.second!!,
                milestoneId = taskToMilestoneProject[it.id]?.first!!,
                taskId = it.id!!
            )
        }
        taskToMilestoneProject.remove(p0?.id)
    }

    override fun getApi(): TasksApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return TasksApi(ApiTestSettings.apiBasePath)
    }

    fun create(
        projectId: UUID,
        milestoneId: UUID,
        task: Task
    ): Task {
        val created = addClosable(
            api.createTask(
                projectId = projectId,
                milestoneId = milestoneId,
                task = task
            )
        )
        taskToMilestoneProject[created.id!!] = Pair(milestoneId, projectId)
        return created
    }

    fun create(
        projectId: UUID,
        milestoneId: UUID
    ): Task {
        return create(
            projectId = projectId,
            milestoneId = milestoneId,
            task = Task(
                name = "Task",
                startDate = "2022-01-01",
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestoneId,
                endDate = "2022-01-31"
            )
        )
    }

    fun find(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID
    ): Task {
        return api.findTask(
            projectId = projectId,
            milestoneId = milestoneId,
            taskId = taskId
        )
    }

    fun list(
        projectId: UUID,
        milestoneId: UUID
    ): Array<Task> {
        return api.listTasks(
            projectId = projectId,
            milestoneId = milestoneId
        )
    }

    fun update(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        task: Task
    ): Task {
        return api.updateTask(
            projectId = projectId,
            milestoneId = milestoneId,
            taskId = taskId,
            task = task
        )
    }

    fun delete(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID
    ) {
        api.deleteTask(
            projectId = projectId,
            milestoneId = milestoneId,
            taskId = taskId
        )
        removeCloseable { closable: Any ->
            if (closable !is Task) {
                return@removeCloseable false
            }

            closable.id == taskId
        }
    }

    fun assertCreateFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        task: Task
    ) {
        try {
            api.createTask(
                projectId = projectId,
                milestoneId = milestoneId,
                task = task
            )
            fail(String.format("Expected create to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertFindFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID
    ) {
        try {
            api.findTask(
                projectId = projectId,
                milestoneId = milestoneId,
                taskId = taskId
            )
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID
    ) {
        try {
            api.listTasks(
                projectId = projectId,
                milestoneId = milestoneId
            )
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        task: Task
    ) {
        try {
            api.updateTask(
                projectId = projectId,
                milestoneId = milestoneId,
                taskId = taskId,
                task = task
            )
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID
    ) {
        try {
            api.deleteTask(
                projectId = projectId,
                milestoneId = milestoneId,
                taskId = taskId
            )
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

}