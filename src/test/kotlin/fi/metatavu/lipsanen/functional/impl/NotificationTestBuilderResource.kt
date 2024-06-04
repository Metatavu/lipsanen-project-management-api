package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.NotificationsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.Notification
import java.util.*

/**
 * Test builder resource for notifications
 */
class NotificationTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Notification, NotificationsApi>(testBuilder, apiClient) {

    override fun clean(p0: Notification?) {
        // not created via api -> nothing to clean
    }

    override fun getApi(): NotificationsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return NotificationsApi(ApiTestSettings.apiBasePath)
    }

    fun delete(id: UUID) {
        try {
            api.deleteNotification(id)
        } catch (e: ClientException) {
            //do nothing
        }
    }

}