package fi.metatavu.lipsanen.functional.settings

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Default test profile
 */
class DefaultTestProfile: QuarkusTestProfile {

    override fun getConfigOverrides(): MutableMap<String, String> {
        val config: MutableMap<String, String> = HashMap()
        return config
    }
}