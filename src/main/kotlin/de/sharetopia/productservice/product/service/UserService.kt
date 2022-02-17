package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.UserDTO
import de.sharetopia.productservice.product.model.UserModel
import de.sharetopia.productservice.product.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository

    fun save(user: UserModel, userId: String): UserModel {
        user.id = userId
        return userRepository.save(user)
    }

    fun updateOrInsert(userId: String, userModel: UserModel): UserModel {
        userModel.id = userId
        return userRepository.save(userModel)
    }

    fun partialUpdate(userId: String, storedUserModel: UserModel, updatedFieldsUserDTO: UserDTO): UserModel {
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

    fun findById(userId: String): Optional<UserModel> {
        return userRepository.findById(userId)
    }

}