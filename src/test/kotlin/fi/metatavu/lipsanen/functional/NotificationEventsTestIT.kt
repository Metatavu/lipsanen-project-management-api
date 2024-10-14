package fi.metatavu.lipsanen.functional

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.lipsanen.api.model.TaskAssignedNotificationData
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.*
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

        val testUser = tb.admin.user.create("test0", UserRole.USER).id!!
        val testUser1 = tb.admin.user.create("test1", UserRole.USER).id!!

        val adminUser = tb.admin.user.create("admin0", UserRole.ADMIN)
        val task1 = tb.admin.task.create(task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(testUser, testUser1),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id!!
            )
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = testUser,
            projectId = project1.id
        )
        assertEquals(1, notificationEvents.size)
        val notificationEvent = notificationEvents.first()
        val notificationDataParsed = parseNotificationData(notificationEvent.notification) as TaskAssignedNotificationData
        assertEquals(task1.id, notificationDataParsed.taskId)
        assertEquals(task1.name, notificationDataParsed.taskName)
        assertEquals(fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_ASSIGNED, notificationEvent.notification.type)
        assertEquals(false, notificationEvent.read)
        assertEquals(testUser, notificationEvent.receiverId)

        val notificationEventsForAdmin = tb.admin.notificationEvent.list(
            userId = adminUser.id!!,
            projectId = project1.id
        )
        assertEquals(1, notificationEventsForAdmin.size)
        assertEquals(adminUser.id, notificationEventsForAdmin[0].receiverId)

        // Test fails of access rights checks and not found
        tb.user.notificationEvent.assertListFailStatus(400, userId = testUser)
        tb.admin.notificationEvent.assertListFailStatus(404,
            userId = testUser,
            projectId = UUID.randomUUID()
        )

        // Cleanup
        tb.admin.notification.delete(notificationEvents[0].notification.id!!)
    }

    /**
     * Tests creating task status changed notification and task assigned notifications
     */
    @Test
    fun createNotificationTaskStatusChangedTset() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val testUser = tb.admin.user.create("test0", UserRole.USER).id!!

        val task1 = tb.admin.task.create( task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(testUser),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id!!
            )
        )
        tb.admin.task.update(
            taskId = task1.id!!,
            task = task1.copy(status = TaskStatus.IN_PROGRESS)
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = testUser,
            projectId = project1.id
        )
        assertEquals(2, notificationEvents.size)
        val taskUpdated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_STATUS_CHANGED }!!
        val notificationDataParsed = parseNotificationData(taskUpdated.notification) as TaskStatusChangesNotificationData
        assertEquals(false, taskUpdated.read)
        assertEquals(testUser, taskUpdated.receiverId)
        assertEquals(task1.id, notificationDataParsed.taskId)
        assertEquals(task1.name, notificationDataParsed.taskName)
        assertEquals(TaskStatus.IN_PROGRESS, notificationDataParsed.newStatus)

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.addClosable(it.notification) }
    }

    /**
     * Tests creating change proposal status changed notification
     */
    @Test
    fun createNotificationChangeProposalStatusChangedTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val testUser = tb.admin.user.create("test0", UserRole.USER).id!!

        val task1 = tb.admin.task.create(task = Task(
            name = "Task 1",
            startDate = "2024-01-01",
            endDate = "2024-01-02",
            assigneeIds = arrayOf(testUser),
            status = TaskStatus.NOT_STARTED,
            milestoneId = milestone1.id!!
        ))
        val changeProposal = tb.admin.changeProposal.create(
            taskId = task1.id!!
        )!!
        tb.admin.changeProposal.updateChangeProposal(
            changeProposalId = changeProposal.id!!,
            changeProposal = changeProposal.copy(status = fi.metatavu.lipsanen.test.client.models.ChangeProposalStatus.REJECTED)
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = testUser,
            projectId = project1.id
        )
        assertEquals(3, notificationEvents.size)
        // test that assignee received all the notifications
        val proposalCreated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.CHANGE_PROPOSAL_CREATED }!!
        val proposalCreatedDataParsed = parseNotificationData(proposalCreated.notification) as ChangeProposalCreatedNotificationData
        assertEquals(testUser, proposalCreated.receiverId)
        assertEquals(false, proposalCreated.read)
        assertEquals(task1.id, proposalCreatedDataParsed.taskId)
        assertEquals(changeProposal.id, proposalCreatedDataParsed.changeProposalId)

        val proposalUpdated = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.CHANGE_PROPOSAL_STATUS_CHANGED }!!
        val proposalUpdatedDataParsed = parseNotificationData(proposalCreated.notification) as ChangeProposalCreatedNotificationData
        assertEquals(false, proposalUpdated.read)
        assertEquals(testUser, proposalUpdated.receiverId)
        assertEquals(task1.id, proposalUpdatedDataParsed.taskId)
        assertEquals(changeProposal.id, proposalUpdatedDataParsed.changeProposalId)
        val taskAssigned = notificationEvents.find { it.notification.type == fi.metatavu.lipsanen.test.client.models.NotificationType.TASK_ASSIGNED }
        assertNotNull(taskAssigned)

        // Cleanup
        notificationEvents.forEach { tb.admin.notification.addClosable(it.notification) }
    }

    /**
     * Tests creating task assigned notification and updating it as read as well as dismissing it
     */
    @Test
    fun updateNotificationTaskAssignedTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)
        val testUser1 = tb.admin.user.create("test0", UserRole.USER).id!!

        tb.admin.task.create( task = Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(testUser1),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id!!
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
        notificationEvents.forEach { tb.admin.notification.addClosable(it.notification) }
    }

    /**
     * Tests that leaving a comment notifies task assignees, mentioned users and admin.
     * Test scenario:
     * Admin creates task assigned to user2 (who gets assigned to the project)
     * user leaves comment on task, mentions another user1 (who was manually assigned to the project).
     * Admin, user2 should get notifications, but not user1
     */
    @Test
    fun commentLeftTest() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)

        val user1 = tb.admin.user.create("user1", UserRole.USER)
        val user2 = tb.admin.user.create("user2", UserRole.USER)
        val admin1 = tb.admin.user.create("admin1", UserRole.ADMIN)

        //assign user2 to the project so that he can later see the comment he's referenced in
        tb.admin.user.updateUser(
            user2.id!!,
            user = user1.copy(projectIds = arrayOf(project1.id), roles = null)
        )
        tb.admin.user.updateUser(
            admin1.id!!,
            user = admin1.copy(projectIds = arrayOf(project1.id), roles = null)
        )

        //user gets automatically assigned to the project
        val task = tb.admin.task.create(task = Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(user1.id!!),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id!!
            )
        )

        // user 1 leaves a comment mentioning user 2
        val comment = tb.getUser("user1@example.com").taskComment.create(
            taskId = task.id!!,
            taskComment = TaskComment(
                comment = "Comment",
                referencedUsers = arrayOf(user2.id),
                taskId = task.id
            )
        )

        val userNotifications = tb.admin.notificationEvent.list(
            userId = user1.id,
            projectId = project1.id
        )
        assertEquals(1, userNotifications.size)
        assertTrue(userNotifications.any { it.notification.type == NotificationType.TASK_ASSIGNED })

        val adminNotifications = tb.admin.notificationEvent.list(
            userId = admin1.id,
            projectId = project1.id
        )
        assertEquals(2, adminNotifications.size)
        assertTrue(adminNotifications.any { it.notification.type == NotificationType.COMMENT_LEFT })
        assertTrue(adminNotifications.any { it.notification.type == NotificationType.TASK_ASSIGNED })

        val user1Notifications = tb.admin.notificationEvent.list(
            userId = admin1.id,
            projectId = project1.id
        )
        assertEquals(2, user1Notifications.size)
        assertTrue(user1Notifications.any { it.notification.type == NotificationType.TASK_ASSIGNED })
        assertTrue(user1Notifications.any { it.notification.type == NotificationType.COMMENT_LEFT })

        // Test that the comment notification data is correct
        val taskCommentLeftNotification = adminNotifications.find { it.notification.type == NotificationType.COMMENT_LEFT }!!
        val notificationDataParsed = parseNotificationData(taskCommentLeftNotification.notification) as CommentLeftNotificationData
        assertEquals(task.id, notificationDataParsed.taskId)
        assertEquals("Comment", notificationDataParsed.comment)
        assertEquals(task.name, notificationDataParsed.taskName)
        assertEquals(comment.id, notificationDataParsed.commentId)
    }

    /**
     * Parses notification data
     *
     * @param notification notification
     * @return parsed calss
     */
    fun parseNotificationData(notification: Notification): Any {
        val typeReference = when (notification.type) {
            NotificationType.TASK_ASSIGNED -> object: com.fasterxml.jackson.core.type.TypeReference<TaskAssignedNotificationData>() {}
            NotificationType.TASK_STATUS_CHANGED -> object: com.fasterxml.jackson.core.type.TypeReference<TaskStatusChangesNotificationData>() {}
            NotificationType.CHANGE_PROPOSAL_CREATED -> object: com.fasterxml.jackson.core.type.TypeReference<ChangeProposalCreatedNotificationData>() {}
            NotificationType.CHANGE_PROPOSAL_STATUS_CHANGED -> object: com.fasterxml.jackson.core.type.TypeReference<ChangeProposalStatusChangedNotificationData>() {}
            NotificationType.COMMENT_LEFT -> object: com.fasterxml.jackson.core.type.TypeReference<CommentLeftNotificationData>() {}
        }
        return jacksonObjectMapper().readValue(
            jacksonObjectMapper().writeValueAsString(notification.notificationData),
            typeReference
        )
    }

}