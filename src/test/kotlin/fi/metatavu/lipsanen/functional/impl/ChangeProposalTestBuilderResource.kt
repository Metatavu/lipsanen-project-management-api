package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.ChangeProposalsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.ChangeProposal
import fi.metatavu.lipsanen.test.client.models.ChangeProposalStatus
import fi.metatavu.lipsanen.test.client.models.Task
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test builder resource for change proposals
 */
class ChangeProposalTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<ChangeProposal, ChangeProposalsApi>(testBuilder, apiClient) {

    private val proposalToTaskId = mutableMapOf<UUID, UUID>()
    private val proposalToProjectId = mutableMapOf<UUID, UUID>()

    override fun clean(p0: ChangeProposal?) {
        p0?.let {
            api.deleteChangeProposal(
                projectId = proposalToProjectId[it.id!!]!!,
                changeProposalId = it.id
            )
        }
    }

    override fun getApi(): ChangeProposalsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return ChangeProposalsApi(ApiTestSettings.apiBasePath)
    }

    fun create(projectId: UUID, taskId: UUID): ChangeProposal? {
        val created = addClosable(
            api.createChangeProposal(
                projectId,
                ChangeProposal(
                    reason = "reason",
                    comment = "comment",
                    status = ChangeProposalStatus.PENDING,
                    taskId = taskId,
                    startDate = null,
                    endDate = null
                )
            )
        )
        proposalToTaskId[created.id!!] = taskId
        proposalToProjectId[created.id] = projectId
        return created
    }

    fun create(projectId: UUID, changeProposal: ChangeProposal): ChangeProposal? {
        val created = addClosable(api.createChangeProposal(projectId, changeProposal))
        proposalToTaskId[created.id!!] = changeProposal.taskId
        proposalToProjectId[created.id] = projectId
        return created
    }

    fun create(projectId: UUID, taskId: UUID, startDate: String, endDate: String): ChangeProposal {
        val created = addClosable(api.createChangeProposal(projectId, ChangeProposal(
            taskId = taskId,
            comment = "comment",
            status = ChangeProposalStatus.PENDING,
            startDate = startDate,
            endDate = endDate,
            reason = "because thats why"
        )))
        proposalToTaskId[created.id!!] = taskId
        proposalToProjectId[created.id] = projectId
        return created
    }

    fun findChangeProposal(
        projectId: UUID,
        changeProposalId: UUID
    ): ChangeProposal {
        return api.findChangeProposal(projectId, changeProposalId)
    }

    fun listChangeProposalTasksPreview(
        projectId: UUID,
        changeProposalId: UUID
    ): Array<Task> {
        return api.listChangeProposalTasksPreview(projectId, changeProposalId)
    }

    fun assertListChangeProposalTasksPreviewFail(
        expectedStatus: Int,
        projectId: UUID,
        changeProposalId: UUID
    ) {
        try {
            api.listChangeProposalTasksPreview(projectId, changeProposalId)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertFindFail(
        expectedStatus: Int,
        projectId: UUID,
        changeProposalId: UUID
    ) {
        try {
            api.findChangeProposal(projectId, changeProposalId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun listChangeProposals(
        projectId: UUID? = null,
        milestoneId: UUID? = null,
        taskId: UUID? = null,
        first: Int? = null,
        max: Int? = null
    ): Array<ChangeProposal> {
        return api.listChangeProposals(projectId, milestoneId, taskId, first, max)
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID,
        taskId: UUID
    ) {
        try {
            api.listChangeProposals(projectId, taskId)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun updateChangeProposal(
        projectId: UUID,
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ): ChangeProposal {
        return api.updateChangeProposal(projectId, changeProposalId, changeProposal)
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ) {
        try {
            api.updateChangeProposal(projectId, changeProposalId, changeProposal)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        projectId: UUID,
        changeProposalId: UUID
    ) {
        try {
            api.deleteChangeProposal(projectId, changeProposalId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun deleteProposal(projectId: UUID, changeProposalId: UUID) {
        api.deleteChangeProposal(
            projectId = projectId,
            changeProposalId = changeProposalId
        )
        removeCloseable { closable: Any ->
            if (closable !is ChangeProposal) {
                return@removeCloseable false
            }

            closable.id == changeProposalId
        }
    }

    fun assertCreateFail(i: Int, projectId: UUID, proposalData: ChangeProposal) {
        try {
            api.createChangeProposal(projectId, proposalData)
            fail(String.format("Expected create to fail with status %d", i))
        } catch (e: ClientException) {
            Assertions.assertEquals(i.toLong(), e.statusCode.toLong())
        }
    }
}