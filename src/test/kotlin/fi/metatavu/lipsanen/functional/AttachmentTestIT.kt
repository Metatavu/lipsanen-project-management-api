package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Attachment
import fi.metatavu.lipsanen.test.client.models.UserRole
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class AttachmentTestIT : AbstractFunctionalTest() {

    @Test
    fun testCreateAttachment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create("project1")
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val createdTask = tb.admin.task.create(milestoneId = milestone.id!!)

        val attachmentData = Attachment(
            projectId = project.id,
            taskId = createdTask.id,
            type = "image",
            name = "image.jpg",
            url = "https://example.com/image.jpg"
        )

        val createdAttachment = tb.admin.attachment.create(attachmentData)
        assertNotNull(createdAttachment!!)
        assertEquals(attachmentData.projectId, createdAttachment.projectId)
        assertEquals(attachmentData.taskId, createdAttachment.taskId)
        assertEquals(attachmentData.type, createdAttachment.type)
        assertEquals(attachmentData.name, createdAttachment.name)
        assertEquals(attachmentData.url, createdAttachment.url)

        // Access rights checks
        val project2 = tb.admin.project.create("project2")
        val user2 = tb.admin.user.create("user2", UserRole.USER, project2.id)

        tb.getUser(user2.email).attachment.assertCreateFail(403, attachmentData)

        // Invalid data checks
        tb.admin.attachment.assertCreateFail(
            400,
            Attachment(
                projectId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                type = "image",
                name = "image.jpg",
                url = "https://example.com/image.jpg"
            )
        )
        tb.admin.attachment.assertCreateFail(
            400,
            Attachment(
                projectId = project.id,
                taskId = UUID.randomUUID(),
                type = "image",
                name = "image.jpg",
                url = "https://example.com/image.jpg"
            )
        )
        tb.admin.attachment.assertCreateFail(
            400,
            Attachment(
                projectId = project2.id!!,
                taskId = createdTask.id,
                type = "image",
                name = "image.jpg",
                url = "https://example.com/image.jpg"
            )
        )
    }

    @Test
    fun testListAttachments() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create("project1")
        val user = tb.admin.user.create("user1", UserRole.USER, project.id)
        val project2 = tb.admin.project.create("project2")
        val proectMilestone = tb.admin.milestone.create(projectId = project.id!!)
        val projectTask = tb.admin.task.create(milestoneId = proectMilestone.id!!)

        tb.admin.attachment.create(project, projectTask)
        tb.admin.attachment.create(project, projectTask)
        tb.admin.attachment.create(project, null)
        tb.admin.attachment.create(project2, null)

        val allAttachmentsByAdmin = tb.admin.attachment.list()
        assertEquals(4, allAttachmentsByAdmin.size)

        val allAttachmentsByUser = tb.getUser(user.email).attachment.list()
        assertEquals(3, allAttachmentsByUser.size)

        val projectAttachments = tb.admin.attachment.list(project.id)
        assertEquals(3, projectAttachments.size)

        val taskAttachments = tb.admin.attachment.list(project.id, projectTask.id)
        assertEquals(2, taskAttachments.size)

        tb.admin.attachment.assertListFail(404, UUID.randomUUID())
        tb.admin.attachment.assertListFail(404, project.id, UUID.randomUUID())
    }

    @Test
    fun testFindAttachment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create("project1")
        val user = tb.admin.user.create("user1", UserRole.USER, project.id)

        val attachment = tb.admin.attachment.create(project, null)
        val foundAttachment = tb.admin.attachment.find(attachment!!.id!!)
        assertNotNull(foundAttachment)

        assertNotNull(tb.getUser(user.email).attachment.find(attachment.id!!))
        val user1 = tb.admin.user.create("user2", UserRole.USER)
        tb.getUser(user1.email).attachment.assertFindFail(403, attachment.id)
        tb.admin.attachment.assertFindFail(404, UUID.randomUUID())
    }

    @Test
    fun testUpdateAttachment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create("project1")
        val project2 = tb.admin.project.create("project2")
        val task2 = tb.admin.task.create(milestoneId = tb.admin.milestone.create(projectId = project2.id!!).id!!)

        val attachment = tb.admin.attachment.create(project, null)
        val updateData = Attachment(
            projectId = project2.id,
            taskId = task2.id,
            type = "image",
            name = "image.jpg",
            url = "https://example.com/image.jpg"
        )
        val updated = tb.admin.attachment.update(attachment!!.id!!, updateData)
        assertNotNull(updated)
        assertEquals(updateData.projectId, updated.projectId)
        assertEquals(updateData.taskId, updated.taskId)
        assertEquals(updateData.type, updated.type)
        assertEquals(updateData.name, updated.name)
        assertEquals(updateData.url, updated.url)

        // access rights checks
        val user1 = tb.admin.user.create("user2", UserRole.USER, project.id)
        tb.getUser(user1.email).attachment.assertUpdateFail(403, attachment.id!!, updateData)

        // invalid data checks
        tb.admin.attachment.assertUpdateFail(400, UUID.randomUUID(), updateData)
        tb.admin.attachment.assertUpdateFail(
            400,
            attachment.id,
            Attachment(
                projectId = UUID.randomUUID(),
                taskId = UUID.randomUUID(),
                type = "image",
                name = "image.jpg",
                url = "https://example.com/image.jpg"
            )
        )
    }

    @Test
    fun testDeleteAttachment() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create("project1")
        val anotherProject = tb.admin.project.create("project2")
        val anotherProjectUser = tb.admin.user.create("user2", UserRole.USER, anotherProject.id)

        val project1Attachment = tb.admin.attachment.create(project, null)

        // Access rights checks
        tb.getUser(anotherProjectUser.email).attachment.assertDeleteFail(403, project1Attachment!!.id!!)

        // Invalid data checks
        tb.admin.attachment.assertDeleteFail(404, UUID.randomUUID())

        tb.admin.attachment.delete(project1Attachment.id!!)
        tb.admin.attachment.assertFindFail(404, project1Attachment.id!!)

    }
}