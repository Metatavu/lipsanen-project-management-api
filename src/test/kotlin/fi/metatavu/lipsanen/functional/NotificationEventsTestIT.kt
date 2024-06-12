package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.NotificationType
import fi.metatavu.lipsanen.test.client.models.Task
import fi.metatavu.lipsanen.test.client.models.TaskStatus
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.*
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
    fun createNotificationsTaskAssignedTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)

        val testUser = tb.admin.user.create("test0").id!!
        val task1 = tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(testUser),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id
            )
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = testUser,
            projectId = project1.id
        )
        assertEquals(1, notificationEvents.size)
        val notificationEvent = notificationEvents.first()
        assertEquals(task1.id, notificationEvent.notification.taskId)
        assertEquals(fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_ASSIGNED, notificationEvent.notification.type)
        assertEquals(false, notificationEvent.read)
        assertEquals(testUser, notificationEvent.receiverId)
        assertNotNull(notificationEvent.notification.message)

       /* val notificationEventsForAdmin = tb.admin.notificationEvent.list(
            userId = admin!!.id!!,
            projectId = project1.id
        )
        assertEquals(1, notificationEventsForAdmin.size)
        assertEquals(admin.id, notificationEventsForAdmin[0].receiverId)
        todo cannot check admin not since there is no admin user
*/

        // Test fails of access rights checks and not found
        tb.user.notificationEvent.assertListFailStatus(400, userId = testUser)
        tb.admin.notificationEvent.assertListFailStatus(404,
            userId = testUser,
            projectId = UUID.randomUUID()
        )

        // Cleanup
        //todo no way to clean up admin notifications
        tb.admin.notification.delete(notificationEvents[0].notification.id!!)
    }

    /**
     * Tests creating task status changed notification and task assigned notifications
     */
    @Test
    fun createNotificationTaskStatusChangedTset() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val testUser = tb.admin.user.create("test0").id!!

        val task1 = tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(testUser),
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

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = testUser,
            projectId = project1.id
        )
        assertEquals(2, notificationEvents.size)
        val taskUpdated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_STATUS_CHANGED }
        assertEquals(task1.id, taskUpdated!!.notification.taskId)
        assertEquals(false, taskUpdated.read)
        assertEquals(testUser, taskUpdated.receiverId)
        assertNotNull(taskUpdated.notification.message)

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.delete(it.notification.id!!) }
    }

    /**
     * Tests creating change proposal status changed notification
     */
    @Test
    fun createNotificationChangeProposalStatusChangedTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val testUser = tb.admin.user.create("test0").id!!

        val task1 = tb.admin.task.create(projectId = project1.id, milestoneId = milestone1.id!!, task = Task(
            name = "Task 1",
            startDate = "2024-01-01",
            endDate = "2024-01-02",
            assigneeIds = arrayOf(testUser),
            status = TaskStatus.NOT_STARTED,
            milestoneId = milestone1.id
        ))
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
            userId = testUser,
            projectId = project1.id
        )
        assertEquals(3, notificationEvents.size)
        // test that assignee received all the notifications
        val proposalCreated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.CHANGE_PROPOSAL_CREATED }
        assertEquals(task1.id, proposalCreated!!.notification.taskId)
        assertEquals(false, proposalCreated.read)
        assertEquals(testUser, proposalCreated.receiverId)
        assertNotNull(proposalCreated.notification.message)
        val proposalUpdated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.CHANGE_PROPOSAL_STATUS_CHANGED }
        assertEquals(task1.id, proposalUpdated!!.notification.taskId)
        assertEquals(false, proposalUpdated.read)
        assertEquals(testUser, proposalUpdated.receiverId)
        assertNotNull(proposalUpdated.notification.message)
        val taskAssigned = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_ASSIGNED }
        assertNotNull(taskAssigned)

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.delete(it.notification.id!!) }
    }

    /**
     * Tests creating task assigned notification and updating it as read as well as dismissing it
     */
    @Test
    fun updateNotificationTaskAssignedTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val testUser1 = tb.admin.user.create("test0").id!!

        tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task = Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(testUser1),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id
            )
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = testUser1,
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
            userId = testUser1,
            projectId = project1.id,
            readStatus = true
        )
        assertEquals(1, notificationEventsRead.size)

        val notificationEventsUnread = tb.admin.notificationEvent.list(
            userId = testUser1,
            projectId = project1.id,
            readStatus = false
        )
        assertEquals(0, notificationEventsUnread.size)

        //Dismiss
        tb.admin.notificationEvent.delete(notificationEvent.id)
        assertEquals(
            0, tb.admin.notificationEvent.list(
                userId = testUser1,
                projectId = project1.id,
            ).size
        )

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.delete(it.notification.id!!) }
    }

    /**
     * Tests that leaving a comment notifies task assignees, mentioned users and admin.
     * Test scenario:
     * Admin creates task assigned to user (who gets assigned to the project)
     * user leaves comment on task, mentions another user1 (who was manually assigned to the project).
     * Admin, user1 and user should receive the comment notifications
     */
    @Test
    fun commentLeftTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val allUsers = tb.admin.user.listUsers()

        val user = allUsers.find { it.firstName == "user" }!!.id!!
        val user1 = allUsers.find { it.firstName == "user1" }!!.id!!
        val user2 = allUsers.find { it.firstName == "user2" }!!.id!!
        val admin = allUsers.find { it.firstName == "admin" }!!.id!!
        //assign user1 to the project
        tb.admin.user.updateUser(
            user1,
            user = allUsers.find { it.id == user1}!!.copy(projectIds = arrayOf(project1.id))
        )

        //user gets automatically assigned to the project
        val task = tb.admin.task.create(
            projectId = project1.id, milestoneId = milestone1.id!!, task = Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(user),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id
            )
        )

        val comment = tb.user.taskComment.create(
            projectId = project1.id,
            milestoneId = milestone1.id,
            taskId = task.id!!,
            taskComment = fi.metatavu.lipsanen.test.client.models.TaskComment(
                comment = "Comment",
                referencedUsers = arrayOf(user1),
                taskId = task.id
            )
        )

        //todo check events auth for user
        val userNotifications = tb.admin.notificationEvent.list(
            userId = user,
            projectId = project1.id
        )
        assertEquals(1, userNotifications.size)

        val adminNotifications = tb.admin.notificationEvent.list(
            userId = admin,
            projectId = project1.id
        )
        assertEquals(2, adminNotifications.size)
        assertTrue(adminNotifications.any { it.notification.type == NotificationType.COMMENT_LEFT })

        val user1Notifications = tb.user1.notificationEvent.list(
            userId = user1,
            projectId = project1.id
        )
        assertEquals(1, user1Notifications.size)
        assertEquals(NotificationType.COMMENT_LEFT, user1Notifications[0].notification.type)
        assertEquals(comment.id!!, user1Notifications[0].notification.commentId)
        assertEquals(task.id, user1Notifications[0].notification.taskId)

        val user2Notifications = tb.user2.notificationEvent.list(
            userId = user2,
            projectId = project1.id
        )
        assertEquals(0, user2Notifications.size)
    }

}