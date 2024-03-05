package fi.metatavu.lipsanen.tocoman

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.projects.ProjectEntity
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
            val projects = xmlMapper.readValue(
                file.readBytes(),
                object: com.fasterxml.jackson.core.type.TypeReference<Projects>() {}
            )
            projects.project!!.let {
                projectController.createProject(
                    name = it.projName ?: "project",
                    tocomanId = it.id ?: 0,
                    userId = userId
                )
            }
        } catch (e: Exception) {
            logger.error("Error parsing XML: ${e.message}")
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
        project.id = 0                      //todo should be filled as the mex in the current database?
        project.projName = existingProject.name
        mainProject.project = project
        val byteArrayOutputStream = ByteArrayOutputStream()
        xmlMapper.writeValue(byteArrayOutputStream, mainProject)
        return byteArrayOutputStream
    }
}