package fi.metatavu.lipsanen.functional.settings

import java.util.UUID

/**
 * Settings implementation for test builder
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 */
class ApiTestSettings {

    companion object {

        /**
         * Returns API service base path
         */
        val apiBasePath: String
            get() = "http://localhost:8081"

        val userId = UUID.fromString("f4c1e6a1-705a-471a-825d-1982b5112ebd")
        val user1Id = UUID.fromString("3d8d282a-e64b-4529-891f-8bad5ce9d8d9")
        val user2Id = UUID.fromString("ef89e98e-6aa3-4511-9b80-ff98bd87fe36")

    }
}