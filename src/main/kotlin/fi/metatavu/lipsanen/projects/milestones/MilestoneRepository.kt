package fi.metatavu.lipsanen.projects.milestones

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.*

/**
 * Repository for milestones
 */
@ApplicationScoped
class MilestoneRepository : AbstractRepository<MilestoneEntity, UUID>() {

    /**
     * Creates a new milestone
     *
     * @param id id
     * @param project project
     * @param name name
     * @param startDate start date
     * @param endDate end date
     * @param originalStartDate original start date
     * @param originalEndDate original end date
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created milestone
     */
    suspend fun create(
        id: UUID,
        project: ProjectEntity,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        originalStartDate: LocalDate?,
        originalEndDate: LocalDate?,
        creatorId: UUID,
        lastModifierId: UUID
    ): MilestoneEntity {
        val milestone = MilestoneEntity()
        milestone.id = id
        milestone.project = project
        milestone.name = name
        milestone.startDate = startDate
        milestone.endDate = endDate
        // If originalStartDate is null, use startDate
        milestone.originalStartDate = originalStartDate ?: startDate
        // If originalEndDate is null, use endDate
        milestone.originalEndDate = originalEndDate ?: endDate
        milestone.creatorId = creatorId
        milestone.lastModifierId = lastModifierId
        return persistSuspending(milestone)
    }
}