package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.api.model.JobPosition
import fi.metatavu.lipsanen.api.spec.JobPositionsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun listJobPositions(first: Int?, max: Int?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (jobPositions, count) = jobPositionController.list(first = first, max = max)
        createOk(jobPositions.map { jobPositionTranslator.translate(it) }, count)
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createJobPosition(jobPosition: JobPosition): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (jobPositionController.findByName(jobPosition.name) != null) {
            return@async createBadRequest("Job position with name ${jobPosition.name} already exists")
        }
        val created = jobPositionController.createJobPosition(jobPosition, userId)
        createOk(jobPositionTranslator.translate(created))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun findJobPosition(positionId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val found = jobPositionController.findJobPosition(positionId) ?: return@async createNotFound("Job position not found")
        createOk(jobPositionTranslator.translate(found))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun updateJobPosition(positionId: UUID, jobPosition: JobPosition): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val found = jobPositionController.findJobPosition(positionId) ?: return@async createNotFound("Job position not found")

        val sameName = jobPositionController.findByName(jobPosition.name)
        if (sameName != null && sameName.id != positionId) {
            return@async createBadRequest("Job position with name ${jobPosition.name} already exists")
        }
        val updated = jobPositionController.updateJobPosition(found, jobPosition, userId)
        createOk(jobPositionTranslator.translate(updated))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteJobPosition(positionId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val found = jobPositionController.findJobPosition(positionId) ?: return@async createNotFound("Job position not found")
        jobPositionController.deleteJobPosition(found)
        createNoContent()
    }.asUni()
}