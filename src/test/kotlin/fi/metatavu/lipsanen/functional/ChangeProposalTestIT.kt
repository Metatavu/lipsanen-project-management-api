package fi.metatavu.lipsanen.functional

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.invalid.*
import fi.metatavu.invalid.providers.SimpleInvalidValueProvider
import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.*
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import io.restassured.http.Method
import org.junit.jupiter.api.Assertions.*
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
        val task1 = tb.admin.task.create(milestoneId = milestone1.id!!)
        val task2 = tb.admin.task.create(milestoneId = milestone2.id!!)
        val task3 = tb.admin.task.create(milestoneId = milestone2.id)

        val proposal1 = tb.admin.changeProposal.create(taskId = task1.id!!)
        val proposal2 = tb.admin.changeProposal.create(taskId = task2.id!!)
        val proposal3 = tb.admin.changeProposal.create(taskId = task3.id!!)

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

        val allProposals = tb.admin.changeProposal.listChangeProposals()
        assertEquals(3, allProposals.size)

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
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)
        val task2 = tb.admin.task.create(milestoneId = milestone2.id!!)

        tb.admin.changeProposal.assertListFail(
            404,
            projectId = project.id,
            taskId = task2.id!!
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/changeProposals",
            method = Method.GET,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
        )
            .query(
                InvalidValueTestScenarioQuery(
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
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)

        val proposalData = ChangeProposal(
            reason = "reason",
            comment = "comment",
            status = ChangeProposalStatus.PENDING,
            taskId = task1.id!!, startDate = null, endDate = LocalDate.now().toString()
        )
        val changeProposal = tb.admin.changeProposal.create(
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
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)
        val changeProposal = ChangeProposal(
            reason = "reason",
            comment = "comment",
            status = ChangeProposalStatus.PENDING,
            taskId = task1.id!!, startDate = null, endDate = LocalDate.now().toString()
        )

        InvalidValueTestScenarioBuilder(
            path = "v1/changeProposals",
            method = Method.POST,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(changeProposal),
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
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)

        val changeProposal = tb.admin.changeProposal.create(taskId = task1.id!!)!!
        val foundChangeProposal = tb.admin.changeProposal.findChangeProposal(
            changeProposalId = changeProposal.id!!
        )

        assertEquals(changeProposal.id, foundChangeProposal.id)

        // join the project
        val createdUser = tb.admin.user.create("testUser", UserRole.USER)
        tb.admin.user.updateUser(userId = createdUser.id!!, user = createdUser.copy(projectIds = arrayOf(project.id), roles = null))

        assertNotNull(
            tb.getUser("testUser@example.com").changeProposal.findChangeProposal(
                changeProposalId = changeProposal.id
            )
        )
    }

    @Test
    fun testFindChangeProposalFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)

        val proposal = tb.admin.changeProposal.create(taskId = task1.id!!)

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
        val task = tb.admin.task.create(milestoneId = milestone.id!!)

        val proposal = tb.admin.changeProposal.create(taskId = task.id!!)!!
        val updateData = proposal.copy(
            reason = "new reason",
            comment = "new comment",

            taskId = task.id,
            startDate = LocalDate.now().toString(),
            endDate = LocalDate.now().toString()

        )
        val updatedProposal = tb.admin.changeProposal.updateChangeProposal(
            changeProposalId = proposal.id!!,
            changeProposal = updateData
        )

        assertEquals(updateData.reason, updatedProposal.reason)
        assertEquals(updateData.comment, updatedProposal.comment)
        assertEquals(updateData.startDate, updatedProposal.startDate)
        assertEquals(updateData.endDate, updatedProposal.endDate)
    }

    /**
     * Starting state:
     * 3 tasks connected by 1-2, 2-3, 1-3, all finish to start types
     * Task1 from 01.01 to 01.31
     * Task2 from 02.01 to 01.29
     * Task3 from 03.01 to 03.31
     */
    @Test
    fun testApplyPreviewChangeProposal(): Unit = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!, startDate = "2022-01-01", endDate = "2024-12-31")
        val task1 = tb.admin.task.create(
            name = "task 1",
            milestoneId = milestone.id!!,
            startDate = "2024-01-01",
            endDate = "2024-01-31"
        )
        val task2 = tb.admin.task.create(
            name = "task 2",
            milestoneId = milestone.id,
            startDate = "2024-02-01",
            endDate = "2024-02-29"
        )
        val task3 = tb.admin.task.create(
            name = "task 3",
            milestoneId = milestone.id,
            startDate = "2024-03-01",
            endDate = "2024-03-31"
        )
        tb.admin.taskConnection.create(projectId = project.id, taskConnection = TaskConnection(task1.id!!, task2.id!!, TaskConnectionType.FINISH_TO_START))
        tb.admin.taskConnection.create(projectId = project.id, taskConnection = TaskConnection(task2.id, task3.id!!, TaskConnectionType.FINISH_TO_START))
        tb.admin.taskConnection.create(projectId = project.id, taskConnection = TaskConnection(task1.id, task3.id, TaskConnectionType.FINISH_TO_START))

        // create 4 proposals for various tasks
        val proposal = tb.admin.changeProposal.create(taskId = task1.id, startDate = "2024-02-01", endDate = "2024-02-15")
        val proposal5Invalid = tb.admin.changeProposal.create(taskId = task1.id, startDate = "2024-12-30", endDate = "2024-12-30")

        // proposal preview shows that the proposal is invalid
        tb.admin.task.assertListFail(400, changeProposalId = proposal5Invalid.id!!)

        val expectedProposalChanges = tb.admin.task.list(changeProposalId = proposal.id!!)
        assertNotNull(expectedProposalChanges)
        assertEquals(3, expectedProposalChanges.size)
        // check that the task dates are not same as what was offered in the proposal preview
        var allTasks = tb.admin.task.list(projectId = project.id, milestoneId = milestone.id)
        expectedProposalChanges.forEach { expected ->
            allTasks.find { it.id == expected.id }?.let { actual ->
                assertNotEquals(expected.startDate, actual.startDate)
                assertNotEquals(expected.endDate, actual.endDate)
            }
        }

        // approve single proposal
        tb.admin.changeProposal.updateChangeProposal(
            changeProposalId = proposal.id,
            changeProposal = proposal.copy(status = ChangeProposalStatus.APPROVED)
        )
        //verify that the predictions for it were correct
        allTasks = tb.admin.task.list(projectId = project.id, milestoneId = milestone.id)
        expectedProposalChanges.forEach { expected ->
            allTasks.find { it.id == expected.id }?.let { actual ->
                assertEquals(expected.startDate, actual.startDate)
                assertEquals(expected.endDate, actual.endDate)
            }
        }

        // Check that all other proposals got auto-rejected and cannot be updated
        val allProposals = tb.admin.changeProposal.listChangeProposals(project.id, milestone.id)
        assertEquals(2, allProposals.size)
        allProposals.forEach {
            if (it.id == proposal.id) {
                assertEquals(ChangeProposalStatus.APPROVED, it.status)
            } else {
                assertEquals(ChangeProposalStatus.REJECTED, it.status)
            }
            tb.admin.changeProposal.assertUpdateFail(400, it.id!!, it)
        }

        //check that incorrect proposal (breaking the milestone logic) cannot be applied
        val invalidApproval = tb.admin.changeProposal.create(taskId = task1.id)!!
        tb.admin.changeProposal.assertUpdateFail(
            expectedStatus = 400,
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
        val task = tb.admin.task.create(milestoneId = milestone.id!!)

        val proposal =
            tb.admin.changeProposal.create(taskId = task.id!!)!!

        InvalidValueTestScenarioBuilder(
            path = "v1/changeProposals/{changeProposalId}",
            method = Method.PUT,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
            body = jacksonObjectMapper().writeValueAsString(proposal),
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
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)

        // Access rights: assign user to project and try to create/delete proposal
        val createdUser = tb.admin.user.create("testUser", UserRole.USER)
        tb.admin.user.updateUser(userId = createdUser.id!!, user = createdUser.copy(projectIds = arrayOf(project.id), roles = null))

        val proposal = tb.getUser("testUser@example.com").changeProposal.create(taskId = task1.id!!)!!
        tb.getUser("testUser@example.com").changeProposal.deleteProposal(
            changeProposalId = proposal.id!!
        )

        tb.admin.changeProposal.assertFindFail(
            404,
            changeProposalId = proposal.id
        )
    }

    @Test
    fun testDeleteChangeProposalFail() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(milestoneId = milestone.id!!)

        val proposal =
            tb.admin.changeProposal.create(taskId = task1.id!!)!!

        // access rights
      /*  tb.user2.changeProposal.assertDeleteFail(
            403,
            projectId = project.id,
            milestoneId = milestone.id,
            changeProposalId = proposal.id!!
        )*/

        InvalidValueTestScenarioBuilder(
            path = "v1/changeProposals/{changeProposalId}",
            method = Method.DELETE,
            token = tb.admin.accessTokenProvider.accessToken,
            basePath = ApiTestSettings.apiBasePath,
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
