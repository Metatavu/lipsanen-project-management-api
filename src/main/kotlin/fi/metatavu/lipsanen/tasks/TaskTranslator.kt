package fi.metatavu.lipsanen.tasks

import fi.metatavu.lipsanen.api.model.Task
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for tasks
 */
@ApplicationScoped
class TaskTranslator : AbstractTranslator<TaskEntity, Task>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    @Inject
    lateinit var taskAssigneeRepository: TaskAssigneeRepository

    override suspend fun translate(entity: TaskEntity): Task {
        return Task(
            id = entity.id,
            name = entity.name,
            startDate = entity.startDate,
            endDate = entity.endDate,
            milestoneId = entity.milestone.id,
            jobPositionId = entity.jobPosition?.id,
            status = entity.status,
            userRole = entity.userRole,
            estimatedReadiness = entity.estimatedReadiness,
            assigneeIds = taskAssigneeRepository.listByTask(entity).map { it.user.id },
            dependentUserId = entity.dependentUser?.id,
            metadata = metadataTranslator.translate(entity)
        )
    }
}