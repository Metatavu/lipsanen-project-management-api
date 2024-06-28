package fi.metatavu.lipsanen.functional

import fi.metatavu.lipsanen.functional.resources.KeycloakResource
import fi.metatavu.lipsanen.functional.settings.DefaultTestProfile
import fi.metatavu.lipsanen.test.client.models.JobPosition
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for job positions
 */
@QuarkusTest
@TestProfile(DefaultTestProfile::class)
@QuarkusTestResource.List(
    QuarkusTestResource(KeycloakResource::class),
)
class JobPositionsTestIT : AbstractFunctionalTest() {

    @Test
    fun listTest() = createTestBuilder().use { tb ->
        tb.admin.jobPosition.create("a")
        tb.admin.jobPosition.create("b")
        val jobPositions = tb.admin.jobPosition.listJobPositions()
        assertEquals(2, jobPositions.size)
    }

    @Test
    fun createTest() = createTestBuilder().use { tb ->
        val jobPositionData = JobPosition(
            name = "Test",
            color = "#000000",
            iconName = "test"
        )
        val jobPosition = tb.admin.jobPosition.create(jobPositionData)
        assertEquals(jobPositionData.name, jobPosition.name)
        assertEquals(jobPositionData.color, jobPosition.color)
        assertEquals(jobPositionData.iconName, jobPosition.iconName)
        assertNotNull(jobPosition.id)

        tb.admin.jobPosition.assertCreateFails(400, jobPositionData)
        tb.user.jobPosition.assertCreateFails(403, jobPositionData)
    }

    @Test
    fun findTest() = createTestBuilder().use { tb ->
        val jobPositionData = JobPosition(
            name = "Test",
            color = "#000000",
            iconName = "test"
        )
        val jobPosition = tb.admin.jobPosition.create(jobPositionData)
        val foundJobPosition = tb.admin.jobPosition.findJobPosition(jobPosition.id!!)
        assertEquals(jobPosition.id, foundJobPosition.id)
        assertEquals(jobPosition.name, foundJobPosition.name)
        assertEquals(jobPosition.color, foundJobPosition.color)
        assertEquals(jobPosition.iconName, foundJobPosition.iconName)
    }

    @Test
    fun updateTest() = createTestBuilder().use { tb ->
        val jobPosition = tb.admin.jobPosition.create(JobPosition(
            name = "Test",
            color = "#000000",
            iconName = "test"
        ))
        val jobPosition2 = tb.admin.jobPosition.create("Test2")

        val updateJobPositionData = jobPosition.copy(
            name = "Test3",
            color = "#000001",
            iconName = "test3"
        )
        val updatedJobPosition = tb.admin.jobPosition.updateJobPosition(jobPosition.id!!, updateJobPositionData)
        assertEquals(jobPosition.id, updatedJobPosition.id)
        assertEquals(updateJobPositionData.name, updatedJobPosition.name)
        assertEquals(updateJobPositionData.color, updatedJobPosition.color)
        assertEquals(updateJobPositionData.iconName, updatedJobPosition.iconName)

        tb.admin.jobPosition.assertUpdateFails(404, UUID.randomUUID(), updateJobPositionData)
        tb.admin.jobPosition.assertUpdateFails(400, updatedJobPosition.id!!, updateJobPositionData.copy(name = jobPosition2.name))
        tb.user.jobPosition.assertUpdateFails(403, updatedJobPosition.id, updateJobPositionData)
    }

    @Test
    fun deleteTest() = createTestBuilder().use { tb ->
        val jobPositionData = JobPosition(
            name = "Test",
            color = "#000000",
            iconName = "test"
        )
        val jobPosition = tb.admin.jobPosition.create(jobPositionData)
        tb.user.jobPosition.assertDeleteFails(403, jobPosition.id!!)
        tb.admin.jobPosition.deleteJobPosition(jobPosition.id)

        tb.admin.jobPosition.assertFindFails(404, jobPosition.id)
    }

}