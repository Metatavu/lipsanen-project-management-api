package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.ChangeProposalsApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.ChangeProposal
import fi.metatavu.lipsanen.test.client.models.ChangeProposalStatus
import fi.metatavu.lipsanen.test.client.models.TaskProposal
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
    private val proposalToMilestoneId = mutableMapOf<UUID, UUID>()
    private val proposalToProjectId = mutableMapOf<UUID, UUID>()

    override fun clean(p0: ChangeProposal?) {
        p0?.let {
            api.deleteChangeProposal(
                projectId = proposalToProjectId[it.id!!]!!,
                milestoneId = proposalToMilestoneId[it.id]!!,
                taskId = proposalToTaskId[it.id]!!,
                changeProposalId = it.id
            )
        }
    }

    override fun getApi(): ChangeProposalsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return ChangeProposalsApi(ApiTestSettings.apiBasePath)
    }

    fun create(projectId: UUID, milestoneId: UUID, taskId: UUID): ChangeProposal? {
        val created = addClosable(
            api.createChangeProposal(
                projectId, milestoneId, taskId,
                ChangeProposal(
                    reason = "reason",
                    comment = "comment",
                    status = ChangeProposalStatus.PENDING,
                    taskProposal = TaskProposal(taskId = taskId, startDate = null, endDate = null)
                )
            )
        )
        proposalToTaskId[created.id!!] = taskId
        proposalToMilestoneId[created.id] = milestoneId
        proposalToProjectId[created.id] = projectId
        return created
    }

    fun create(projectId: UUID, milestoneId: UUID, taskId: UUID, changeProposal: ChangeProposal): ChangeProposal? {
        val created = addClosable(api.createChangeProposal(projectId, milestoneId, taskId, changeProposal))
        proposalToTaskId[created.id!!] = taskId
        proposalToMilestoneId[created.id!!] = milestoneId
        proposalToProjectId[created.id!!] = projectId
        return created
    }

    fun findChangeProposal(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID, changeProposalId: UUID
    ): ChangeProposal {
        return api.findChangeProposal(projectId, milestoneId, taskId, changeProposalId)
    }

    fun assertFindFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID
    ) {
        try {
            api.findChangeProposal(projectId, milestoneId, taskId, changeProposalId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun listChangeProposals(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        first: Int? = null,
        max: Int? = null
    ): Array<ChangeProposal> {
        return api.listChangeProposals(projectId, milestoneId, taskId, first, max)
    }

    fun assertListFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID
    ) {
        try {
            api.listChangeProposals(projectId, milestoneId, taskId)
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun updateChangeProposal(
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ): ChangeProposal {
        return api.updateChangeProposal(projectId, milestoneId, taskId, changeProposalId, changeProposal)
    }

    fun assertUpdateFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID,
        changeProposal: ChangeProposal
    ) {
        try {
            api.updateChangeProposal(projectId, milestoneId, taskId, changeProposalId, changeProposal)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun assertDeleteFail(
        expectedStatus: Int,
        projectId: UUID,
        milestoneId: UUID,
        taskId: UUID,
        changeProposalId: UUID
    ) {
        try {
            api.deleteChangeProposal(projectId, milestoneId, taskId, changeProposalId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    fun deleteProposal(projectId: UUID, milestoneId: UUID, taskId: UUID, changeProposalId: UUID) {
        api.deleteChangeProposal(
            projectId = projectId,
            milestoneId = milestoneId,
            taskId = taskId,
            changeProposalId = changeProposalId
        )
        removeCloseable { closable: Any ->
            if (closable !is ChangeProposal) {
                return@removeCloseable false
            }

            closable.id == changeProposalId
        }
    }

    fun assertCreateFail(i: Int, projectId: UUID, milestoneId: UUID, taskId: UUID, proposalData: ChangeProposal) {
        try {
            api.createChangeProposal(projectId, milestoneId, taskId, proposalData)
            fail(String.format("Expected create to fail with status %d", i))
        } catch (e: ClientException) {
            Assertions.assertEquals(i.toLong(), e.statusCode.toLong())
        }
    }
}