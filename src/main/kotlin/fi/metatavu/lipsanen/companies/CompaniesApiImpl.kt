package fi.metatavu.lipsanen.companies

import fi.metatavu.lipsanen.api.model.Company
import fi.metatavu.lipsanen.api.spec.CompaniesApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*

/**
 * Companies API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class CompaniesApiImpl : CompaniesApi, AbstractApi() {

    @Inject
    lateinit var companyController: CompanyController

    @Inject
    lateinit var companyTranslator: CompanyTranslator

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun listCompanies(first: Int?, max: Int?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (companies, count) = companyController.list(first = first, max = max)
        createOk(companyTranslator.translate(companies), count)
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createCompany(company: Company): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        val nameDuplicates = companyController.list(name = company.name)
        if (nameDuplicates.second > 0) {
            return@async createConflict("Company with given name ${company.name} already exists!")
        }

        val createdCompany = companyController.create(
            company = company,
            userId = userId
        )

        createOk(companyTranslator.translate(createdCompany))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun findCompany(companyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundCompany = companyController.find(companyId) ?: return@async createNotFound(createNotFoundMessage(COMPANY, companyId))
        createOk(companyTranslator.translate(foundCompany))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteCompany(companyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundCompany = companyController.find(companyId) ?: return@async createNotFound(createNotFoundMessage(COMPANY, companyId))

        if (userController.listUsers(foundCompany, null, null, null).second > 0) {
            return@async createConflict("Company has users")
        }

        companyController.delete(foundCompany)
        createNoContent()
    }.asUni()
}