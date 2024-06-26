package fi.metatavu.lipsanen.functional

import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.Company
import fi.metatavu.lipsanen.test.client.models.UserRole
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for Companies API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class CompanyTestIT : AbstractFunctionalTest() {

    @Test
    fun testCreateCompany() = createTestBuilder().use {
        val companyData = Company(
            name = "Test company"
        )
        val createdCompany = it.admin.company.create(companyData)
        assertEquals(companyData.name, createdCompany.name)
        assertNotNull(createdCompany.id)
        assertNotNull(createdCompany.metadata)
        assertEquals(companyData.name, createdCompany.name)
    }

    @Test
    fun testCreateCompanyFail() = createTestBuilder().use {
        val companyData = Company(
            name = "Test company"
        )
        it.admin.company.create(companyData)
        it.admin.company.assertCreateFail(409, companyData)

        // access rights
        it.user.company.assertCreateFail(403, companyData)
    }

    @Test
    fun testFindCompany() = createTestBuilder().use {
        val companyData = Company(
            name = "Test company"
        )
        val createdCompany = it.admin.company.create(companyData)
        val foundCompany = it.admin.company.findCompany(createdCompany.id!!)
        assertNotNull(foundCompany)
        assertEquals(createdCompany.id, foundCompany.id)
        assertEquals(createdCompany.name, foundCompany.name)
    }

    @Test
    fun testFindCompanyFail() = createTestBuilder().use { tb ->
        InvalidValueTestScenarioBuilder(
            path = "v1/companies/{companyId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "companyId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }

    @Test
    fun testListCompanies() = createTestBuilder().use {
        it.admin.company.create(Company("Company 1"))
        it.admin.company.create(Company("Company 2"))
        val companies = it.admin.company.listCompanies()
        assertNotNull(companies)
        assertEquals(2, companies.size)
    }

    @Test
    fun testDeleteCompany() = createTestBuilder().use {
        val company = it.admin.company.create()
        it.admin.company.deleteCompany(company.id!!)
        it.admin.company.assertFindFail(404, company.id)
    }

    @Test
    fun testDeleteCompanyFail() = createTestBuilder().use {
        val company = it.admin.company.create(Company("Company 1"))

        //cannot delete company which has users
        val createdUser = it.admin.user.create("test", UserRole.USER)
        it.admin.user.updateUser(userId = createdUser.id!!, user = createdUser.copy(companyId = company.id))

        it.admin.company.assertDeleteFail(409, company.id!!)
        it.admin.user.updateUser(userId = createdUser.id, user = createdUser .copy(companyId = null))

        //access rights
        it.user.company.assertDeleteFail(403, company.id)

        InvalidValueTestScenarioBuilder(
            path = "v1/companies/{companyId}",
            method = Method.DELETE,
            token = it.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "companyId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404
                )
            )
            .build()
            .test()
    }
}