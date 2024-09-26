package fi.metatavu.lipsanen.milestones

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
     * @param estimatedReadiness estimated readiness
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
        originalStartDate: LocalDate,
        originalEndDate: LocalDate,
        estimatedReadiness: Int?,
        creatorId: UUID,
        lastModifierId: UUID
    ): MilestoneEntity {
        val milestone = MilestoneEntity()
        milestone.id = id
        milestone.project = project
        milestone.name = name
        milestone.startDate = startDate
        milestone.endDate = endDate
        milestone.originalStartDate = originalStartDate
        milestone.originalEndDate = originalEndDate
        milestone.estimatedReadiness = estimatedReadiness
        milestone.creatorId = creatorId
        milestone.lastModifierId = lastModifierId
        return persistSuspending(milestone)
    }
}