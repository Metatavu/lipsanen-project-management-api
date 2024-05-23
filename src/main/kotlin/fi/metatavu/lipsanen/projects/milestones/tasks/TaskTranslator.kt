package fi.metatavu.lipsanen.projects.milestones.tasks

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
    lateinit var taskAttachmentRepository: TaskAttachmentRepository

    @Inject
    lateinit var taskAssigneeRepository: TaskAssigneeRepository

    override suspend fun translate(entity: TaskEntity): Task {
        return Task(
            id = entity.id,
            name = entity.name,
            startDate = entity.startDate,
            endDate = entity.endDate,
            milestoneId = entity.milestone.id,
            status = entity.status,
            userRole = entity.userRole,
            estimatedDuration = entity.estimatedDuration,
            estimatedReadiness = entity.estimatedReadiness,
            attachmentUrls = taskAttachmentRepository.listByTask(entity).map { it.attachmentUrl },
            assigneeIds = taskAssigneeRepository.listByTask(entity).map { it.assigneeId },
            metadata = metadataTranslator.translate(entity)
        )
    }
}