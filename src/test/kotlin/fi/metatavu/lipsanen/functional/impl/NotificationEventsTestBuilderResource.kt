package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.NotificationEventsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.NotificationEvent
import junit.framework.TestCase.fail
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test builder resource for notifications
 */
class NotificationEventsTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<NotificationEvent, NotificationEventsApi>(testBuilder, apiClient) {

    override fun clean(p0: NotificationEvent?) {
        p0?.let {
            api.deleteNotificationEvent(it.id!!)
        }
    }

    override fun getApi(): NotificationEventsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return NotificationEventsApi(ApiTestSettings.apiBasePath)
    }

    fun list(
        userId: UUID,
        projectId: UUID? = null,
        first: Int? = null,
        max: Int? = null,
        readStatus: Boolean? = null
    ): Array<NotificationEvent> {
        return api.listNotificationEvents(
            userId = userId,
            projectId = projectId,
            first = first,
            max = max,
            readStatus = readStatus
        )
    }

    fun find(
        notificationEventId: UUID
    ): NotificationEvent {
        return api.findNotificationEvent(notificationEventId)
    }

    fun updateNotification(
        notificationEventId: UUID,
        updateBody: NotificationEvent
    ): NotificationEvent {
        return api.updateNotificationEvent(
            notificationEventId,
            updateBody
        )
    }

    fun delete(
        notificationEventId: UUID
    ) {
        api.deleteNotificationEvent(notificationEventId)
    }

    fun assertListFailStatus(
        expectedStatus: Int,
        userId: UUID,
        projectId: UUID? = null,
        first: Int? = null,
        max: Int? = null,
        readStatus: Boolean? = null
    ) {
        try {
            api.listNotificationEvents(
                userId = userId,
                projectId = projectId,
                first = first,
                max = max,
                readStatus = readStatus
            )
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }
}