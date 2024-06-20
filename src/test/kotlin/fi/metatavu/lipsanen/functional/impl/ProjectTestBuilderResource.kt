package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.ProjectsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.Project
import fi.metatavu.lipsanen.test.client.models.ProjectStatus
import io.restassured.common.mapper.TypeRef
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test builder resource for project API
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class ProjectTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Project, ProjectsApi>(testBuilder, apiClient) {

    override fun clean(t: Project?) {
        t?.id?.let {
            try {
                api.deleteProject(it)
            } catch (_: Exception) {}
        }
    }

    override fun getApi(): ProjectsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return ProjectsApi(ApiTestSettings.apiBasePath)
    }

    fun create(projectName: String): Project {
        return create(Project(projectName, status = ProjectStatus.PLANNING))
    }

    fun create(project: Project): Project {
        return addClosable(api.createProject(project))
    }

    fun create(): Project {
        return create(Project("Test project", status = ProjectStatus.PLANNING))
    }

    fun findProject(projectId: UUID): Project {
        return api.findProject(projectId)
    }

    fun assertFindFail(
        expectedStatus: Int,
        projectId: UUID
    ) {
        try {
            api.findProject(projectId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun listProjects(): Array<Project> {
        return api.listProjects()
    }

    fun updateProject(projectId: UUID, project: Project): Project {
        return api.updateProject(projectId, project)
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        project: Project
    ) {
        try {
            api.updateProject(projectId, project)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        projectId: UUID
    ) {
        try {
            api.deleteProject(projectId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun deleteProject(projectId: UUID) {
        api.deleteProject(projectId)
        removeCloseable { closable: Any ->
            if (closable !is Project) {
                return@removeCloseable false
            }

            closable.id == projectId
        }
    }

    fun exportProject(projectId: UUID): ByteArray? {
        return Given {
            header("Authorization", "Bearer ${accessTokenProvider?.accessToken}")
        }.When {
            get("v1/projects/$projectId/export")
        }.then().extract().body().asByteArray()
    }

    // List for storing imported projects in order to avoid adding multiple closeables for the same imported project
    private val importedProjects = mutableListOf<UUID>()

    fun importProject(fileName: String): Project {
        this.javaClass.classLoader.getResourceAsStream(fileName).use {
            val created = Given {
                header("Authorization", "Bearer ${accessTokenProvider!!.accessToken}")
                contentType("application/xml")
                body(it!!.readAllBytes())
            }.When { post("/v1/projects/import") }
                .then()
                .extract()
                .body().`as`(object : TypeRef<Project?>() {})
            if (!importedProjects.contains(created!!.id)){
                addClosable(created)
                importedProjects.add(created.id!!)
            }
            return created
        }
    }
}