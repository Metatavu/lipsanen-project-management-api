package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Project
import fi.metatavu.lipsanen.test.client.models.User
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class UserTestIT: AbstractFunctionalTest() {

    @Test
    fun testCreateUser() = createTestBuilder().use {
        val project = it.admin.project.create()
        val userData = User(
            firstName = "Test",
            lastName = "User",
            email = "user1@example.com",
            projectIds = arrayOf(project.id!!)
        )
        val createdUser = it.admin.user.create(userData)
        assertEquals(userData.firstName, createdUser.firstName)
        assertEquals(userData.lastName, createdUser.lastName)
        assertEquals(userData.email, createdUser.email)
        assertEquals(userData.projectIds!![0], createdUser.projectIds!![0])
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
    }

    @Test
    fun testListUsers() = createTestBuilder().use {
        val users = it.admin.user.listUsers()
        assertEquals(4, users.size)

        val pagedUsers = it.admin.user.listUsers(first = 0, max = 2)
        assertEquals(2, pagedUsers.size)

        val pagedUsers2 = it.admin.user.listUsers(first = 2, max = 10)
        assertEquals(1, pagedUsers2.size)

        it.user.user.assertListFailStatus(403)
    }

    @Test
    fun testFindUser() = createTestBuilder().use {
        //Check that user with login history has some last login info
        val adminId = UUID.fromString("af6451b7-383c-4771-a0cb-bd16fae402c4")
        val foundUser = it.admin.user.findUser(adminId)
        assertNotNull(foundUser.lastLoggedIn)

        it.admin.user.assertFindFailStatus(404, UUID.randomUUID())
        it.user.user.assertFindFailStatus(403, adminId)
    }

    @Test
    fun testUpdateUser() = createTestBuilder().use {
        val project = it.admin.project.create()
        val project2 = it.admin.project.create(Project("Project 2"))
        val userData = User(
            firstName = "Test",
            lastName = "User",
            email = "user1@example.com",
            projectIds = arrayOf(project.id!!)
        )
        val createdUser = it.admin.user.create(userData)

        val updatedUserData = createdUser.copy(
            firstName = "Updated",
            lastName = "User",
            projectIds = arrayOf(project2.id!!)
        )

        val updatedUser = it.admin.user.updateUser(createdUser.id!!, updatedUserData)
        assertEquals(updatedUserData.firstName, updatedUser.firstName)
        assertEquals(updatedUserData.lastName, updatedUser.lastName)
        assertEquals(updatedUserData.email, updatedUser.email)
        assertEquals(updatedUserData.projectIds!![0], updatedUser.projectIds!![0])

        it.admin.user.assertUpdateFailStatus(404, UUID.randomUUID(), updatedUserData)
        it.user.user.assertUpdateFailStatus(403, createdUser.id, updatedUserData)

        // cannot update with invalid project id
        it.admin.user.assertUpdateFailStatus(404, createdUser.id, updatedUserData.copy(projectIds = arrayOf(UUID.randomUUID())))
    }

    @Test
    fun testDeleteUser() = createTestBuilder().use {
        val userData = User(
            firstName = "Test",
            lastName = "User",
            email = "user1@example.com"
        )
        val createdUser = it.admin.user.create(userData)

        //access rights
        it.user.user.assertDeleteFailStatus(403, createdUser.id!!)

        it.admin.user.deleteUser(createdUser.id!!)
        it.admin.user.assertFindFailStatus(404, createdUser.id)

        it.admin.user.assertDeleteFailStatus(404, UUID.randomUUID())
    }

}