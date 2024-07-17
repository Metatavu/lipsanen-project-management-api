package fi.metatavu.lipsanen.projects.themes

import fi.metatavu.lipsanen.api.model.ProjectTheme
import fi.metatavu.lipsanen.api.spec.ProjectThemesApi
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
 * Project themes API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class ProjectThemesApiImpl : ProjectThemesApi, AbstractApi() {

    @Inject
    lateinit var projectThemeController: ProjectThemeController

    @Inject
    lateinit var projectThemeTranslator: ProjectThemeTranslator

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listProjectThemes(projectId: UUID): Uni<Response> = withCoroutineScope {
        val project = projectController.findProject(projectId) ?: return@withCoroutineScope createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )
        val (themes, count) = projectThemeController.list(project)
        createOk(projectThemeTranslator.translate(themes), count)
    }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createProjectTheme(projectId: UUID, projectTheme: ProjectTheme): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val project = projectController.findProject(projectId) ?: return@withCoroutineScope createNotFound(
                createNotFoundMessage(
                    PROJECT,
                    projectId
                )
            )

            val created = projectThemeController.create(project, projectTheme, userId)
            createOk(projectThemeTranslator.translate(created))
        }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.USER.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findProjectTheme(projectId: UUID, themeId: UUID): Uni<Response> =
        withCoroutineScope {
            val (existingTheme, errorResponse) = findTheme(projectId, themeId)
            if (errorResponse != null) {
                return@withCoroutineScope errorResponse
            }

            createOk(projectThemeTranslator.translate(existingTheme!!))
        }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun updateProjectTheme(projectId: UUID, themeId: UUID, projectTheme: ProjectTheme): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val (existingTheme, errorResponse) = findTheme(projectId, themeId)
            if (errorResponse != null) {
                return@withCoroutineScope errorResponse
            }

            val updatedTheme = projectThemeController.update(existingTheme!!, projectTheme, userId)
            createOk(projectThemeTranslator.translate(updatedTheme))
        }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteProjectTheme(projectId: UUID, themeId: UUID): Uni<Response> =
        withCoroutineScope {
            val (existingTheme, errorResponse) = findTheme(projectId, themeId)
            if (errorResponse != null) {
                return@withCoroutineScope errorResponse
            }

            projectThemeController.delete(existingTheme!!)
            createNoContent()
        }

    /**
     * Finds a theme by project and theme id and returns error response if any
     *
     * @param projectId project id
     * @param themeId theme id
     * @return pair of theme and error response, one of them must be not null
     */
    private suspend fun findTheme(projectId: UUID, themeId: UUID): Pair<ProjectThemeEntity?, Response?> {
        val project = projectController.findProject(projectId) ?: return null to createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )

        val existingTheme = projectThemeController.find(project, themeId) ?: return null to createNotFound(
            createNotFoundMessage(
                PROJECT_THEME,
                themeId
            )
        )

        return existingTheme to null
    }
}