package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.NotificationsTestProfile
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

/**
 * Native tests for Notifications cleanup
 */
@QuarkusIntegrationTest
@TestProfile(NotificationsTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class NativeNotificationsCleanupTestIT : NotificationsCleanupTestIT()
