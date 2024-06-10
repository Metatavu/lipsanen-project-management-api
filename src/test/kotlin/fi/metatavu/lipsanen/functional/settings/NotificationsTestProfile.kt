package fi.metatavu.lipsanen.functional.settings

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Test profile for frequent cleaning of notifications
 */
class NotificationsTestProfile: QuarkusTestProfile {

    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "environment" to "test",
            "cron.notifications.cleanup" to " 0/30 * * * * ?",
            "notifications.cleanup.delay.days" to "0"
        )
    }
}