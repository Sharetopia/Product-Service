package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.UserDTO
import de.sharetopia.productservice.product.dto.UserProductsWithRentRequestsView
import de.sharetopia.productservice.product.dto.UserSentRentRequestsWithProductsView
import de.sharetopia.productservice.product.dto.UserView
import de.sharetopia.productservice.product.model.UserModel
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.UserService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/")
@Tag(name = "User", description = "Endpoints for managing user data")
class UserController {
    @Autowired
    private lateinit var productService: ProductService

    @Autowired
    private lateinit var userService: UserService

    private val log: Logger = LoggerFactory.getLogger(UserController::class.java)

    @Operation(summary = "Create user object for current authorized user")
    @PostMapping("/user")
    fun createUser(@RequestBody userDTO: UserDTO, principal: Principal): ResponseEntity<UserView> {
        log.info("Creating user. {method=POST, endpoint=/user, requesterUserId=${principal.name}}")
        var user = ObjectMapperUtils.map(userDTO, UserModel::class.java)
        var createdUser = userService.save(user, principal.name)
        return ResponseEntity.ok(ObjectMapperUtils.map(createdUser, UserView::class.java))
    }

    @Operation(
        summary = "Update or insert user",
        description = "Updates/inserts user depending on if the given id already exists"
    )
    @PutMapping("/user")
    fun updateOrInsertCurrentAuthorizedUser(
        @RequestBody userDTO: UserDTO,
        principal: Principal
    ): ResponseEntity<UserView> {
        val authenticatedUserId = principal.name
        log.info("Updating/insertíng user. {method=PUT, endpoint=/user, requesterUserId=${authenticatedUserId}}")
        val requestUserModel = ObjectMapperUtils.map(userDTO, UserModel::class.java)
        val updatedUserModel = userService.updateOrInsert(authenticatedUserId, requestUserModel)
        return ResponseEntity.ok(ObjectMapperUtils.map(updatedUserModel, UserView::class.java))
    }

    @Operation(
        summary = "Updates product by id",
        description = "Updates the provided fields of the product which belongs to the given id."
    )
    @PatchMapping("/user")
    fun partialUpdate(
        @RequestBody updatedFieldsUserDTO: UserDTO,
        principal: Principal
    ): UserView {
        val authenticatedUserId = principal.name
        log.info("Partial update to current authorized user. {method=PATCH, endpoint=/user, requesterUserId=${principal.name}}")
        val updatedUser = userService.partialUpdate(authenticatedUserId, updatedFieldsUserDTO)
        return ObjectMapperUtils.map(updatedUser, UserView::class.java)
    }

    @Operation(summary = "Gets user information about currently authorized user")
    @GetMapping("/user")
    fun getCurrentAuthorizedUser(principal: Principal): ResponseEntity<UserView> {
        var authenticatedUserId = principal.name
        log.info("Fetching current authorized user. {method=GET, endpoint=/user, requesterUserId=${authenticatedUserId}}")
        var user = userService.findById(authenticatedUserId)
        return ResponseEntity.ok(ObjectMapperUtils.map(user, UserView::class.java))
    }

    @Operation(summary = "Gets user by id")
    @GetMapping("/user/{id}")
    fun getUser(@PathVariable(value = "id") id: String, principal: Principal): ResponseEntity<UserModel> {
        log.info("Fetching user by id. {method=GET, endpoint=/user/{id}, requestedUserId=$id, requesterUserId=${principal.name}}")
        var user = userService.findById(id)
        return ResponseEntity.ok(user)
    }

    @Operation(summary = "Gets offered products and corresponding rent requests for authenticated user")
    @GetMapping("/user/offeredProductsOverview")
    fun getOfferedProductsOfUser(principal: Principal): ResponseEntity<MutableList<UserProductsWithRentRequestsView>> {
        var currentUserId = principal.name
        log.info("Fetching offered Products offered by current authorized user. {method=GET, endpoint=/user/offeredProductsOverview, requesterUserId=${principal.name}}")
        var productsAndRentRequestsForUser = productService.getProductsWithRentRequestsForUser(currentUserId)
        return ResponseEntity.ok(productsAndRentRequestsForUser)
    }

    @Operation(summary = "Gets rent requests with corresponding products for authenticated user")
    @GetMapping("/user/requestedProductsOverview")
    fun getRentRequestsOfUser(principal: Principal): ResponseEntity<MutableList<UserSentRentRequestsWithProductsView>> {
        var currentUserId = principal.name
        log.info("Fetching rent request for products offered by current authorized user. {method=GET, endpoint=/user/requestedProductsOverview, requesterUserId=${principal.name}}")
        var rentRequestsWithProducts = productService.getRentRequestsWithProducts(currentUserId)
        return ResponseEntity.ok(rentRequestsWithProducts)
    }
}