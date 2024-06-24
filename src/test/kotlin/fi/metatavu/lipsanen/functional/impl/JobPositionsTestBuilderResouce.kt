package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.JobPositionsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.JobPosition
import junit.framework.TestCase.fail
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test builder resource for job positions
 */
class JobPositionsTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<JobPosition, JobPositionsApi>(testBuilder, apiClient) {
    override fun clean(p0: JobPosition?) {
        p0?.let { api.deleteJobPosition(it.id!!) }
    }

    override fun getApi(): JobPositionsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return JobPositionsApi(ApiTestSettings.apiBasePath)
    }

    fun create(jobPosition: JobPosition): JobPosition {
        return addClosable(api.createJobPosition(jobPosition))
    }

    fun create(
        name: String,
        color: String? = null,
        iconName: String? = null
    ): JobPosition {
        return create(JobPosition(name = name, color = color, iconName = iconName))
    }

    fun assertCreateFails(expectedStatus: Int, jobPosition: JobPosition) = try {
        api.createJobPosition(jobPosition)
        fail("Expected create to fail")
    } catch (e: ClientException) {
        Assertions.assertEquals(expectedStatus, e.statusCode)
    }

    fun findJobPosition(jobPositionId: UUID): JobPosition {
        return api.findJobPosition(jobPositionId)
    }

    fun assertFindFails(expectedStatus: Int, jobPositionId: UUID) = try {
        api.findJobPosition(jobPositionId)
        fail("Expected find to fail")
    } catch (e: ClientException) {
        Assertions.assertEquals(expectedStatus, e.statusCode)
    }

    fun listJobPositions(): Array<JobPosition> {
        return api.listJobPositions()
    }

    fun assertListFails(expectedStatus: Int) = try {
        api.listJobPositions()
        fail("Expected list to fail")
    } catch (e: ClientException) {
        Assertions.assertEquals(expectedStatus, e.statusCode)
    }

    fun updateJobPosition(jobPositionId: UUID, jobPosition: JobPosition): JobPosition {
        return api.updateJobPosition(jobPosition.id!!, jobPosition)
    }

    fun assertUpdateFails(expectedStatus: Int, jobPositionId: UUID, jobPosition: JobPosition) = try {
        api.updateJobPosition(jobPositionId, jobPosition)
        fail("Expected update to fail")
    } catch (e: ClientException) {
        Assertions.assertEquals(expectedStatus, e.statusCode)
    }

    fun deleteJobPosition(jobPositionId: UUID) {
        api.deleteJobPosition(jobPositionId)
        removeCloseable { closable: Any ->
            if (closable !is JobPosition) {
                return@removeCloseable false
            }

            closable.id == jobPositionId
        }
    }

    fun assertDeleteFails(expectedStatus: Int, jobPositionId: UUID) = try {
        api.deleteJobPosition(jobPositionId)
        fail("Expected delete to fail")
    } catch (e: ClientException) {
        Assertions.assertEquals(expectedStatus, e.statusCode)
    }

}