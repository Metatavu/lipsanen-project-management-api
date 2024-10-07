package fi.metatavu.lipsanen.functional

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Project
import fi.metatavu.lipsanen.test.client.models.ProjectStatus
import fi.metatavu.lipsanen.test.client.models.UserRole
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.jose4j.jwk.Use
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

/**
 * Tests for Project API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class ProjectTestIT : AbstractFunctionalTest() {

    @Test
    fun listProjects() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create(Project("Project 1", status = ProjectStatus.PLANNING))
        val project2 = tb.admin.project.create(Project("Project 2", status = ProjectStatus.PLANNING))
        val projects = tb.admin.project.listProjects()
        assertNotNull(projects)
        assertEquals(2, projects.size)

        // access rights
        val user1 = tb.admin.user.create("test1", UserRole.USER)
        tb.admin.user.updateUser(
            user1.id!!,
            user = user1.copy(projectIds = arrayOf(project1.id!!), roles = null)
        )
        val user2 = tb.admin.user.create("test2", UserRole.USER)

        val user1Projects = tb.getUser("test1@example.com").project.listProjects()
        assertNotNull(user1Projects)
        assertEquals(1, user1Projects.size)

        val user2Projects = tb.getUser("test2@example.com").project.listProjects()
        assertNotNull(user2Projects)
        assertEquals(0, user2Projects.size)
    }

    @Test
    fun createProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        assertNotNull(project)
    }

    @Test
    fun findProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val foundProject = tb.admin.project.findProject(project.id!!)
        assertNotNull(foundProject)

        // access rights
        val user1 = tb.admin.user.create("test1", UserRole.USER)
        val updatedUser1 = tb.admin.user.updateUser(
            user1.id!!,
            user = user1.copy(projectIds = arrayOf(foundProject.id!!), roles = null)
        )
        assertNotNull(updatedUser1)
        assertEquals(1, updatedUser1.projectIds!!.size)
        assertNotNull(tb.getUser("test1@example.com").project.findProject(project.id))
        assertEquals(project.status, ProjectStatus.INITIATION)

        // project owner rights
        val owner = tb.admin.user.create("owner", UserRole.PROJECT_OWNER)
        tb.getUser(owner.email).project.assertFindFail(403, project.id)
        tb.admin.user.updateUser(
            owner.id!!,
            user = owner.copy(projectIds = arrayOf(foundProject.id), roles = null)
        )
        assertNotNull(tb.getUser(owner.email).project.findProject(project.id))
    }

    @Test
    fun findProjectFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        tb.user.project.assertFindFail(403, project.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun updateProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val newProjectStatus = ProjectStatus.PLANNING
        val updateData = project.copy(
            name = "Updated project",
            status = newProjectStatus,
            estimatedStartDate = "2022-01-01",
            estimatedEndDate = "2022-12-31"
        )
        val updatedProject = tb.admin.project.updateProject(project.id!!, updateData)
        assertNotNull(updatedProject)
        assertEquals("Updated project", updatedProject.name)
        assertEquals(newProjectStatus, updatedProject.status)
        assertEquals("2022-01-01", updatedProject.estimatedStartDate)
        assertEquals("2022-12-31", updatedProject.estimatedEndDate)
        tb.user.project.assertUpdateFail(403, project.id, updatedProject)
    }

    @Test
    fun updateProjectFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(project)
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun deleteProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()

        tb.user.project.assertDeleteFail(403, project.id!!)
        tb.admin.project.deleteProject(project.id)

        val projects = tb.admin.project.listProjects()
        assertNotNull(projects)
        assertEquals(0, projects.size)
    }

    @Test
    fun deleteProjectFail() = createTestBuilder().use { tb ->
        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun exportProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val projectBytes = tb.admin.project.exportProject(project.id!!)
        assertNotNull(projectBytes)
        assertTrue(
            projectBytes!!.toString(
                Charset.defaultCharset()
            ).contains("<Project><ID>0</ID><ProjName>Test project</ProjName><Created/><Changed/></Project>")
        )
    }

    @Test
    fun testImport(): Unit = createTestBuilder().use { tb ->
        tb.admin.user.create("test1", UserRole.ADMIN)
        val imported1 = tb.getUser("test1@example.com").project.importProject("tocoman_project.xml")
        assertNotNull(imported1)

        var project = tb.getUser("test1@example.com").project.findProject(imported1.id!!)
        assertEquals(1, project.tocomanId)
        assertEquals("As.Oy Esimerkki", project.name)

        val imported2 = tb.getUser("test1@example.com").project.importProject("tocoman_project2.xml")
        project = tb.admin.project.findProject(imported2.id!!)
        assertEquals(1, project.tocomanId)
        assertEquals("Project 2", project.name)
    }
}