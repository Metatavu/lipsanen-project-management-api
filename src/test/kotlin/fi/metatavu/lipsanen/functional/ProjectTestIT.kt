package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Project
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
class ProjectTestIT : AbstractFunctionalTest() {

    @Test
    fun listProjects() = createTestBuilder().use { tb ->
        tb.admin.project.create()
        val projects = tb.admin.project.listProjects()
        assertNotNull(projects)
        assertEquals(1, projects.size)
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
    }

    @Test
    fun updateProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val updatedProject = tb.admin.project.updateProject(project.id!!, Project("Updated project"))
        assertNotNull(updatedProject)
        assertEquals("Updated project", updatedProject.name)
    }

    @Test
    fun deleteProject() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        tb.admin.project.deleteProject(project.id!!)
        val projects = tb.admin.project.listProjects()
        assertNotNull(projects)
        projects.forEach { println(it.id) }
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

        var project = tb.admin.project.findProject(imported1!!.id!!)
        assertEquals(1, project.tocomanId)
        assertEquals("As.Oy Esimerkki",project.name)

        val imported2 = tb.admin.project.importProject("tocoman_project2.xml")
        project = tb.admin.project.findProject(imported2!!.id!!)
        assertEquals(1, project.tocomanId)
        assertEquals("Project 2", project.name)
    }
}