package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.api.model.JobPosition
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

@ApplicationScoped
class JobPositionController {

    @Inject
    lateinit var jobPositionRepository: JobPositionRepository


    suspend fun updateJobPosition(jobPosition: JobPositionEntity, restObject: JobPosition, userId: UUID): JobPositionEntity {
        return jobPositionRepository.update(jobPosition, restObject.name, restObject.color, restObject.iconName, userId)
    }

    suspend fun list(first: Int?, max: Int?): Pair<List<JobPositionEntity>, Long> {
        return jobPositionRepository.applyFirstMaxToQuery(
            jobPositionRepository.findAll(),
            first,
            max
        )
    }

    suspend fun findJobPosition(positionId: UUID): JobPositionEntity? {
        return jobPositionRepository.find(positionId)
    }

    suspend fun createJobPosition(name: JobPosition, userId: UUID): JobPositionEntity {
        return jobPositionRepository.create(name.name, name.color, name.iconName, userId)
    }


}