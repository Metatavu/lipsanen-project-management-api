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
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
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

        // assign user to project
        val user = tb.admin.user.listUsers().find { it.firstName == "user"}
        tb.admin.user.updateUser(
            user!!.id!!,
            user = user.copy(projectIds = arrayOf(project1.id!!))
        )

        val user1Projects = tb.user.project.listProjects()
        assertNotNull(user1Projects)
        assertEquals(1, user1Projects.size)

        val user2Projects = tb.user2.project.listProjects()
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

        // assign user to project
        val user1 = tb.admin.user.listUsers().find { it.firstName == "user"}
        val updatedUser1 = tb.admin.user.updateUser(
            user1!!.id!!,
            user = user1.copy(projectIds = arrayOf(project.id))
        )
        assertNotNull(updatedUser1)
        assertEquals(1, updatedUser1.projectIds!!.size)
        assertNotNull(tb.user.project.findProject(project.id))
        assertEquals(project.status, ProjectStatus.INITIATION)
    }

    @Test
    fun findProjectFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        tb.user2.project.assertFindFail(403, project.id!!)

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
        val updatedProject = tb.admin.project.updateProject(project.id!!, Project("Updated project", status = newProjectStatus))
        assertNotNull(updatedProject)
        assertEquals("Updated project", updatedProject.name)
        assertEquals(newProjectStatus, updatedProject.status)
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
        val imported1 = tb.admin.project.importProject("tocoman_project.xml")
        assertNotNull(imported1)

        var project = tb.admin.project.findProject(imported1.id!!)
        assertEquals(1, project.tocomanId)
        assertEquals("As.Oy Esimerkki",project.name)

        val imported2 = tb.admin.project.importProject("tocoman_project2.xml")
        project = tb.admin.project.findProject(imported2.id!!)
        assertEquals(1, project.tocomanId)
        assertEquals("Project 2", project.name)
    }
}