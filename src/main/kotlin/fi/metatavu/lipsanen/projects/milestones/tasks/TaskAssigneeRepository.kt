package fi.metatavu.lipsanen.projects.milestones.tasks

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.users.UserEntity
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
        user: UserEntity
    ): TaskAssigneeEntity {
        val taskAssigneeEntity = TaskAssigneeEntity()
        taskAssigneeEntity.id = id
        taskAssigneeEntity.task = task
        taskAssigneeEntity.user = user
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

        return find(queryBuilder.toString(), Sort.ascending("id"), params).list<TaskAssigneeEntity>().awaitSuspending()
    }

    /**
     * Lists task assignments by the user
     *
     * @param assigneeId assignee id
     * @return list of task assignees
     */
    suspend fun listByAssignee(user: UserEntity): List<TaskAssigneeEntity> {
        val queryBuilder = StringBuilder()
        val params = Parameters()

        queryBuilder.append("user = :user")
        params.and("user", user)

        return find(queryBuilder.toString(), params).list<TaskAssigneeEntity>().awaitSuspending()
    }
}