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
import fi.metatavu.lipsanen.test.client.models.UserRole
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Test class for user API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class UserTestIT: AbstractFunctionalTest() {

    @Test
    fun testListUsers() = createTestBuilder().use {
        val project1 = it.admin.project.create("project1").id
        val project2 = it.admin.project.create("project2").id
        val company = it.admin.company.create().id
        it.admin.user.create("user1", UserRole.USER, project1, company)
        it.admin.user.create("user2", UserRole.USER, project1)
        it.admin.user.create("user3", UserRole.USER, project2, company)
        it.admin.user.create("user4", UserRole.USER)

        val users = it.admin.user.listUsers(null, null, null)
        assertEquals(4, users.size)

        val pagedUsers = it.admin.user.listUsers(first = 0, max = 2)
        assertEquals(2, pagedUsers.size)

        val pagedUsers2 = it.admin.user.listUsers(first = 2, max = 10)
        assertEquals(2, pagedUsers2.size)

        val companyUsers = it.admin.user.listUsers(companyId = company)
        assertEquals(2, companyUsers.size)

        val projecUsers = it.admin.user.listUsers(projectId = project2)
        assertEquals(1, projecUsers.size)

        val projectCompanyUsers = it.admin.user.listUsers(projectId = project1, companyId = company)
        assertEquals(1, projectCompanyUsers.size)

        it.user.user.assertListFailStatus(403)
    }

    @Test
    fun testFindUser() = createTestBuilder().use {
        val user1 = it.admin.user.create("user1", UserRole.USER)
        it.getUser("user1@example.com").project.listProjects()

        //Check that user with login history has some last login info
        val foundUser2 = it.admin.user.findUser(user1.id!!)
        assertNotNull(foundUser2.lastLoggedIn)
    }

    @Test
    fun testFindUserFail() = createTestBuilder().use {
        it.user.user.assertFindFailStatus(403, UUID.randomUUID())

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
        val position = it.admin.jobPosition.create("a")
        val position2 = it.admin.jobPosition.create("b")
        val project = it.admin.project.create()
        val project2 = it.admin.project.create(Project("Project 2", status = ProjectStatus.PLANNING))
        val userData = User(
            firstName = "usertest",
            lastName = "usertest",
            email = "usertest@example.com",
            projectIds = arrayOf(project.id!!),
            roles = arrayOf(UserRole.USER),
            jobPositionId = position.id
        )
        val createdUser = it.admin.user.create(userData)

        val updatedUserData = createdUser.copy(
            firstName = "Updated",
            lastName = "User",
            projectIds = arrayOf(project2.id!!),
            roles = arrayOf(UserRole.ADMIN),
            jobPositionId = position2.id
        )

        it.admin.user.updateUser(createdUser.id!!, updatedUserData)
        val foundUser = it.admin.user.findUser(createdUser.id)

        assertEquals(updatedUserData.firstName, foundUser.firstName)
        assertEquals(updatedUserData.lastName, foundUser.lastName)
        assertEquals(updatedUserData.email, foundUser.email)
        assertEquals(updatedUserData.projectIds!![0], foundUser.projectIds!![0])
        assertEquals(updatedUserData.roles!![0], foundUser.roles!![0])
        assertEquals(updatedUserData.jobPositionId, foundUser.jobPositionId)

        it.admin.user.assertUpdateFailStatus(404, UUID.randomUUID(), updatedUserData)
        it.user.user.assertUpdateFailStatus(403, createdUser.id, updatedUserData)

        // cannot update with invalid project id
        it.admin.user.assertUpdateFailStatus(404, createdUser.id, updatedUserData.copy(projectIds = arrayOf(UUID.randomUUID())))
    }

    @Test
    fun testDeleteUser() = createTestBuilder().use {
        val userData = User(
            firstName = "usertest",
            lastName = "usertest",
            email = "usertest@example.com"
        )
        val createdUser = it.admin.user.create(userData)

        it.admin.user.deleteUser(createdUser.id!!)
        it.admin.user.assertFindFailStatus(404, createdUser.id)
    }

    @Test
    fun testDeleteUserFail() = createTestBuilder().use {
        it.user.user.assertDeleteFailStatus(403, UUID.randomUUID())

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