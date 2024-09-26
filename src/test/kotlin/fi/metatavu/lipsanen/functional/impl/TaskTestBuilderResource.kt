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

    override fun clean(p0: Task?) {
        p0?.let {
            api.deleteTask(
                taskId = it.id!!
            )
        }
    }

    override fun getApi(): TasksApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return TasksApi(ApiTestSettings.apiBasePath)
    }

    fun create(
        task: Task
    ): Task {
        val created = addClosable(
            api.createTask(
                task = task
            )
        )
        return created
    }

    fun create(
        milestoneId: UUID,
        startDate: String,
        endDate: String,
        name: String? = null
    ): Task {
        return create(
            task = Task(
                name = name ?: "Task",
                startDate = startDate,
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestoneId,
                endDate = endDate
            )
        )
    }

    fun create(
        milestoneId: UUID,
        assigneeId: UUID ?= null
    ): Task {
        return create(
            task = Task(
                name = "Task",
                startDate = "2022-01-01",
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestoneId,
                endDate = "2022-01-31",
                assigneeIds = assigneeId?.let { arrayOf(it) }
            )
        )
    }

    fun find(
        taskId: UUID
    ): Task {
        return api.findTask(
            taskId = taskId
        )
    }

    fun list(
        projectId: UUID? = null,
        changeProposalId: UUID? = null,
        milestoneId: UUID? = null
    ): Array<Task> {
        return api.listTasks(
            projectId = projectId,
            changeProposalId = changeProposalId,
            milestoneId = milestoneId
        )
    }

    fun update(
        taskId: UUID,
        task: Task
    ): Task {
        return api.updateTask(
            taskId = taskId,
            task = task
        )
    }

    fun delete(
        taskId: UUID
    ) {
        api.deleteTask(
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
        task: Task
    ) {
        try {
            api.createTask(
                task = task
            )
            fail(String.format("Expected create to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertFindFail(
        expectedStatus: Int,
        taskId: UUID
    ) {
        try {
            api.findTask(
                taskId = taskId
            )
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID? = null,
        milestoneId: UUID? = null,
        changeProposalId: UUID? = null
    ) {
        try {
            api.listTasks(
                projectId = projectId,
                milestoneId = milestoneId,
                changeProposalId = changeProposalId
            )
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        taskId: UUID,
        task: Task
    ) {
        try {
            api.updateTask(
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
        taskId: UUID
    ) {
        try {
            api.deleteTask(
                taskId = taskId
            )
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

}