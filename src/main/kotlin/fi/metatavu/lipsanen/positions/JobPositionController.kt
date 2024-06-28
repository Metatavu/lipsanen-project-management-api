package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.api.model.JobPosition
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for job positions
 */
@ApplicationScoped
class JobPositionController {

    @Inject
    lateinit var jobPositionRepository: JobPositionRepository

    @Inject
    lateinit var userController: UserController

    /**
     * Lists job positions
     *
     * @param first first result
     * @param max max results
     * @return list of job positions
     */
    suspend fun list(first: Int?, max: Int?): Pair<List<JobPositionEntity>, Long> {
        return jobPositionRepository.applyFirstMaxToQuery(
            jobPositionRepository.findAll(Sort.descending("name")),
            first,
            max
        )
    }

    /**
     * Creates job position
     *
     * @param name job position name
     * @param userId user id
     * @return created job position
     */
    suspend fun createJobPosition(name: JobPosition, userId: UUID): JobPositionEntity {
        return jobPositionRepository.create(name.name, name.color, name.iconName, userId)
    }

    /**
     * Finds job position
     *
     * @param positionId job position id
     * @return job position
     */
    suspend fun findJobPosition(positionId: UUID): JobPositionEntity? {
        return jobPositionRepository.find(positionId)
    }

    /**
     * Finds job position by name
     *
     * @param name job position name
     * @return job position
     */
    suspend fun findByName(name: String): JobPositionEntity? {
        return jobPositionRepository.find("name", name).firstResult<JobPositionEntity>().awaitSuspending()
    }

    /**
     * Updates job position
     *
     * @param jobPosition job position
     * @param restObject rest object
     * @param userId user id
     * @return updated job position
     */
    suspend fun updateJobPosition(jobPosition: JobPositionEntity, restObject: JobPosition, userId: UUID): JobPositionEntity {
        return jobPositionRepository.update(jobPosition, restObject.name, restObject.color, restObject.iconName, userId)
    }

    /**
     * Deletes job position and unassigns users which belong to it
     *
     * @param jobPositionEntity job position entity
     */
    suspend fun deleteJobPosition(jobPositionEntity: JobPositionEntity) {
        userController.listUserEntities(jobPosition = jobPositionEntity).first.forEach {
            userController.persistUserEntity(it.apply { jobPosition = null })
        }
        jobPositionRepository.deleteSuspending(jobPositionEntity)
    }
}