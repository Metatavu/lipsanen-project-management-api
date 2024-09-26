package fi.metatavu.lipsanen.tasks.connections

import fi.metatavu.lipsanen.api.model.TaskConnection
import fi.metatavu.lipsanen.rest.AbstractTranslator
import jakarta.enterprise.context.ApplicationScoped

/**
 * Translator for task connections
 */
@ApplicationScoped
class TaskConnectionTranslator : AbstractTranslator<TaskConnectionEntity, TaskConnection>() {

    override suspend fun translate(entity: TaskConnectionEntity): TaskConnection {
        return TaskConnection(
            id = entity.id,
            sourceTaskId = entity.source.id,
            targetTaskId = entity.target.id,
            type = entity.type
        )
    }
}