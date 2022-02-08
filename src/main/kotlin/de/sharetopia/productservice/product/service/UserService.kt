package de.sharetopia.productservice.product.service

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

    fun findById(userId: String): Optional<UserModel> {
        return userRepository.findById(userId)
    }

}