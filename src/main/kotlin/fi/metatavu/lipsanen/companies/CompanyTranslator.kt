package fi.metatavu.lipsanen.companies

import fi.metatavu.lipsanen.api.model.Company
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for company
 */
@ApplicationScoped
class CompanyTranslator : AbstractTranslator<CompanyEntity, Company>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: CompanyEntity): Company {
        return Company(
            id = entity.id,
            name = entity.name,
            metadata = metadataTranslator.translate(entity)
        )
    }
}