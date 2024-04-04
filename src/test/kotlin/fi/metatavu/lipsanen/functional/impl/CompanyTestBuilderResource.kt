package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.CompaniesApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.Company
import junit.framework.TestCase.fail
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test builder resource for companies
 *
 * @param testBuilder test builder
 * @param accessTokenProvider access token provider
 * @param apiClient api client
 */
class CompanyTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Company, CompaniesApi>(testBuilder, apiClient) {

    override fun clean(p0: Company?) {
        p0?.id?.let { api.deleteCompany(it) }
    }

    override fun getApi(): CompaniesApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return CompaniesApi(ApiTestSettings.apiBasePath)
    }

    fun create(): Company {
        return create(Company("Test company"))
    }

    fun create(companyData: Company): Company {
        return addClosable(api.createCompany(companyData))
    }

    fun assertCreateFail(
        expectedStatus: Int,
        companyData: Company
    ) {
        try {
            api.createCompany(companyData)
            fail(String.format("Expected create to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun findCompany(id: UUID): Company {
        return api.findCompany(id)
    }

    fun assertFindFail(
        expectedStatus: Int,
        id: UUID
    ) {
        try {
            api.findCompany(id)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun listCompanies(
        first: Int? = null,
        max: Int? = null
    ): Array<Company> {
        return api.listCompanies(first = first, max = max)
    }

    fun assertListFail(
        expectedStatus: Int,
        first: Int? = null,
        max: Int? = null
    ) {
        try {
            api.listCompanies(first = first, max = max)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }


    fun deleteCompany(id: UUID) {
        api.deleteCompany(id)
        removeCloseable { closable: Any ->
            if (closable !is Company) {
                return@removeCloseable false
            }
            closable.id == id
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        id: UUID
    ) {
        try {
            api.deleteCompany(id)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }
}