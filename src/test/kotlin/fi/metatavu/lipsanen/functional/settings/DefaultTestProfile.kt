package fi.metatavu.lipsanen.functional.settings

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Default test profile
 */
class DefaultTestProfile: QuarkusTestProfile {

    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "environment" to "test",
            "cron.notifications.cleanup" to "0 0 0 * * ?",
            "notifications.cleanup.delay.days" to "15",
            "lipsanen.keycloak.defaultRole" to "default-roles-lipsanen-user-management"
        )
    }
}

class NonTestEnvProfile: QuarkusTestProfile {

    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "cron.notifications.cleanup" to "0 0 0 * * ?",
            "notifications.cleanup.delay.days" to "15",
            "lipsanen.keycloak.defaultRole" to "default-roles-lipsanen-user-management"
        )
    }
}