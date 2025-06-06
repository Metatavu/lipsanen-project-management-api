package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.NotificationsTestProfile
import fi.metatavu.lipsanen.test.client.models.Task
import fi.metatavu.lipsanen.test.client.models.TaskStatus
import fi.metatavu.lipsanen.test.client.models.UserRole
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Tests for Notifications Cleanup
 */
@QuarkusTest
@TestProfile(NotificationsTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class NotificationsCleanupTestIT : AbstractFunctionalTest() {

    /**
     * Tests cleanup of notifications
     */
    @Test
    fun cleanupNotifications() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project1.id!!)

        val user = tb.admin.user.create("user1", UserRole.USER)
        val admin = tb.admin.user.create("admin1", UserRole.ADMIN)

        // create task assigned notifications
        tb.admin.task.create(task =
            Task(
                name = "Task 1",
                startDate = "2024-01-01",
                endDate = "2024-01-02",
                assigneeIds = arrayOf(user.id!!),
                status = TaskStatus.NOT_STARTED,
                milestoneId = milestone1.id!!
            )
        )

        val notificationEvents = tb.admin.notificationEvent.list(
            userId = user.id,
            projectId = project1.id
        )
        assertEquals(1, notificationEvents.size)
        val notificationEvents1 = tb.admin.notificationEvent.list(
            userId = admin.id!!,
            projectId = project1.id
        )
        assertEquals(1, notificationEvents1.size)

        // wait for cleanup
        Awaitility.await().atMost(60, TimeUnit.SECONDS)
            .pollInterval(Duration.ofSeconds(3)).until {
                val adminAfterCleanup = tb.admin.notificationEvent.list(
                    userId = admin.id,
                    projectId = project1.id
                )
                val userAfterCleanup = tb.admin.notificationEvent.list(
                    userId = user.id,
                    projectId = project1.id
                )
                adminAfterCleanup.isEmpty() && userAfterCleanup.isEmpty()
            }
    }

}