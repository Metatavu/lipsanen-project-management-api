package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.api.model.JobPosition
import fi.metatavu.lipsanen.api.spec.JobPositionsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

/**
 * Job positions API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class JobPositionApiImpl: JobPositionsApi, AbstractApi() {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var jobPositionController: JobPositionController

    @Inject
    lateinit var jobPositionTranslator: JobPositionTranslator

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listJobPositions(first: Int?, max: Int?): Uni<Response> = withCoroutineScope {
        val (jobPositions, count) = jobPositionController.list(first = first, max = max)
        createOk(jobPositions.map { jobPositionTranslator.translate(it) }, count)
    }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createJobPosition(jobPosition: JobPosition): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        if (jobPositionController.findByName(jobPosition.name) != null) {
            return@withCoroutineScope createBadRequest("Job position with name ${jobPosition.name} already exists")
        }
        val created = jobPositionController.createJobPosition(jobPosition, userId)
        createOk(jobPositionTranslator.translate(created))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findJobPosition(positionId: UUID): Uni<Response> = withCoroutineScope {
        val found = jobPositionController.findJobPosition(positionId) ?: return@withCoroutineScope createNotFound("Job position not found")
        createOk(jobPositionTranslator.translate(found))
    }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun updateJobPosition(positionId: UUID, jobPosition: JobPosition): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val found = jobPositionController.findJobPosition(positionId) ?: return@withCoroutineScope createNotFound("Job position not found")

        val sameName = jobPositionController.findByName(jobPosition.name)
        if (sameName != null && sameName.id != positionId) {
            return@withCoroutineScope createBadRequest("Job position with name ${jobPosition.name} already exists")
        }
        val updated = jobPositionController.updateJobPosition(found, jobPosition, userId)
        createOk(jobPositionTranslator.translate(updated))
    }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteJobPosition(positionId: UUID): Uni<Response> = withCoroutineScope {
        val found = jobPositionController.findJobPosition(positionId) ?: return@withCoroutineScope createNotFound("Job position not found")
        jobPositionController.deleteJobPosition(found)
        createNoContent()
    }
}