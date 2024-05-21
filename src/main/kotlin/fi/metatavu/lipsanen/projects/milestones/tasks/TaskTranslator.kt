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

    override suspend fun translate(entity: TaskEntity): Task {
        return Task(
            id = entity.id,
            name = entity.name,
            startDate = entity.startDate,
            endDate = entity.endDate,
            milestoneId = entity.milestone.id,
            status = entity.status,
            assigneeIds = entity.assignees.map { it.assigneeId},
            userRole = entity.userRole,
            estimatedDuration = entity.estimatedDuration,
            estimatedReadiness = entity.estimatedReadiness,
            attachmentUrls = entity.attachments.map { it.attachmentUrl },
            metadata = metadataTranslator.translate(entity)
        )
    }
}