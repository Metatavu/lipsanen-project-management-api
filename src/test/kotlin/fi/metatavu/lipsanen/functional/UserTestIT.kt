package fi.metatavu.lipsanen.functional

import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Project
import fi.metatavu.lipsanen.test.client.models.ProjectStatus
import fi.metatavu.lipsanen.test.client.models.User
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured
import io.restassured.http.Method
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class UserTestIT: AbstractFunctionalTest() {

    @Test
    fun testCreateUser() = createTestBuilder().use {
        val project = it.admin.project.create()
        val createdCompany = it.admin.company.create()
        val userData = User(
            firstName = "Test",
            lastName = "User",
            email = "user1@example.com",
            projectIds = arrayOf(project.id!!),
            companyId = createdCompany.id,
            roles = emptyArray()
        )
        val createdUser = it.admin.user.create(userData)
        assertEquals(userData.firstName, createdUser.firstName)
        assertEquals(userData.lastName, createdUser.lastName)
        assertEquals(userData.email, createdUser.email)
        assertEquals(userData.companyId, createdUser.companyId)
        assertEquals(userData.projectIds!![0], createdUser.projectIds!![0])
        assertNotNull(createdUser.id)
        assertNull(createdUser.lastLoggedIn)

        val foundUser = it.admin.user.findUser(createdUser.id!!, true)
        assertEquals(fi.metatavu.lipsanen.test.client.models.UserRole.USER, foundUser.roles[0])

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

    @Test
    fun testListUsers() = createTestBuilder().use {
        val users = it.admin.user.listUsers(null, null, null, true)
        assertEquals(3, users.size)
        users.forEach { user ->
            assertTrue(fi.metatavu.lipsanen.test.client.models.UserRole.USER == user.roles[0] || fi.metatavu.lipsanen.test.client.models.UserRole.ADMIN == user.roles[0])
        }

        val pagedUsers = it.admin.user.listUsers(first = 0, max = 2)
        assertEquals(2, pagedUsers.size)

        val pagedUsers2 = it.admin.user.listUsers(first = 2, max = 10)
        assertEquals(2, pagedUsers2.size)

        val pagedUsers3 = it.admin.user.listUsers(first = 2, max = 3)
        assertEquals(2, pagedUsers3.size)

        val pagedUsers4 = it.admin.user.listUsers(first = 3, max = 2)
        assertEquals(1, pagedUsers4.size)

        // Filter by company
        val company = it.admin.company.create()
        it.admin.user.updateUser(users[0].id!!, users[0].copy(companyId = company.id))
        val companyUsers = it.admin.user.listUsers(companyId = company.id)
        assertEquals(1, companyUsers.size)
        it.admin.user.updateUser(users[0].id!!, users[0].copy(companyId = null))

        it.user.user.assertListFailStatus(403)
    }

    @Test
    fun testFindUser() = createTestBuilder().use {
        //Check that user with login history has some last login info
        val adminId = UUID.fromString("af6451b7-383c-4771-a0cb-bd16fae402c4")
        val foundUser = it.admin.user.findUser(adminId)
        assertNotNull(foundUser.lastLoggedIn)

        val foundUserWithRole = it.admin.user.findUser(adminId, true)
        assertEquals(fi.metatavu.lipsanen.test.client.models.UserRole.ADMIN, foundUserWithRole.roles[0])
    }

    @Test
    fun testFindUserFail() = createTestBuilder().use {
        val adminId = UUID.fromString("af6451b7-383c-4771-a0cb-bd16fae402c4")
        it.user.user.assertFindFailStatus(403, adminId)

        InvalidValueTestScenarioBuilder(
            path = "v1/users/{userId}",
            method = Method.GET,
            token = it.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "userId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun testUpdateUser() = createTestBuilder().use {
        val project = it.admin.project.create()
        val project2 = it.admin.project.create(Project("Project 2", status = ProjectStatus.PLANNING))
        val userData = User(
            firstName = "Test",
            lastName = "User",
            email = "user1@example.com",
            projectIds = arrayOf(project.id!!),
            roles = emptyArray()
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
            email = "user1@example.com",
            roles = emptyArray()
        )
        val createdUser = it.admin.user.create(userData)

        it.admin.user.deleteUser(createdUser.id!!)
        it.admin.user.assertFindFailStatus(404, createdUser.id)
    }

    @Test
    fun testDeleteUserFail() = createTestBuilder().use {
        val adminId = UUID.fromString("af6451b7-383c-4771-a0cb-bd16fae402c4")
        it.user.user.assertDeleteFailStatus(403, adminId)

        InvalidValueTestScenarioBuilder(
            path = "v1/users/{userId}",
            method = Method.DELETE,
            token = it.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "userId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

}