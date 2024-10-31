package fi.metatavu.lipsanen.exceptions

import java.time.LocalDate
import java.util.*

/**
 * Exception for task outside milestone boundaries
 *
 * @param taskId task id
 * @param startDate new start date
 * @param endDate new end date
 */
class TaskOutsideMilestoneException(taskId: UUID, startDate: LocalDate, endDate: LocalDate) :
    Exception("Task $taskId with new dates $startDate - $endDate goes out of the milestone boundaries")