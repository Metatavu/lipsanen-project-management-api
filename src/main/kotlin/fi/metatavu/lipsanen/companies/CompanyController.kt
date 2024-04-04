package fi.metatavu.lipsanen.companies

import fi.metatavu.lipsanen.api.model.Company
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for managing companies
 */
@ApplicationScoped
class CompanyController {

    @Inject
    lateinit var companyRepository: CompanyRepository

    /**
     * Lists companies
     *
     * @param name company name
     * @param first first result
     * @param max max results
     * @return list of companies
     */
    suspend fun list(name: String? = null, first: Int? = null, max: Int? = null): Pair<List<CompanyEntity>, Long> {
        return companyRepository.list(name = name, first = first, max = max)
    }

    /**
     * Creates a new company
     *
     * @param company company
     * @param userId user id
     * @return created company
     */
    suspend fun create(company: Company, userId: UUID): CompanyEntity {
        return companyRepository.create(
            id = UUID.randomUUID(),
            name = company.name,
            creatorId = userId,
            lastModifierId = userId
        )
    }

    /**
     * Finds company by id
     *
     * @param companyId company id
     * @return found company or null if not found
     */
    suspend fun find(companyId: UUID): CompanyEntity? {
        return companyRepository.findByIdSuspending(companyId)
    }

    /**
     * Updates company
     *
     * @param company company
     * @param foundCompany found company
     * @param userId user id
     * @return updated company
     */
    suspend fun delete(foundCompany: CompanyEntity) {
        return companyRepository.deleteSuspending(foundCompany)
    }
}
