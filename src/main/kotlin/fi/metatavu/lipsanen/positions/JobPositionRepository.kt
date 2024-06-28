package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.persistence.AbstractRepository
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for job positions
 */
@ApplicationScoped
class JobPositionRepository: AbstractRepository<JobPositionEntity, UUID>() {

    /**
     * Creates a new job position
     *
     * @param name name
     * @param color color
     * @param iconName icon name
     * @return created job position
     */
    suspend fun create(name: String, color: String?, iconName: String?, creatorId: UUID): JobPositionEntity {
        val jobPositionEntity = JobPositionEntity()
        jobPositionEntity.id = UUID.randomUUID()
        jobPositionEntity.name = name
        jobPositionEntity.color = color
        jobPositionEntity.iconName = iconName
        jobPositionEntity.creatorId = creatorId
        jobPositionEntity.lastModifierId = creatorId
        return persistSuspending(jobPositionEntity)
    }

    /**
     * Lists job positions
     *
     * @param first first result
     * @param max max results
     * @return list of job positions
     */
    suspend fun update(jobPosition: JobPositionEntity, name: String, color: String?, iconName: String?, modifierId: UUID): JobPositionEntity {
        jobPosition.name = name
        jobPosition.color = color
        jobPosition.iconName = iconName
        jobPosition.lastModifierId = modifierId
        return persistSuspending(jobPosition)
    }

    /**
     * Finds a job position by id
     *
     * @param id id
     * @return found job position or null if not found
     */
    suspend fun find(id: UUID): JobPositionEntity? {
        return findByIdSuspending(id)
    }
}