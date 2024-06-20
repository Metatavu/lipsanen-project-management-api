package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.NonTestEnvProfile
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

@QuarkusIntegrationTest
@TestProfile(NonTestEnvProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class NativeUserCreateTestIT: UserCreateTestIT() {
}