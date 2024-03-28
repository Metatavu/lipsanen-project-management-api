package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Project
import fi.metatavu.lipsanen.test.client.models.User
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
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
        val project1 = tb.admin.project.create(Project("Project 1"))
        val project2 = tb.admin.project.create(Project("Project 2"))
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

        tb.user2.project.assertFindFail(403, project.id)
    }

    @Test
    fun updateProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val updatedProject = tb.admin.project.updateProject(project.id!!, Project("Updated project"))
        assertNotNull(updatedProject)
        assertEquals("Updated project", updatedProject.name)

        tb.user.project.assertUpdateFail(403, project.id, Project("Updated project"))
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