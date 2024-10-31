package fi.metatavu.lipsanen.exceptions

import java.util.*

/**
 * Exception for various cases of user not found
 */
class UserNotFoundException(userId: UUID) :
    Exception("User $userId not found")