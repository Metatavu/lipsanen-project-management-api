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
    override fun clean(p0: TaskComment?) {
        p0?.let {
            api.deleteTaskComment(
                taskId = it.taskId,
                commentId = it.id!!
            )
        }
    }

    override fun getApi(): TaskCommentsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return TaskCommentsApi(ApiTestSettings.apiBasePath)
    }

    fun create(
        taskId: UUID, taskComment: TaskComment? = null
    ): TaskComment {
        val comment = addClosable(
            api.createTaskComment(
                taskId = taskId,
                taskComment = taskComment
                    ?: TaskComment(
                        taskId = taskId, comment = "comment", referencedUsers = emptyArray()
                    )
            )
        )
        return comment
    }

    fun findTaskComment(taskId: UUID, taskCommentId: UUID): TaskComment {
        return api.findTaskComment(taskId, taskCommentId)
    }

    fun listTaskComments(taskId: UUID): Array<TaskComment> {
        return api.listTaskComments(taskId)
    }

    fun updateTaskComment(
        taskId: UUID, taskCommentId: UUID, taskComment: TaskComment
    ): TaskComment {
        return api.updateTaskComment(taskId, taskCommentId, taskComment)
    }

    fun deleteTaskComment(taskId: UUID, taskCommentId: UUID) {
        api.deleteTaskComment(taskId, taskCommentId)
        removeCloseable { closable: Any ->
            if (closable !is TaskComment) {
                return@removeCloseable false
            }

            closable.id == taskCommentId
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int, taskId: UUID, taskCommentId: UUID
    ) {
        try {
            api.deleteTaskComment(taskId, taskCommentId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        taskId: UUID,
        taskCommentId: UUID,
        taskComment: TaskComment
    ) {
        try {
            api.updateTaskComment(taskId, taskCommentId, taskComment)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertFindFail(
        expectedStatus: Int, taskId: UUID, taskCommentId: UUID
    ) {
        try {
            api.findTaskComment(taskId, taskCommentId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

}