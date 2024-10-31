package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.AttachmentsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.Attachment
import fi.metatavu.lipsanen.test.client.models.Project
import fi.metatavu.lipsanen.test.client.models.Task
import junit.framework.TestCase.fail
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test builder resource for attachments
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class AttachmentTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Attachment, AttachmentsApi>(testBuilder, apiClient) {

    override fun clean(p0: Attachment?) {
        p0?.id?.let {
            try {
                api.deleteAttachment(it)
            } catch (_: Exception) {}
        }
    }

    override fun getApi(): AttachmentsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return AttachmentsApi(ApiTestSettings.apiBasePath)
    }

    fun create(project: Project, projectTask: Task?): Attachment? {
        return create(
            Attachment(
                projectId = project.id!!,
                taskId = projectTask?.id,
                url = "https://example.com",
                type = "image",
                name = "example.jpg"
            )
        )
    }

    fun create(attachment: Attachment): Attachment? {
        return addClosable(api.createAttachment(attachment))
    }

    fun assertCreateFail(
        expectedStatus: Int,
        companyData: Attachment
    ) {
        try {
            api.createAttachment(companyData)
            fail(String.format("Expected create to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun find(id: UUID): Attachment {
        return api.findAttachment(id)
    }

    fun assertFindFail(
        expectedStatus: Int,
        id: UUID
    ) {
        try {
            api.findAttachment(id)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun update(id: UUID, updateData: Attachment): Attachment {
        return api.updateAttachment(id, updateData)
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        id: UUID,
        updateData: Attachment
    ) {
        try {
            api.updateAttachment(id, updateData)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun list(
        projectId: UUID? = null,
        taskId: UUID? = null,
        first: Int? = null,
        max: Int? = null
    ): Array<Attachment> {
        return api.listAttachments(projectId = projectId, taskId = taskId, first = first, max = max)
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID? = null,
        taskId: UUID? = null,
        first: Int? = null,
        max: Int? = null
    ) {
        try {
            api.listAttachments(projectId, taskId, first = first, max = max)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }


    fun delete(id: UUID) {
        api.deleteAttachment(id)
        removeCloseable { closable: Any ->
            if (closable !is Attachment) {
                return@removeCloseable false
            }
            closable.id == id
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        id: UUID
    ) {
        try {
            api.deleteAttachment(id)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }
}