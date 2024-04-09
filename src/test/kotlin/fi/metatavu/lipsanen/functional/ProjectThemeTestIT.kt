package fi.metatavu.lipsanen.functional

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.invalid.providers.SimpleInvalidValueProvider
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.ProjectTheme
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for Project theme API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class ProjectThemeTestIT : AbstractFunctionalTest() {

    @Test
    fun listProjectThemes() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create("Project 1")
        val project2 = tb.admin.project.create("Project 2")

        val themeData = ProjectTheme(
            logoUrl = "https://example.com/logo.png",
            themeColor = "#000000"
        )
        tb.admin.projectTheme.create(project1.id!!, themeData)
        tb.admin.projectTheme.create(project2.id!!, themeData)
        tb.admin.projectTheme.create(project2.id, themeData)

        val project1Themes = tb.admin.projectTheme.listProjectThemes(project1.id)
        assertNotNull(project1Themes)
        assertEquals(1, project1Themes.size)
        val project2Themes = tb.admin.projectTheme.listProjectThemes(project2.id)
        assertNotNull(project2Themes)
        assertEquals(2, project2Themes.size)
    }

    @Test
    fun listProjectThemeFail() = createTestBuilder().use { tb ->
        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/themes",
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
    fun createProjectTheme() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val themeData = ProjectTheme(
            logoUrl = "https://example.com/logo.png",
            themeColor = "#000000"
        )
        val projectTheme = tb.admin.projectTheme.create(project.id!!, themeData)!!

        assertNotNull(projectTheme)
        assertEquals(themeData.logoUrl, projectTheme.logoUrl)
        assertEquals(themeData.themeColor, projectTheme.themeColor)
        assertNotNull(projectTheme.id)
        assertNotNull(projectTheme.metadata)
    }

    @Test
    fun createProjectThemeFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val themeData = ProjectTheme(
            logoUrl = "https://example.com/logo.png",
            themeColor = "#000000"
        )
        tb.user.projectTheme.assertCreateFail(403, project.id!!, themeData)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/themes",
            method = Method.POST,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(themeData)
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
    fun findProjectTheme() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create("Project 1")
        val theme = tb.admin.projectTheme.create(
            project.id!!,
            ProjectTheme(
                logoUrl = "https://example.com/logo.png",
                themeColor = "#000000"
            )
        )

        val foundTheme = tb.admin.projectTheme.findProjectTheme(project.id, theme!!.id!!)!!
        assertNotNull(foundTheme)
        assertEquals(theme.id, foundTheme.id)
        assertEquals(theme.logoUrl, foundTheme.logoUrl)
        assertEquals(theme.themeColor, foundTheme.themeColor)
    }

    @Test
    fun findProjectThemeFail() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create("Project 1")
        val project2 = tb.admin.project.create("Project 2")

        val project1Theme = tb.admin.projectTheme.create(project1.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/themes/{themeId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL.plus(SimpleInvalidValueProvider(project2.id.toString())),
                    default = project1.id.toString(),
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "themeId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = project1Theme!!.id.toString(),
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun updateProjectTheme() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val projectTheme = tb.admin.projectTheme.create(
            project.id!!,
            ProjectTheme(
                logoUrl = "https://example.com/logo.png",
                themeColor = "#000000"
            )
        )

        val updateData = ProjectTheme(
            logoUrl = "https://example.com/logo2.png",
            themeColor = "#FFFFFF"
        )

        val updatedProjectTheme = tb.admin.projectTheme.updateProjectTheme(project.id, projectTheme!!.id!!, updateData)
        assertNotNull(updatedProjectTheme)
        assertEquals(updateData.logoUrl, updatedProjectTheme.logoUrl)
        assertEquals(updateData.themeColor, updatedProjectTheme.themeColor)
    }

    @Test
    fun updateProjectThemeFail() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create("Project 1")
        val project2 = tb.admin.project.create("Project 2")

        val project1Theme = tb.admin.projectTheme.create(project1.id!!)

        //access rights
        tb.user.projectTheme.assertUpdateFail(403, project1.id, project1Theme!!.id!!, project1Theme)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/themes/{themeId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(project1Theme)  //any body is valid
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL.plus(SimpleInvalidValueProvider(project2.id.toString())),
                    default = project1.id.toString(),
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "themeId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = project1Theme.id.toString(),
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun deleteProjectTheme() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val project1Theme = tb.admin.projectTheme.create(
            project.id!!,
            ProjectTheme(
                logoUrl = "https://example.com/logo.png",
                themeColor = "#000000"
            )
        )

        tb.admin.projectTheme.deleteTheme(project1Theme!!.id!!)
        tb.admin.projectTheme.assertFindFail(404, project.id, project1Theme.id!!)
        val projectThemes = tb.admin.projectTheme.listProjectThemes(project.id)
        assertEquals(0, projectThemes.size)

        // check that project deletion removes its theme
        val project2 = tb.admin.project.create("Project 2")
        val project1Theme2 = tb.admin.projectTheme.create(
            project2.id!!,
            project1Theme
        )
        tb.admin.project.deleteProject(project2.id)
        tb.admin.projectTheme.assertFindFail(404, project2.id, project1Theme2!!.id!!)
        // Remove auto-removed theme from closables
        tb.admin.projectTheme.removeCloseable {
            return@removeCloseable it is ProjectTheme && it.id == project1Theme2.id
        }
    }

    @Test
    fun deleteProjectThemeFail() = createTestBuilder().use { tb ->
        val project1 = tb.admin.project.create("Project 1")
        val project2 = tb.admin.project.create("Project 2")

        val project1Theme = tb.admin.projectTheme.create(project1.id!!)

        tb.user.projectTheme.assertDeleteFail(403, project1.id, project1Theme!!.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/themes/{themeId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL.plus(SimpleInvalidValueProvider(project2.id.toString())),
                    default = project1.id.toString(),
                    expectedStatus = 404
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "themeId",
                    values = InvalidValues.STRING_NOT_NULL,
                    default = project1Theme.id.toString(),
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }


}