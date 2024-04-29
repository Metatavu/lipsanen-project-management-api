package fi.metatavu.lipsanen.projects.milestones.tasks.connections

import fi.metatavu.lipsanen.api.model.TaskConnectionType
import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for task connections
 */
@ApplicationScoped
class TaskConnectionRepository : AbstractRepository<TaskConnectionEntity, UUID>() {

    /**
     * Creates a new task connection
     *
     * @param id task connection id
     * @param source source task
     * @param target target task
     * @param type task connection type
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created task connection
     */
    suspend fun create(
        id: UUID,
        source: TaskEntity,
        target: TaskEntity,
        type: TaskConnectionType,
        creatorId: UUID,
        lastModifierId: UUID
    ): TaskConnectionEntity {
        val taskConnectionEntity = TaskConnectionEntity()
        taskConnectionEntity.id = id
        taskConnectionEntity.source = source
        taskConnectionEntity.target = target
        taskConnectionEntity.type = type
        return persistSuspending(taskConnectionEntity)
    }

    /**
     * Lists task connections by source task
     *
     * @param sourceTask source task
     * @return list of task connections
     */
    suspend fun listBySourceTask(sourceTask: TaskEntity): List<TaskConnectionEntity> {
        return list("source", sourceTask).awaitSuspending()
    }

    /**
     * Lists task connections by source tasks
     *
     * @param taskFilter task filter
     * @return list of task connections
     */
    suspend fun listBySourceTasks(taskFilter: List<TaskEntity>): List<TaskConnectionEntity> {
        return list("source in :tasks", Parameters.with("tasks", taskFilter)).awaitSuspending()
    }

    /**
     * Lists task connections by target task
     *
     * @param targetTask target task
     * @return list of task connections
     */
    suspend fun listByTargetTask(targetTask: TaskEntity): List<TaskConnectionEntity> {
        return list("target", targetTask).awaitSuspending()
    }

    /**
     * Lists task connections by target tasks
     *
     * @param taskFilter task filter
     * @return list of task connections
     */
    suspend fun listByTargetTasks(taskFilter: List<TaskEntity>): List<TaskConnectionEntity> {
        return list("target in :tasks", Parameters.with("tasks", taskFilter)).awaitSuspending()
    }

    /**
     * Counts task connections by task
     *
     * @param task task
     * @return count of task connections
     */
    suspend fun countByTask(task: TaskEntity): Long {
        return count("source = :task or target = :task", Parameters.with("task", task)).awaitSuspending()
    }

    /**
     * Lists task connections by tasks
     *
     * @param taskFilter task filter
     * @return list of task connections
     */
    suspend fun listByTasks(taskFilter: List<TaskEntity>): List<TaskConnectionEntity> {
        return list(
            "source in :tasks or target in :tasks",
            Parameters.with("tasks", taskFilter)
        ).awaitSuspending()
    }

}