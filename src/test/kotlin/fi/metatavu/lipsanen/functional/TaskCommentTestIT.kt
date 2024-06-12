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
import fi.metatavu.lipsanen.test.client.models.TaskComment
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for Task Comment API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class TaskCommentTestIT : AbstractFunctionalTest() {

    @Test
    fun testListTaskComments() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val milestone2 = tb.admin.milestone.create(projectId = project.id)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone2.id!!)
        tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)
        tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)
        tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone2.id, taskId = task1.id!!)

        val taskComments = tb.admin.taskComment.listTaskComments(projectId = project.id, milestoneId = milestone.id, taskId = task.id)
        assert(taskComments.size == 2)
        val task1Comments = tb.admin.taskComment.listTaskComments(
            projectId = project.id,
            milestoneId = milestone2.id,
            taskId = task1.id
        )
        assert(task1Comments.size == 1)
    }

    @Test
    fun testListFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}/comments",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun testCreateTaskComment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val user = tb.admin.user.listUsers().find { it.firstName == "user" }!!
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!, assigneeId = user.id!!)

        val taskCommentData = TaskComment(
            taskId = task.id!!,
            comment = "comment",
            referencedUsers = arrayOf(user.id)
        )

        val createdComment = tb.admin.taskComment.create(
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskComment = taskCommentData
        )
        assertNotNull(createdComment.id)
        assertEquals(createdComment.comment, taskCommentData.comment)
        assertEquals(createdComment.referencedUsers.size, taskCommentData.referencedUsers.size)
        assertEquals(task.id, createdComment.taskId)
    }

    @Test
    fun testCreateTaskCommentFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = TaskComment(
            taskId = task.id!!,
            comment = "comment",
            referencedUsers = emptyArray()
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}/comments",
            method = Method.POST,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(taskComment)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    default = project.id,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    default = milestone.id,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    default = task.id!!,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    values = listOf(
                        SimpleInvalidValueProvider(
                            jacksonObjectMapper().writeValueAsString(
                                taskComment.copy(
                                    taskId = UUID.randomUUID()
                                )
                            )
                        ),
                        SimpleInvalidValueProvider(
                            jacksonObjectMapper().writeValueAsString(
                                taskComment.copy(
                                    referencedUsers = arrayOf(UUID.randomUUID())
                                )
                            )
                        ),
                        SimpleInvalidValueProvider(
                            jacksonObjectMapper().writeValueAsString(
                                taskComment.copy(
                                    referencedUsers = arrayOf(ApiTestSettings.user1Id)
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
    fun testFindTaskComment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        val foundComment = tb.admin.taskComment.findTaskComment(
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!
        )
        assertNotNull(foundComment)
    }

    @Test
    fun testFindTaskCommentFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        //access rights
        tb.user.taskComment.assertFindFail(
            403,
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}/comments/{commentId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id!!
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = milestone.id!!
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = task.id!!
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "commentId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = taskComment.id!!
                )
            )
            .build()
            .test()
    }

    @Test
    fun testUpdateTaskComment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        val updatedComment = tb.admin.taskComment.updateTaskComment(
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!,
            taskComment = TaskComment(
                taskId = task.id,
                comment = "updated comment",
                referencedUsers = emptyArray()
            )
        )
        assertEquals(updatedComment.comment, "updated comment")
    }

    @Test
    fun testUpdateTaskCommentFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        //access rights
        tb.user.taskComment.assertUpdateFail(
            403,
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!,
            taskComment = taskComment
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}/comments/{commentId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(taskComment)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    default = project.id!!,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    default = milestone.id!!,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    default = task.id!!,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "commentId",
                    default = taskComment.id!!,
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    values = listOf(
                        SimpleInvalidValueProvider(
                            jacksonObjectMapper().writeValueAsString(
                                TaskComment(
                                    taskId = UUID.randomUUID(),
                                    comment = "comment",
                                    referencedUsers = emptyArray()
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
    fun testDeleteTaskComment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        tb.admin.taskComment.deleteTaskComment(
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!
        )
        tb.admin.taskComment.assertFindFail(
            404,
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!
        )
    }

    @Test
    fun testDeleteTaskCommentFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val taskComment = tb.admin.taskComment.create(projectId = project.id, milestoneId = milestone.id, taskId = task.id!!)

        //access rights
        tb.user.taskComment.assertDeleteFail(
            403,
            projectId = project.id,
            milestoneId = milestone.id,
            taskId = task.id,
            taskCommentId = taskComment.id!!
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/milestones/{milestoneId}/tasks/{taskId}/comments/{commentId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "milestoneId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "taskId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "commentId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }
}