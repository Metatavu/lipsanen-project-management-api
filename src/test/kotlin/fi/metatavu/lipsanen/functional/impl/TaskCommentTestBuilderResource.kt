package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.TaskCommentsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.TaskComment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test Resource for TaskComments API
 */
class TaskCommentTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<TaskComment, TaskCommentsApi>(testBuilder, apiClient) {
    private val taskCommentToProjectId = mutableMapOf<UUID, UUID>()
    override fun clean(p0: TaskComment?) {
        p0?.let {
            api.deleteTaskComment(
                projectId = taskCommentToProjectId[it.id]!!,
                taskId = it.taskId,
                commentId = it.id!!
            )
            taskCommentToProjectId.remove(p0.id)
        }
    }

    override fun getApi(): TaskCommentsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return TaskCommentsApi(ApiTestSettings.apiBasePath)
    }

    fun create(
        projectId: UUID, taskId: UUID, taskComment: TaskComment? = null
    ): TaskComment {
        val comment = addClosable(
            api.createTaskComment(
                projectId = projectId,
                taskId = taskId,
                taskComment = taskComment
                    ?: TaskComment(
                        taskId = taskId, comment = "comment", referencedUsers = emptyArray()
                    )
            )
        )
        taskCommentToProjectId[comment.id!!] = projectId
        return comment
    }

    fun findTaskComment(projectId: UUID, taskId: UUID, taskCommentId: UUID): TaskComment {
        return api.findTaskComment(projectId, taskId, taskCommentId)
    }

    fun listTaskComments(projectId: UUID, taskId: UUID): Array<TaskComment> {
        return api.listTaskComments(projectId, taskId)
    }

    fun updateTaskComment(
        projectId: UUID, taskId: UUID, taskCommentId: UUID, taskComment: TaskComment
    ): TaskComment {
        return api.updateTaskComment(projectId, taskId, taskCommentId, taskComment)
    }

    fun deleteTaskComment(projectId: UUID, taskId: UUID, taskCommentId: UUID) {
        api.deleteTaskComment(projectId, taskId, taskCommentId)
        removeCloseable { closable: Any ->
            if (closable !is TaskComment) {
                return@removeCloseable false
            }

            closable.id == taskCommentId
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int, projectId: UUID, taskId: UUID, taskCommentId: UUID
    ) {
        try {
            api.deleteTaskComment(projectId, taskId, taskCommentId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        taskId: UUID,
        taskCommentId: UUID,
        taskComment: TaskComment
    ) {
        try {
            api.updateTaskComment(projectId, taskId, taskCommentId, taskComment)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertFindFail(
        expectedStatus: Int, projectId: UUID, taskId: UUID, taskCommentId: UUID
    ) {
        try {
            api.findTaskComment(projectId, taskId, taskCommentId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

}