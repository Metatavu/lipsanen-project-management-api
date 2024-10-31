package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.spec.NotificationsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

/**
 * Notifications API implementation (only for testing purposes)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
class NotificationsApiImpl : NotificationsApi, AbstractApi() {

    @ConfigProperty(name = "environment")
    lateinit var environment: Optional<String>

    @Inject
    lateinit var notificationsController: NotificationsController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteNotification(notificationId: UUID): Uni<Response> = withCoroutineScope {
        if (environment.isPresent && environment.get() == "test") {
            return@withCoroutineScope createForbidden("Deleting notifications is not allowed out of test environment")
        }
        val notification = notificationsController.find(notificationId) ?: return@withCoroutineScope createNotFound("Notification not found")
        notificationsController.delete(notification)
        return@withCoroutineScope createNoContent()
    }
}