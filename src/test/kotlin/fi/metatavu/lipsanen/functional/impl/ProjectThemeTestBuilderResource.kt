package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.ProjectThemesApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.ProjectTheme
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test builder resource for project theme API
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class ProjectThemeTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<ProjectTheme, ProjectThemesApi>(testBuilder, apiClient) {

    private val themeToProjectId = mutableMapOf<UUID, UUID>()

    override fun clean(p0: ProjectTheme?) {
        p0?.let { api.deleteProjectTheme(themeToProjectId[it.id]!!, it.id!!) }
    }

    override fun getApi(): ProjectThemesApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return ProjectThemesApi(ApiTestSettings.apiBasePath)
    }

    fun create(projectId: UUID, projectTheme: ProjectTheme): ProjectTheme? {
        val created = addClosable(api.createProjectTheme(projectId, projectTheme))
        themeToProjectId[created.id!!] = projectId
        return created
    }

    fun create(projectId: UUID): ProjectTheme? {
        val created = addClosable(
            api.createProjectTheme(
                projectId,
                ProjectTheme(
                    logoUrl = "https://example.com/logo.png",
                    themeColor = "#000000",
                )
            )
        )
        themeToProjectId[created.id!!] = projectId
        return created
    }

    fun findProjectTheme(projectId: UUID, projectThemeId: UUID): ProjectTheme? {
        return api.findProjectTheme(projectId, projectThemeId)
    }

    fun assertFindFail(
        expectedStatus: Int,
        projectId: UUID,
        projectThemeId: UUID
    ) {
        try {
            api.findProjectTheme(projectId, projectThemeId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun listProjectThemes(projectId: UUID): Array<ProjectTheme> {
        return api.listProjectThemes(projectId)
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID
    ) {
        try {
            api.listProjectThemes(projectId)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun updateProjectTheme(projectId: UUID, projectThemeId: UUID, projectTheme: ProjectTheme): ProjectTheme {
        return api.updateProjectTheme(projectId, projectThemeId, projectTheme)
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        projectThemeId: UUID,
        projectTheme: ProjectTheme
    ) {
        try {
            api.updateProjectTheme(projectId, projectThemeId, projectTheme)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        projectId: UUID,
        projectThemeId: UUID
    ) {
        try {
            api.deleteProjectTheme(projectId, projectThemeId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun deleteTheme(projectThemeId: UUID) {
        api.deleteProjectTheme(themeToProjectId[projectThemeId]!!, projectThemeId)
        removeCloseable { closable: Any ->
            if (closable !is ProjectTheme) {
                return@removeCloseable false
            }

            closable.id == projectThemeId
        }
    }

    fun assertCreateFail(i: Int, id: UUID, themeData: ProjectTheme) {
        try {
            api.createProjectTheme(id, themeData)
            fail(String.format("Expected create to fail with status %d", i))
        } catch (e: ClientException) {
            Assertions.assertEquals(i.toLong(), e.statusCode.toLong())
        }
    }
}