package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.TaskConnectionsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.TaskConnection
import fi.metatavu.lipsanen.test.client.models.TaskConnectionRole
import junit.framework.TestCase.fail
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test builder resource for task connection API
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class TaskConnectionTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<TaskConnection, TaskConnectionsApi>(testBuilder, apiClient) {

    private val taskConnectionToProjectId = mutableMapOf<UUID, UUID>()

    override fun clean(p0: TaskConnection?) {
        p0?.let {
            api.deleteTaskConnection(
                projectId = taskConnectionToProjectId[it.id]!!,
                connectionId = it.id!!
            )
            taskConnectionToProjectId.remove(p0.id)
        }

    }

    override fun getApi(): TaskConnectionsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return TaskConnectionsApi(ApiTestSettings.apiBasePath)
    }

    fun create(projectId: UUID, taskConnection: TaskConnection): TaskConnection {
        val connection = addClosable(
            api.createTaskConnection(
                projectId = projectId,
                taskConnection = taskConnection
            )
        )
        taskConnectionToProjectId[connection.id!!] = projectId
        return connection
    }

    fun findTaskConnection(projectId: UUID, taskConnectionId: UUID): TaskConnection {
        return api.findTaskConnection(projectId, taskConnectionId)
    }

    fun listTaskConnections(
        projectId: UUID,
        taskId: UUID? = null,
        connectionRole: TaskConnectionRole? = null
    ): Array<TaskConnection> {
        return api.listTaskConnections(projectId, taskId, connectionRole)
    }

    fun updateTaskConnection(projectId: UUID, taskConnectionId: UUID, taskConnection: TaskConnection): TaskConnection {
        return api.updateTaskConnection(projectId, taskConnectionId, taskConnection)
    }

    fun deleteTaskConnection(projectId: UUID, taskConnectionId: UUID) {
        api.deleteTaskConnection(projectId, taskConnectionId)
        removeCloseable { closable: Any ->
            if (closable !is TaskConnection) {
                return@removeCloseable false
            }

            closable.id == taskConnectionId
        }
    }

    fun assertCreateFail(status: Int, projectId: UUID, taskConnection: TaskConnection) {
        try {
            api.createTaskConnection(projectId, taskConnection)
            fail("Expected createTaskConnection to fail")
        } catch (e: ClientException) {
            Assertions.assertEquals(status, e.statusCode)
        }
    }

    fun assertFindFail(status: Int, projectId: UUID, taskConnectionId: UUID) {
        try {
            api.findTaskConnection(projectId, taskConnectionId)
            fail("Expected findTaskConnection to fail")
        } catch (e: ClientException) {
            Assertions.assertEquals(status, e.statusCode)
        }
    }

    fun assertListFail(status: Int, projectId: UUID) {
        try {
            api.listTaskConnections(projectId)
            fail("Expected listTaskConnections to fail")
        } catch (e: ClientException) {
            Assertions.assertEquals(status, e.statusCode)
        }
    }

    fun assertUpdateFail(status: Int, projectId: UUID, taskConnectionId: UUID, taskConnection: TaskConnection) {
        try {
            api.updateTaskConnection(projectId, taskConnectionId, taskConnection)
            fail("Expected updateTaskConnection to fail")
        } catch (e: ClientException) {
            Assertions.assertEquals(status, e.statusCode)
        }
    }

    fun assertDeleteFail(status: Int, projectId: UUID, taskConnectionId: UUID) {
        try {
            api.deleteTaskConnection(projectId, taskConnectionId)
            fail("Expected deleteTaskConnection to fail")
        } catch (e: ClientException) {
            Assertions.assertEquals(status, e.statusCode)
        }
    }
}