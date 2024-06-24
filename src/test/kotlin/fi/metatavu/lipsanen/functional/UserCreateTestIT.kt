package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.NonTestEnvProfile
import fi.metatavu.lipsanen.test.client.models.User
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for user creation
 */
@QuarkusTest
@TestProfile(NonTestEnvProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class UserCreateTestIT: AbstractFunctionalTest() {

    @Test
    fun testCreateUser() = createTestBuilder().use {
        val jobPosition = it.admin.jobPosition.create("Test")
        val project = it.admin.project.create()
        val createdCompany = it.admin.company.create()
        val userData = User(
            firstName = "usertest",
            lastName = "usertest",
            email = "usertest@example.com",
            projectIds = arrayOf(project.id!!),
            companyId = createdCompany.id,
            jobPositionId = jobPosition.id
        )
        val createdUser = it.admin.user.create(userData)
        assertEquals(userData.firstName, createdUser.firstName)
        assertEquals(userData.lastName, createdUser.lastName)
        assertEquals(userData.email, createdUser.email)
        assertEquals(userData.companyId, createdUser.companyId)
        assertEquals(userData.projectIds!![0], createdUser.projectIds!![0])
        assertEquals(userData.jobPositionId, createdUser.jobPositionId)
        assertNotNull(createdUser.id)
        assertNull(createdUser.lastLoggedIn)

        val smptTestPort = ConfigProvider.getConfig().getValue("lipsanen.smtp.http.test-port", Int::class.java)
        val smtpTestHost = ConfigProvider.getConfig().getValue("lipsanen.smtp.http.test-host", String::class.java)

        RestAssured.baseURI = "http://$smtpTestHost"
        RestAssured.port = smptTestPort
        RestAssured.basePath = "/api/v2"

        val messages = RestAssured.given().`when`().get("/messages")
            .then().extract().body().asString()

        assertEquals(
            true,
            messages.contains("Update Password. Click on the link below to start this process")
        )

        //cannot create user with same email
        it.admin.user.assertCreateFailStatus(409, userData)
        it.user.user.assertCreateFailStatus(403, userData.copy(email = "newemail"))

        it.admin.user.assertCreateFailStatus(404, userData.copy(projectIds = arrayOf(UUID.randomUUID()), email = "newemail@example.com"))
        it.admin.user.assertCreateFailStatus(404, userData.copy(companyId = UUID.randomUUID(), email = "newemail@example.com"))
    }

}