package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.UserDTO
import de.sharetopia.productservice.product.dto.UserProductsWithRentRequestsView
import de.sharetopia.productservice.product.dto.UserSentRentRequestsWithProductsView
import de.sharetopia.productservice.product.dto.UserView
import de.sharetopia.productservice.product.exception.UserNotFoundException
import de.sharetopia.productservice.product.exception.NotAllowedAccessToResource
import de.sharetopia.productservice.product.model.UserModel
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.UserService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/")
@Tag(name = "User", description = "Endpoints for managing user data")
class UserController {
    @Autowired
    private lateinit var productService: ProductService
    @Autowired
    private lateinit var userService: UserService

    @Operation(summary = "Create user object for current authorized user")
    @PostMapping("/user")
    fun createUser(@RequestBody userDTO: UserDTO, principal: Principal): ResponseEntity<UserView> {
        var user = ObjectMapperUtils.map(userDTO, UserModel::class.java)
        var createdUser = userService.save(user, principal.name)
        return ResponseEntity.ok(ObjectMapperUtils.map(createdUser, UserView::class.java))
    }

    @Operation(summary = "Gets user information about currently authorized user")
    @GetMapping("/user")
    fun getCurrentAuthorizedUser(principal: Principal): ResponseEntity<UserView> {
        var currentUserId = principal.name
        var user = userService.findById(currentUserId).orElseThrow{ UserNotFoundException(currentUserId) }
        return ResponseEntity.ok(ObjectMapperUtils.map(user, UserView::class.java))
    }

    @Operation(summary = "Gets user by id")
    @GetMapping("/user/{userId}")
    fun getUser(@PathVariable(value = "id") userId: String, principal: Principal): ResponseEntity<Optional<UserModel>> {
        if(principal.name!=userId){
            throw NotAllowedAccessToResource(principal.name)
        }
        var user = userService.findById(userId)
        return ResponseEntity.ok(user)
    }

    @Operation(summary = "Gets offered products and corresponding rent requests for authenticated user")
    @GetMapping("/user/offeredProductsOverview")
    fun getOfferedProductsOfUser(principal: Principal): ResponseEntity<MutableList<UserProductsWithRentRequestsView>> {
        var currentUserId = principal.name
        var productsAndRentRequestsForUser = productService.getProductsWithRentRequestsForUser(currentUserId)
        return ResponseEntity.ok(productsAndRentRequestsForUser)
    }

    @Operation(summary = "Gets rent requests with corresponding products for authenticated user")
    @GetMapping("/user/requestedProductsOverview")
    fun getRentRequestsOfUser(principal: Principal): ResponseEntity<MutableList<UserSentRentRequestsWithProductsView>> {
        var currentUserId = principal.name
        var rentRequestsWithProducts = productService.getRentRequestsWithProducts(currentUserId)
        return ResponseEntity.ok(rentRequestsWithProducts)
    }
}