package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.UserModel
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<UserModel, String> {
}