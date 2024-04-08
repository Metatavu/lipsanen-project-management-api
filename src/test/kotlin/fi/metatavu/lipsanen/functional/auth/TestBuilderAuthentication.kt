package fi.metatavu.lipsanen.functional.auth

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenTestBuilderAuthentication
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.impl.CompanyTestBuilderResource
import fi.metatavu.lipsanen.functional.impl.ProjectTestBuilderResource
import fi.metatavu.lipsanen.functional.impl.ProjectThemeTestBuilderResource
import fi.metatavu.lipsanen.functional.impl.UserTestBuilderResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient

/**
 * Test builder authentication
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 *
 * @param testBuilder test builder instance
 * @param accessTokenProvider access token provider
 */
class TestBuilderAuthentication(
    private val testBuilder: TestBuilder,
    val accessTokenProvider: AccessTokenProvider
) : AccessTokenTestBuilderAuthentication<ApiClient>(testBuilder, accessTokenProvider) {

    val project = ProjectTestBuilderResource(testBuilder, accessTokenProvider, createClient(accessTokenProvider))
    val user = UserTestBuilderResource(testBuilder, accessTokenProvider, createClient(accessTokenProvider))
    val company = CompanyTestBuilderResource(testBuilder, accessTokenProvider, createClient(accessTokenProvider))
    val projectTheme = ProjectThemeTestBuilderResource(testBuilder, accessTokenProvider, createClient(accessTokenProvider))

    override fun createClient(authProvider: AccessTokenProvider): ApiClient {
        val result = ApiClient(ApiTestSettings.apiBasePath)
        ApiClient.accessToken = authProvider.accessToken
        return result
    }

}