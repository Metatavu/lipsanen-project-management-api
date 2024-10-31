package fi.metatavu.lipsanen.tocoman

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection
import java.util.*

/**
 * POJO for Tocoman XML projects
 */
@RegisterForReflection
class Project {
    @JsonProperty("ID")
    var id: Int = 0
    @JsonProperty("ProjName")
    var projName: String? = null
    @JsonProperty("Created")
    var created: Date? = null
    @JsonProperty("Changed")
    var changed: Date? = null
}
@RegisterForReflection
class Projects {
    @JsonProperty("Project")
    var project: Project? = null
}

