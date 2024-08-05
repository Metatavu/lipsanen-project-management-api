package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.TaskConnection
import fi.metatavu.lipsanen.test.client.models.TaskConnectionRole
import fi.metatavu.lipsanen.test.client.models.TaskConnectionType
import fi.metatavu.lipsanen.test.client.models.TaskStatus
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Task connection tests
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class TaskConnectionTestIT : AbstractFunctionalTest() {

    @Test
    fun testConnectionCreate() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val createdTask = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val createdTask2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id)
        val taskConnection = tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(
                sourceTaskId = createdTask.id!!,
                targetTaskId = createdTask2.id!!,
                type = TaskConnectionType.START_TO_START
            )
        )

        assertNotNull(taskConnection)
        assertNotNull(taskConnection.id)
        assertEquals(createdTask.id, taskConnection.sourceTaskId)
        assertEquals(createdTask2.id, taskConnection.targetTaskId)

        // check connection checks
        tb.admin.task.assertUpdateFail(
            409,
            projectId = project.id,
            taskId = createdTask2.id,
            task = createdTask2.copy(status = TaskStatus.IN_PROGRESS)
        )
    }

    @Test
    fun testConnectionList() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val createdTask = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val createdTask2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id)
        val createdTask3 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id)

        val taskConnection = tb.admin.taskConnection.create(
            projectId = project.id, taskConnection = TaskConnection(
                sourceTaskId = createdTask.id!!,
                targetTaskId = createdTask2.id!!,
                TaskConnectionType.START_TO_START
            )
        )
        val taskConnection2 = tb.admin.taskConnection.create(
            projectId = project.id, taskConnection = TaskConnection(
                sourceTaskId = createdTask2.id,
                targetTaskId = createdTask3.id!!,
                TaskConnectionType.START_TO_START
            )
        )

        tb.admin.taskConnection.listTaskConnections(project.id).let {
            assertEquals(2, it.size)
        }

        tb.admin.taskConnection.listTaskConnections(project.id, createdTask2.id).let {
            assertEquals(2, it.size)
        }

        tb.admin.taskConnection.listTaskConnections(project.id, createdTask3.id, TaskConnectionRole.TARGET).let {
            assertEquals(1, it.size)
            assertEquals(taskConnection2.id, it[0].id)
        }
    }

    @Test
    fun testConnectionFind() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val createdTask = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val createdTask2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id)
        val taskConnection = tb.admin.taskConnection.create(
            projectId = project.id, taskConnection = TaskConnection(
                sourceTaskId = createdTask.id!!,
                targetTaskId = createdTask2.id!!,
                TaskConnectionType.START_TO_START
            )
        )

        val foundConnection = tb.admin.taskConnection.findTaskConnection(project.id, taskConnection.id!!)
        assertNotNull(foundConnection)

    }

    @Test
    fun testConnectionUpdate() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val createdTask = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!, startDate = "2021-01-01", endDate = "2021-01-02")
        val createdTask2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id, startDate = "2021-01-02", endDate = "2021-01-03")
        val taskConnection = tb.admin.taskConnection.create(
            projectId = project.id, taskConnection = TaskConnection(
                sourceTaskId = createdTask.id!!,
                targetTaskId = createdTask2.id!!,
                TaskConnectionType.FINISH_TO_FINISH
            )
        )

        val updatedConnection = tb.admin.taskConnection.updateTaskConnection(
            projectId = project.id,
            taskConnectionId = taskConnection.id!!,
            taskConnection = TaskConnection(
                sourceTaskId = createdTask.id,
                targetTaskId = createdTask2.id,
                type = TaskConnectionType.FINISH_TO_START
            )
        )

        assertNotNull(updatedConnection)
        assertEquals(taskConnection.id, updatedConnection.id)
        assertEquals(createdTask2.id, updatedConnection.targetTaskId)
        assertEquals(createdTask.id, updatedConnection.sourceTaskId)
        assertEquals(TaskConnectionType.FINISH_TO_START, updatedConnection.type)
    }

    @Test
    fun testConnectionDelete() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val createdTask = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val createdTask2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id)
        val taskConnection = tb.admin.taskConnection.create(
            projectId = project.id, taskConnection = TaskConnection(
                sourceTaskId = createdTask.id!!,
                targetTaskId = createdTask2.id!!,
                TaskConnectionType.START_TO_START
            )
        )

        tb.admin.taskConnection.deleteTaskConnection(project.id, taskConnection.id!!)
        tb.admin.taskConnection.assertFindFail(404, project.id, taskConnection.id)
    }

}