package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.api.model.TaskStatus
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.milestones.MilestoneEntity
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.*

/**
 * Repository for task entities
 */
@ApplicationScoped
class TaskEntityRepository : AbstractRepository<TaskEntity, UUID>() {

    /**
     * Creates a new task
     *
     * @param id task id
     * @param name task name
     * @param startDate task start date
     * @param endDate task end date
     * @param milestone milestone
     * @param status task status
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created task
     */
    suspend fun create(
        id: UUID,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        milestone: MilestoneEntity,
        status: TaskStatus,
        userRole: UserRole?,
        estimatedDuration: Float?,
        estimatedReadiness: Int?,
        creatorId: UUID,
        lastModifierId: UUID
    ): TaskEntity {
        val taskEntity = TaskEntity()
        taskEntity.id = id
        taskEntity.name = name
        taskEntity.startDate = startDate
        taskEntity.endDate = endDate
        taskEntity.milestone = milestone
        taskEntity.status = status
        taskEntity.userRole = userRole
        taskEntity.estimatedDuration = estimatedDuration
        taskEntity.estimatedReadiness = estimatedReadiness
        taskEntity.creatorId = creatorId
        taskEntity.lastModifierId = lastModifierId
        return persistSuspending(taskEntity)
    }

}