package fi.metatavu.lipsanen.functional

import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.*
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
        val jobPosition1 = it.admin.jobPosition.create(JobPosition("architect"))

        val user1 = it.admin.user.create("user1", UserRole.USER, project1, company, jobPosition1.id)
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

        val jobPositionUsers = it.admin.user.listUsers(jobPositionId = jobPosition1.id)
        assertEquals(1, jobPositionUsers.size)

        val projectUsersListedByUser = it.getUser(user1.email).user.listUsers(projectId = project1)
        assertEquals(2, projectUsersListedByUser.size)

        val companyUsersListedByUser = it.getUser(user1.email).user.listUsers(companyId = company)
        assertEquals(1, companyUsersListedByUser.size)

        it.getUser(user1.email).user.assertListFailStatus(403, projectId = project2)
    }

    @Test
    fun testFindUser() = createTestBuilder().use {
        val project1 = it.admin.project.create("project 1").id
        val project2 = it.admin.project.create("project 2").id

        val user1 = it.admin.user.create("user1", UserRole.USER, projectId = project1)
        it.getUser("user1@example.com").project.listProjects()

        // Test that user can find itself
        val foundUser = it.getUser(user1.email).user.findUser(user1.id!!)
        assertNotNull(foundUser)

        val user2 = it.admin.user.create("user2", UserRole.USER, projectId = project1)
        val user3 = it.admin.user.create("user3", UserRole.USER, projectId = project2)
        // User 1 can find user 2 only since they share the project
        val foundSameProjectUser = it.getUser(user1.email).user.findUser(user2.id!!)
        assertNotNull(foundSameProjectUser)
        // User 1 cannot find user 3 since they do not share the project
        it.getUser(user1.email).user.assertFindFailStatus(404, user3.id!!)
    }

    @Test
    fun testFindUserFail() = createTestBuilder().use {
        it.user.user.assertFindFailStatus(404, UUID.randomUUID())

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
    fun testAdminUpdateUser() = createTestBuilder().use {
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

        // Check that null values are not updated
        val updateData2 = updatedUserData.copy(projectIds = null, roles = null)
        val afterSecondUpdate = it.admin.user.updateUser(createdUser.id, updateData2)
        assertEquals(foundUser.projectIds[0], afterSecondUpdate.projectIds!![0])
        assertEquals(foundUser.roles[0], afterSecondUpdate.roles!![0])

        it.admin.user.assertUpdateFailStatus(404, UUID.randomUUID(), updatedUserData)
        it.user.user.assertUpdateFailStatus(403, createdUser.id, updatedUserData)

        // cannot update with invalid project id
        it.admin.user.assertUpdateFailStatus(404, createdUser.id, updatedUserData.copy(projectIds = arrayOf(UUID.randomUUID())))
    }

    @Test
    fun testProjectOwnerUpdateUser() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val project2 = tb.admin.project.create(Project("Project 2", status = ProjectStatus.PLANNING))
        val project3 = tb.admin.project.create(Project("Project 3", status = ProjectStatus.PLANNING))

        val project2Owner = tb.admin.user.create("projectOwner", UserRole.PROJECT_OWNER, project2.id!!)

        val userProject1 = tb.admin.user.create("userProject1", UserRole.USER, project.id!!)

        // project owner cannot unassign users from projects he has no access to
        tb.getUser(project2Owner.email).user.assertUpdateFailStatus(403, userProject1.id!!, userProject1.copy(projectIds = arrayOf()))
        // project owner cannot assign users to projects he has no access to
        tb.getUser(project2Owner.email).user.assertUpdateFailStatus(403, userProject1.id, userProject1.copy(projectIds = arrayOf(project3.id!!)))
        // project owner can assign and unassign users to projects he has access to
        var updatedUser = tb.getUser(project2Owner.email).user.updateUser(userProject1.id, userProject1.copy(projectIds = arrayOf(project.id, project2.id)))
        assertEquals(2, updatedUser.projectIds!!.size)
        updatedUser = tb.getUser(project2Owner.email).user.updateUser(userProject1.id, userProject1.copy(projectIds = arrayOf(project.id)))
        assertEquals(1, updatedUser.projectIds!!.size)
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