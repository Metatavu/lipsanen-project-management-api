package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

/**
 * Native tests for Project API
 */
@QuarkusIntegrationTest
@TestProfile(DefaultTestProfile::class)
class NativeProjectTestIT: ProjectTestIT()
