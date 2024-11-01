package fi.metatavu.lipsanen.rest

sealed class UserRole {

    data object ADMIN :UserRole() {
        const val NAME = "admin"
    }

    data object PROJECT_OWNER : UserRole() {
        const val NAME = "project-owner"
    }

    data object USER : UserRole() {
        const val NAME = "user"
    }
}