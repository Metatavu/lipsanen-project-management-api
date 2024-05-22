package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
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
     * Lists task assignees by task id
     *
     * @param taskId task id
     * @return list of task assignees
     */
    suspend fun listByTaskId(taskId: UUID): Pair<List<TaskAssigneeEntity>, Long> {
        val queryBuilder = StringBuilder()
        val params = Parameters()

        queryBuilder.append("task.id = :taskId")
        params.and("taskId", taskId)

        return applyFirstMaxToQuery(
            query = find(queryBuilder.toString(), Sort.descending("id"), params),
            firstIndex = null,
            maxResults = null
        )
    }
}