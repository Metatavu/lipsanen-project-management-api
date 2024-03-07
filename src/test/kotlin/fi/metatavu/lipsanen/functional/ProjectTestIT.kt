package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Project
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.common.mapper.TypeRef
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
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
        tb.user.project.create()
        val projects = tb.user.project.listProjects()
        assertNotNull(projects)
        assertEquals(1, projects.size)
    }

    @Test
    fun createProject() = createTestBuilder().use { tb ->
        val project = tb.user.project.create()
        assertNotNull(project)
    }

    @Test
    fun findProject() = createTestBuilder().use { tb ->
        val project = tb.user.project.create()
        val foundProject = tb.user.project.findProject(project.id!!)
        assertNotNull(foundProject)
    }

    @Test
    fun updateProject() = createTestBuilder().use { tb ->
        val project = tb.user.project.create()
        val updatedProject = tb.user.project.updateProject(project.id!!, Project("Updated project"))
        assertNotNull(updatedProject)
        assertEquals("Updated project", updatedProject.name)
    }

    @Test
    fun deleteProject() = createTestBuilder().use { tb ->
        val project = tb.user.project.create()
        tb.user.project.deleteProject(project.id!!)
        val projects = tb.user.project.listProjects()
        assertNotNull(projects)
        projects.forEach { println(it.id) }
        assertEquals(0, projects.size)
    }

    @Test
    fun exportProject() = createTestBuilder().use { tb ->
        val project = tb.user.project.create()
        val projectBytes = tb.user.project.exportProject(project.id!!)
        assertNotNull(projectBytes)
        assertTrue(
            projectBytes!!.toString(
                Charset.defaultCharset()
            ).contains("<Project><ID>0</ID><ProjName>Test project</ProjName><Created/><Changed/></Project>")
        )
    }

    @Test
    fun testImport(): Unit = createTestBuilder().use { tb ->
        this.javaClass.classLoader.getResourceAsStream("tocoman_project.xml").use {
            val parsed = Given {
                header("Authorization", "Bearer ${tb.user.accessTokenProvider.accessToken}")
                contentType("application/xml")
                body(it!!.readAllBytes())
            }.When { post("/v1/projects/import") }
                .then()
                .extract()
                .body().`as`(object : TypeRef<Project?>() {})
            assertNotNull(parsed)
            try {
                val projects = tb.user.project.listProjects()
                assertNotNull(projects)
                assertEquals(1, projects.size)
                assertNotNull(projects[0].id)
                assertNotNull(projects[0].tocomanId)
                assertNotNull(projects[0].name)
            } finally {
                tb.user.project.deleteProject(parsed!!.id!!)
            }

        }
    }
}