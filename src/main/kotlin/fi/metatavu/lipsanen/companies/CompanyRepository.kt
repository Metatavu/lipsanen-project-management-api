package fi.metatavu.lipsanen.companies

import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for companies
 */
@ApplicationScoped
class CompanyRepository : AbstractRepository<CompanyEntity, UUID>() {

    /**
     * Creates a new company
     *
     * @param id company id
     * @param name company name
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created company
     */
    suspend fun create(
        id: UUID,
        name: String,
        creatorId: UUID,
        lastModifierId: UUID
    ): CompanyEntity {
        val companyEntity = CompanyEntity()
        companyEntity.id = id
        companyEntity.name = name
        companyEntity.creatorId = creatorId
        companyEntity.lastModifierId = lastModifierId
        return persistSuspending(companyEntity)
    }

    /**
     * Lists companies
     *
     * @param name company name
     * @param first first result
     * @param max max results
     * @return list of companies
     */
    suspend fun list(name: String?, first: Int?, max: Int?): Pair<List<CompanyEntity>, Long> {
        val sb = StringBuilder()
        val params = Parameters()

        if (name != null) {
            sb.append("name = :name")
            params.and("name", name)
        }

        return applyFirstMaxToQuery(
            query = find(sb.toString(), Sort.descending("modifiedAt"), params),
            firstIndex = first,
            maxResults = max
        )
    }

}