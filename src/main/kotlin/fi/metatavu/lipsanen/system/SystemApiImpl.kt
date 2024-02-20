package fi.metatavu.lipsanen.system

import fi.metatavu.lipsanen.api.spec.SystemApi
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.RequestScoped
import jakarta.ws.rs.core.Response

/**
 * System API implementation
 */
@RequestScoped
class SystemApiImpl: SystemApi {
    override fun ping(): Uni<Response> = Uni.createFrom().item { Response.ok("pong").build() }
}