package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Task
import fi.metatavu.lipsanen.test.client.models.TaskStatus
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

/*
Tests For NotificationEvents API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class NotificationEventsTestIT : AbstractFunctionalTest() {

    /**
     * Tests creating task assigned notification and the notifications
     * being sent to intended users + admin of the project,
     * Tests some fail scenarios
     */
    @Test
    fun createNotificationsTaskAssigned() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)

        val users = tb.admin.user.listUsers(null, null, null, true)
        val admin = users.find { it.firstName == "admin" }

        val user = users.find { it.firstName == "user" }
        val task1 = tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(user!!.id!!),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id
            )
        )

        val notificationEvents = tb.user.notificationEvent.list(
            userId = user.id!!,
            projectId = project1.id
        )
        assertEquals(1, notificationEvents.size)
        val notificationEvent = notificationEvents.first()
        assertEquals(task1.id, notificationEvent.notification.taskId)
        assertEquals(fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_ASSIGNED, notificationEvent.notification.type)
        assertEquals(false, notificationEvent.read)
        assertEquals(user.id, notificationEvent.receiverId)
        assertNotNull(notificationEvent.notification.message)

        val notificationEventsForAdmin = tb.admin.notificationEvent.list(
            userId = admin!!.id!!,
            projectId = project1.id
        )
        assertEquals(1, notificationEventsForAdmin.size)
        assertEquals(admin.id, notificationEventsForAdmin[0].receiverId)

        // Test fails of access rights checks and not found
        tb.admin.notificationEvent.assertListFailStatus(400, userId = user.id)
        tb.user.notificationEvent.assertListFailStatus(404,
            userId = user.id,
            projectId = UUID.randomUUID()
        )

        // Cleanup
        tb.admin.notification.delete(notificationEventsForAdmin[0].notification.id!!)
    }

    /**
     * Tests creating task status changed notification and task assigned notifications
     */
    @Test
    fun createNotificationTaskStatusChanged() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val user = tb.admin.user.listUsers().find { it.firstName == "user" }

        val task1 = tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(user!!.id!!),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id
            )
        )
        tb.admin.task.update(
            projectId = project1.id,
            milestoneId = milestone1.id,
            taskId = task1.id!!,
            task = task1.copy(status = TaskStatus.IN_PROGRESS)
        )

        val notificationEvents = tb.user.notificationEvent.list(
            userId = user.id!!,
            projectId = project1.id
        )
        assertEquals(2, notificationEvents.size)
        val taskUpdated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_STATUS_CHANGED }
        assertEquals(task1.id, taskUpdated!!.notification.taskId)
        assertEquals(false, taskUpdated.read)
        assertEquals(user.id, taskUpdated.receiverId)
        assertNotNull(taskUpdated.notification.message)

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.delete(it.notification.id!!) }
    }

    /**
     * Tests creating change proposal status changed notification
     */
    @Test
    fun createNotificationChangeProposalStatusChanged() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val admin = tb.admin.user.listUsers().find { it.firstName == "admin" }!!

        val task1 = tb.admin.task.create(projectId = project1.id, milestoneId = milestone1.id!!)
        val changeProposal = tb.admin.changeProposal.create(
            projectId = project1.id,
            milestoneId = milestone1.id,
            taskId = task1.id!!
        )!!
        tb.admin.changeProposal.updateChangeProposal(
            projectId = project1.id,
            milestoneId = milestone1.id,
            changeProposalId = changeProposal.id!!,
            changeProposal = changeProposal.copy(status = fi.metatavu.lipsanen.test.client.models.ChangeProposalStatus.REJECTED)
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = admin.id!!,
            projectId = project1.id
        )
        assertEquals(1, notificationEvents.size)
        val proposalUpdated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.CHANGE_PROPOSAL_STATUS_CHANGED }
        assertEquals(task1.id, proposalUpdated!!.notification.taskId)
        assertEquals(false, proposalUpdated.read)
        assertEquals(admin.id, proposalUpdated.receiverId)
        assertNotNull(proposalUpdated.notification.message)

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.delete(it.notification.id!!) }
    }

    /**
     * Tests creating task assigned notification and updating it as read as well as dismissing it
     */
    @Test
    fun updateNotificationTaskAssigned() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val admin = tb.admin.user.listUsers().find { it.firstName == "admin" }!!
        tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task = Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(admin.id!!),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id
            )
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = admin.id,
            projectId = project1.id,
        )
        assertEquals(1, notificationEvents.size)

        // Mark as read
        val notificationEvent = notificationEvents.first()
        val updated = tb.admin.notificationEvent.updateNotification(
            notificationEventId = notificationEvent.id!!,
            updateBody = notificationEvent.copy(read = true)
        )
        assertEquals(true, updated.read)

        val notificationEventsRead = tb.admin.notificationEvent.list(
            userId = admin.id,
            projectId = project1.id,
            readStatus = true
        )
        assertEquals(1, notificationEventsRead.size)

        val notificationEventsUnread = tb.admin.notificationEvent.list(
            userId = admin.id,
            projectId = project1.id,
            readStatus = false
        )
        assertEquals(0, notificationEventsUnread.size)

        //Dismiss
        tb.admin.notificationEvent.delete(notificationEvent.id)
        assertEquals(
            0, tb.admin.notificationEvent.list(
                userId = admin.id,
                projectId = project1.id,
            ).size
        )

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.delete(it.notification.id!!) }
    }

}