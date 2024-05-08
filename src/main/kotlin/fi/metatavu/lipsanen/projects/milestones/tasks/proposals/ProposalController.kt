package fi.metatavu.lipsanen.projects.milestones.tasks.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskEntity
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

@ApplicationScoped
class ProposalController {

    @Inject
    lateinit var proposalRepository: ProposalRepository

    suspend fun create(
        task: TaskEntity,
        proposal: ChangeProposal,
        creatorId: UUID
    ): ProposalEntity {
        return proposalRepository.create(
            id = UUID.randomUUID(),
            task = task,
            reason = proposal.reason,
            comment = proposal.comment,
            status = proposal.status,
            startDate = proposal.taskProposal.startDate,
            endDate = proposal.taskProposal.endDate,
            creatorId = creatorId,
            lastModifierId = creatorId
        )
    }

    suspend fun isCreator(proposal: ProposalEntity, userId: UUID): Boolean {
        return proposal.creatorId == userId
    }

    //todo sorting
    // todo check if query makes sense
    suspend fun listChangeProposals(
        task: TaskEntity,
        first: Int?,
        max: Int?
    ): Pair<List<ProposalEntity>, Long> {
        return proposalRepository.applyFirstMaxToQuery(
            query = proposalRepository.find(
                "task=:task",
                Sort.ascending("createdAt"),
                Parameters.with("task", task)
            ),
            firstIndex = first,
            maxResults = max
        )

    }

    suspend fun find(task: TaskEntity, changeProposalId: UUID): ProposalEntity? {
        return proposalRepository.find(
            "task=:task and id=:id",
            Parameters.with("task", task).and("id", changeProposalId)
        ).firstResult<ProposalEntity>().awaitSuspending()
    }

    suspend fun update(foundProposal: ProposalEntity, changeProposal: ChangeProposal, userId: UUID): ProposalEntity {
        foundProposal.reason = changeProposal.reason
        foundProposal.comment = changeProposal.comment
        foundProposal.startDate = changeProposal.taskProposal.startDate
        foundProposal.endDate = changeProposal.taskProposal.endDate
        foundProposal.lastModifierId = userId
        return proposalRepository.persistSuspending(foundProposal)
    }

    suspend fun delete(proposal: ProposalEntity) {
        proposalRepository.deleteSuspending(proposal)
    }
}