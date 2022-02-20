package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.UserDTO
import de.sharetopia.productservice.product.exception.UserNotFoundException
import de.sharetopia.productservice.product.model.UserModel
import de.sharetopia.productservice.product.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository

    private val log: Logger = LoggerFactory.getLogger(UserService::class.java)

    fun save(user: UserModel, userId: String): UserModel {
        user.id = userId
        return userRepository.save(user)
    }

    fun updateOrInsert(userId: String, userModel: UserModel): UserModel {
        userModel.id = userId
        return userRepository.save(userModel)
    }

    fun partialUpdate(userId: String, updatedFieldsUserDTO: UserDTO): UserModel {
        val storedUserModel = findById(userId)
        storedUserModel.id = userId
        val updatedModel = storedUserModel.copy(
            profilePictureURL = updatedFieldsUserDTO.profilePictureURL ?: storedUserModel.profilePictureURL,
            forename = updatedFieldsUserDTO.forename ?: storedUserModel.forename,
            surname = updatedFieldsUserDTO.surname ?: storedUserModel.surname,
            address = updatedFieldsUserDTO.address ?: storedUserModel.address,
            city = updatedFieldsUserDTO.city ?: storedUserModel.city,
            rating = updatedFieldsUserDTO.rating ?: storedUserModel.rating,
            postalCode = updatedFieldsUserDTO.postalCode ?: storedUserModel.postalCode
        )
        return userRepository.save(updatedModel)
    }

    fun findById(userId: String): UserModel {
        return userRepository.findById(userId).orElseThrow {
            log.error("Error fetching current authorized user. {error=UserNotFoundException, requesterUserId=${userId}}")
            UserNotFoundException(userId)
        }
    }

}