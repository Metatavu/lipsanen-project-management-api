package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.ChangeProposal
import fi.metatavu.lipsanen.test.client.models.ChangeProposalStatus
import fi.metatavu.lipsanen.test.client.models.TaskProposal
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Native tests for ChangeProposals API
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
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)
        val task2 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id)

        val proposals0 = tb.admin.changeProposal.listChangeProposals(projectId = project.id, milestoneId = milestone.id, taskId = task1.id!!)


        val changeProposal = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id!!, taskId = task1.id!!)
        val changeProposal2 = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id, taskId = task2.id!!)
        val changeProposal3 = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id, taskId = task2.id)

        val proposals = tb.admin.changeProposal.listChangeProposals(projectId = project.id, milestoneId = milestone.id, taskId = task1.id)
        assertEquals(1, proposals.size)

        val proposals2 = tb.admin.changeProposal.listChangeProposals(projectId = project.id, milestoneId = milestone.id, taskId = task2.id)
        assertEquals(2, proposals2.size)

        val paging0 = tb.admin.changeProposal.listChangeProposals(projectId = project.id, milestoneId = milestone.id, taskId = task2.id, first = 0)
        assertEquals(2, paging0.size)
        println(paging0.size)
        val paging1 = tb.admin.changeProposal.listChangeProposals(projectId = project.id, milestoneId = milestone.id, taskId = task2.id, first = 1)
        
        println(paging1.size)
        val paging2 = tb.admin.changeProposal.listChangeProposals(projectId = project.id, milestoneId = milestone.id, taskId = task2.id, first = 2)
        println(paging2.size)
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
            taskProposal = TaskProposal(taskId = task1.id!!, startDate = null, endDate = LocalDate.now().toString())
        )
        val changeProposal = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id, taskId = task1.id!!, changeProposal = proposalData)!!

        assertEquals(proposalData.reason, changeProposal.reason)
        assertEquals(proposalData.comment, changeProposal.comment)
        assertEquals(proposalData.status, changeProposal.status)
        assertEquals(proposalData.taskProposal.startDate, changeProposal.taskProposal.startDate)
        assertEquals(proposalData.taskProposal.endDate, changeProposal.taskProposal.endDate)
    }

    @Test
    fun testFindChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val changeProposal = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id, taskId = task1.id!!)!!
        val foundChangeProposal = tb.admin.changeProposal.findChangeProposal(projectId = project.id, milestoneId = milestone.id, taskId = task1.id, changeProposalId = changeProposal.id!!)

        assertEquals(changeProposal.id, foundChangeProposal.id)
    }

    @Test
    fun testUpdateChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val changeProposal = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id, taskId = task1.id!!)!!
        val updateData = changeProposal.copy(
            reason = "new reason",
            comment = "new comment",
            taskProposal = TaskProposal(taskId = task1.id, startDate = LocalDate.now().toString(), endDate = LocalDate.now().toString())
        )
        val updatedProposal = tb.admin.changeProposal.updateChangeProposal(projectId = project.id, milestoneId = milestone.id, taskId = task1.id, changeProposalId = changeProposal.id!!, changeProposal = updateData)

        assertEquals(updateData.reason, updatedProposal.reason)
        assertEquals(updateData.comment, updatedProposal.comment)
        assertEquals(updateData.taskProposal.startDate, updatedProposal.taskProposal.startDate)
        assertEquals(updateData.taskProposal.endDate, updatedProposal.taskProposal.endDate)
    }

    @Test
    fun testDeleteChangeProposal() = createTestBuilder().use { tb ->
        val project = tb.admin.project.create()
        val milestone = tb.admin.milestone.create(projectId = project.id!!)
        val task1 = tb.admin.task.create(projectId = project.id, milestoneId = milestone.id!!)

        val changeProposal = tb.admin.changeProposal.create(projectId = project.id, milestoneId = milestone.id, taskId = task1.id!!)!!
        tb.admin.changeProposal.deleteProposal(projectId = project.id, milestoneId = milestone.id, taskId = task1.id, changeProposalId = changeProposal.id!!)

        tb.admin.changeProposal.assertFindFail(404, projectId = project.id, milestoneId = milestone.id, taskId = task1.id, changeProposalId = changeProposal.id!!)
    }
}