package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Task assignee repository
 */
@ApplicationScoped
class TaskAssigneeRepository : AbstractRepository<TaskAssigneeEntity, UUID>() {

    /**
     * Creates a new task assignee entity
     *
     * @param id id
     * @param task task
     * @param assigneeId assignee id
     * @return created task assignee
     */
    suspend fun create(
        id: UUID,
        task: TaskEntity,
        assigneeId: UUID
    ): TaskAssigneeEntity {
        val taskAssigneeEntity = TaskAssigneeEntity()
        taskAssigneeEntity.id = id
        taskAssigneeEntity.task = task
        taskAssigneeEntity.assigneeId = assigneeId
        return persistSuspending(taskAssigneeEntity)
    }

    /**
     * Lists task assignees by task
     *
     * @param task task
     * @return list of task assignees
     */
    suspend fun listByTask(task: TaskEntity): List<TaskAssigneeEntity> {
        val queryBuilder = StringBuilder()
        val params = Parameters()

        queryBuilder.append("task = :task")
        params.and("task", task)

        return find(queryBuilder.toString(), Sort.ascending("assigneeId"), params).list<TaskAssigneeEntity>().awaitSuspending()
    }

    /**
     * Lists task assignees by assignee
     *
     * @param assigneeId assignee id
     * @return list of task assignees
     */
    suspend fun listByAssignee(assigneeId: UUID): List<TaskAssigneeEntity> {
        val queryBuilder = StringBuilder()
        val params = Parameters()

        queryBuilder.append("assigneeId = :assigneeId")
        params.and("assigneeId", assigneeId)

        return find(queryBuilder.toString(), params).list<TaskAssigneeEntity>().awaitSuspending()
    }
}