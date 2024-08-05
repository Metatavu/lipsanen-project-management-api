package fi.metatavu.lipsanen.functional

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.invalid.InvalidValueTestScenarioBody
import fi.metatavu.invalid.InvalidValueTestScenarioBuilder
import fi.metatavu.invalid.InvalidValueTestScenarioPath
import fi.metatavu.invalid.InvalidValues
import fi.metatavu.invalid.providers.SimpleInvalidValueProvider
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.*
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

/**
 * Tests for ChangeProposals API
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class ChangeProposalTestIT : AbstractFunctionalTest() {

    @Test
    fun testListChangeProposals() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone1 = tb.admin.milestone.create(projectId = project.id!!)
        val milestone2 = tb.admin.milestone.create(projectId = project.id)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone1.id!!)
        val task2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone2.id!!)
        val task3 = tb.admin.task.create(projectId = project.id, milestoneId = milestone2.id)

        val proposal1 = tb.admin.changeProposal.create(projectId = project.id, taskId = task1.id!!)
        val proposal2 = tb.admin.changeProposal.create(projectId = project.id, taskId = task2.id!!)
        val proposal3 = tb.admin.changeProposal.create(projectId = project.id, taskId = task3.id!!)

        val proposals = tb.admin.changeProposal.listChangeProposals(
            projectId = project.id,
            taskId = task1.id
        )
        assertEquals(1, proposals.size)

        val proposals2 = tb.admin.changeProposal.listChangeProposals(
            projectId = project.id,
            milestoneId = milestone2.id,
        )
        assertEquals(2, proposals2.size)
        assertEquals(proposal2!!.id, proposals2[0].id)
        assertEquals(proposal3!!.id, proposals2[1].id)

        val paging0 = tb.admin.changeProposal.listChangeProposals(
            projectId = project.id,
            milestoneId = milestone2.id,
            first = 0
        )
        assertEquals(2, paging0.size)
        val paging1 = tb.admin.changeProposal.listChangeProposals(
            projectId = project.id,
            milestoneId = milestone2.id,
            first = 1
        )
        assertEquals(1, paging1.size)

        val paging2 = tb.admin.changeProposal.listChangeProposals(
            projectId = project.id,
            milestoneId = milestone2.id,
            first = 2
        )
        assertEquals(0, paging2.size)

        tb.user.changeProposal.assertListFail(
            403,
            projectId = project.id,
            taskId = task1.id
        )

        // join the project
        val createdUser = tb.admin.user.create("testUser", UserRole.USER)
        tb.admin.user.updateUser(userId = createdUser.id!!, user = createdUser.copy(projectIds = arrayOf(project.id), roles = null))
        assertNotNull(
            tb.getUser("testUser@example.com").changeProposal.listChangeProposals(
                projectId = project.id,
                taskId = task1.id
            )
        )
    }

    @Test
    fun testListChangeProposalsFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val milestone2 = tb.admin.milestone.create(projectId = project.id)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val task2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone2.id!!)

        tb.admin.changeProposal.assertListFail(
            404,
            projectId = project.id,
            taskId = task2.id!!
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/changeProposals",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id.toString()
                )
            )
            .build()
            .test()
    }

    @Test
    fun testCreateChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val proposalData = ChangeProposal(
            reason = "reason",
            comment = "comment",
            status = ChangeProposalStatus.PENDING,
            taskId = task1.id!!, startDate = null, endDate = LocalDate.now().toString()
        )
        val changeProposal = tb.admin.changeProposal.create(
            projectId = project.id,
            changeProposal = proposalData
        )!!

        assertEquals(proposalData.reason, changeProposal.reason)
        assertEquals(proposalData.comment, changeProposal.comment)
        assertEquals(proposalData.status, changeProposal.status)
        assertEquals(proposalData.startDate, changeProposal.startDate)
        assertEquals(proposalData.endDate, changeProposal.endDate)
    }

    @Test
    fun testCreateChangeProposalFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val changeProposal = ChangeProposal(
            reason = "reason",
            comment = "comment",
            status = ChangeProposalStatus.PENDING,
            taskId = task1.id!!, startDate = null, endDate = LocalDate.now().toString()
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/changeProposals",
            method = Method.POST,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(changeProposal),
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id.toString()
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    expectedStatus = 400,
                    values = listOf(
                        changeProposal.copy(taskId = UUID.randomUUID())
                    )
                        .map { jacksonObjectMapper().writeValueAsString(it) }
                        .map { SimpleInvalidValueProvider(it) }
                )
            )
            .build()
            .test()
    }

    @Test
    fun testFindChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val changeProposal = tb.admin.changeProposal.create(projectId = project.id, taskId = task1.id!!)!!
        val foundChangeProposal = tb.admin.changeProposal.findChangeProposal(
            projectId = project.id,
            changeProposalId = changeProposal.id!!
        )

        assertEquals(changeProposal.id, foundChangeProposal.id)

        // join the project
        val createdUser = tb.admin.user.create("testUser", UserRole.USER)
        tb.admin.user.updateUser(userId = createdUser.id!!, user = createdUser.copy(projectIds = arrayOf(project.id), roles = null))

        assertNotNull(
            tb.getUser("testUser@example.com").changeProposal.findChangeProposal(
                projectId = project.id,
                changeProposalId = changeProposal.id
            )
        )
    }

    @Test
    fun testFindChangeProposalFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val proposal = tb.admin.changeProposal.create(projectId = project.id, taskId = task1.id!!)

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/changeProposals/{changeProposalId}",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id.toString()
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "changeProposalId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = proposal!!.id.toString()
                )
            )
            .build()
            .test()
    }

    @Test
    fun testUpdateChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val proposal = tb.admin.changeProposal.create(projectId = project.id, taskId = task.id!!)!!
        val updateData = proposal.copy(
            reason = "new reason",
            comment = "new comment",

            taskId = task.id,
            startDate = LocalDate.now().toString(),
            endDate = LocalDate.now().toString()

        )
        val updatedProposal = tb.admin.changeProposal.updateChangeProposal(
            projectId = project.id,
            changeProposalId = proposal.id!!,
            changeProposal = updateData
        )

        assertEquals(updateData.reason, updatedProposal.reason)
        assertEquals(updateData.comment, updatedProposal.comment)
        assertEquals(updateData.startDate, updatedProposal.startDate)
        assertEquals(updateData.endDate, updatedProposal.endDate)
    }

    @Test
    fun testApplyChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!, startDate = "2024-01-01", endDate = "2024-12-31")
        val task1 = tb.admin.task.create(
            projectId = project.id,
            milestoneId = milestone.id!!,
            startDate = "2024-01-01",
            endDate = "2024-01-31"
        )
        val task2 = tb.admin.task.create(
            projectId = project.id,
            milestoneId = milestone.id,
            startDate = "2024-02-01",
            endDate = "2024-02-29"
        )
        val task3 = tb.admin.task.create(
            projectId = project.id,
            milestoneId = milestone.id,
            startDate = "2024-03-01",
            endDate = "2024-03-31"
        )

        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(task1.id!!, task2.id!!, TaskConnectionType.FINISH_TO_START)
        )
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(task2.id, task3.id!!, TaskConnectionType.FINISH_TO_START)
        )
        tb.admin.taskConnection.create(
            projectId = project.id,
            taskConnection = TaskConnection(task1.id, task3.id, TaskConnectionType.FINISH_TO_START)
        )

        // create 4 proposals for various tasks
        tb.admin.changeProposal.create(projectId = project.id, taskId = task1.id)!!
        val proposal2 = tb.admin.changeProposal.create(
            projectId = project.id, ChangeProposal(
                reason = "reason",
                comment = "comment",
                status = ChangeProposalStatus.PENDING,
                taskId = task1.id,
                startDate = "2024-02-01",
                endDate = "2024-02-15"
            )
        )!!
        tb.admin.changeProposal.create(projectId = project.id, taskId = task2.id)!!
        tb.admin.changeProposal.create(projectId = project.id, taskId = task3.id)!!

        // approve single proposal
        tb.admin.changeProposal.updateChangeProposal(
            projectId = project.id,
            changeProposalId = proposal2.id!!,
            changeProposal = proposal2.copy(status = ChangeProposalStatus.APPROVED)
        )

        // Check the status of all other proposals
        val allProposals = tb.admin.changeProposal.listChangeProposals(project.id, milestone.id)
        assertEquals(4, allProposals.size)
        allProposals.forEach {
            if (it.id == proposal2.id) {
                assertEquals(ChangeProposalStatus.APPROVED, it.status)
            } else {
                assertEquals(ChangeProposalStatus.REJECTED, it.status)
            }
        }

        //check that incorrect proposal (breaking the milestone logic) cannot be applied
        val invalidApproval = tb.admin.changeProposal.create(projectId = project.id, taskId = task1.id)!!

        tb.admin.changeProposal.assertUpdateFail(
            expectedStatus = 400,
            projectId = project.id,
            changeProposalId = invalidApproval.id!!,
            changeProposal = invalidApproval.copy(
                status = ChangeProposalStatus.APPROVED,
                startDate = "2024-11-01",
                endDate = "2024-11-10",
            )
        )
    }

    @Test
    fun testUpdateChangeProposalFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val proposal =
            tb.admin.changeProposal.create(projectId = project.id, taskId = task.id!!)!!

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/changeProposals/{changeProposalId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(proposal),
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id.toString()
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "changeProposalId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = proposal.id.toString()
                )
            )
            .body(
                InvalidValueTestScenarioBody(
                    expectedStatus = 400,
                    values = listOf(
                        proposal.copy(taskId = UUID.randomUUID())
                    )
                        .map { jacksonObjectMapper().writeValueAsString(it) }
                        .map { SimpleInvalidValueProvider(it) }
                )
            )
            .build()
            .test()
    }


    @Test
    fun testDeleteChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        // Access rights: assign user to project and try to create/delete proposal
        val createdUser = tb.admin.user.create("testUser", UserRole.USER)
        tb.admin.user.updateUser(userId = createdUser.id!!, user = createdUser.copy(projectIds = arrayOf(project.id), roles = null))

        val proposal = tb.getUser("testUser@example.com").changeProposal.create(projectId = project.id, taskId = task1.id!!)!!
        tb.getUser("testUser@example.com").changeProposal.deleteProposal(
            projectId = project.id,
            changeProposalId = proposal.id!!
        )

        tb.admin.changeProposal.assertFindFail(
            404,
            projectId = project.id,
            changeProposalId = proposal.id
        )
    }

    @Test
    fun testDeleteChangeProposalFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val proposal =
            tb.admin.changeProposal.create(projectId = project.id, taskId = task1.id!!)!!

        // access rights
      /*  tb.user2.changeProposal.assertDeleteFail(
            403,
            projectId = project.id,
            milestoneId = milestone.id,
            changeProposalId = proposal.id!!
        )*/

        InvalidValueTestScenarioBuilder(
            path = "v1/projects/{projectId}/changeProposals/{changeProposalId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .path(
                InvalidValueTestScenarioPath(
                    name = "projectId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = project.id.toString()
                )
            )
            .path(
                InvalidValueTestScenarioPath(
                    name = "changeProposalId",
                    values = InvalidValues.STRING_NOT_NULL,
                    expectedStatus = 404,
                    default = proposal.id.toString()
                )
            )
            .build()
            .test()
    }
}
