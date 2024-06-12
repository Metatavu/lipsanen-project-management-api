package fi.metatavu.lipsanen.exceptions

import java.time.LocalDate
import java.util.*

class UserNotFoundException(userId: UUID) :
    Exception("User $userId not found in the db")