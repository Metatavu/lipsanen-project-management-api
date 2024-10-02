package fi.metatavu.lipsanen.tocoman

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.users.UserController
import fi.metatavu.lipsanen.users.userstoprojects.UserToProjectRepository
import io.quarkus.hibernate.reactive.panache.Panache
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.*

/**
 * Controller for dealing with Tocoman-related data
 */
@ApplicationScoped
class TocomanController {

    val xmlMapper: XmlMapper = XmlMapper()

    @Inject
    lateinit var projectController: ProjectController

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var userToProjectRepository: UserToProjectRepository

    @Inject
    lateinit var logger: org.jboss.logging.Logger

    @PostConstruct
    fun init() {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    /**
     * Imports projects from a Tocoman XML file
     *
     * @param file the file to import
     * @param userId the user ID
     * @return the imported project
     */
    suspend fun importProjects(file: File, userId: UUID): ProjectEntity? {
        return try {
            val user = userController.findUser(userId) ?: throw Exception("User not found")
            val projects = xmlMapper.readValue(
                file.readBytes(),
                object: com.fasterxml.jackson.core.type.TypeReference<Projects>() {}
            )
            projects.project?.let {
                if (it.projName == null) {
                    logger.error("Missing project name")
                    return null
                }
                val existingProject = projectController.findProjectByTocomanId(it.id)
                val newProject = if (existingProject == null) {
                    projectController.createProject(
                        name = it.projName!!,
                        tocomanId = it.id,
                        creatorId = userId
                    )
                } else {
                    projectController.updateProject(
                        existingProject = existingProject,
                        name = it.projName!!,
                        userId = userId
                    )
                }
                userToProjectRepository.create(
                    id = UUID.randomUUID(),
                    user = user,
                    project = newProject
                )
                newProject
            }
        } catch (e: Exception) {
            Panache.currentTransaction().awaitSuspending().markForRollback()
            logger.error("Error creating project: ${e.message}")
            null
        }
    }

    /**
     * Exports a project (incomplete)
     *
     * @param existingProject existing project
     * @return exported project
     */
    fun exportProject(existingProject: ProjectEntity): OutputStream? {
        val mainProject = Projects()
        val project = Project()
        project.id = 0                      //todo should be filled as the max in the current database?
        project.projName = existingProject.name
        mainProject.project = project
        val byteArrayOutputStream = ByteArrayOutputStream()
        xmlMapper.writeValue(byteArrayOutputStream, mainProject)
        return byteArrayOutputStream
    }
}