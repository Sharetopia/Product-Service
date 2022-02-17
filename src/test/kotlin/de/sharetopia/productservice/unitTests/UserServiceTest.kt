package de.sharetopia.productservice.unitTests

import de.sharetopia.productservice.product.dto.UserDTO
import de.sharetopia.productservice.product.exception.UserNotFoundException
import de.sharetopia.productservice.product.model.UserModel
import de.sharetopia.productservice.product.repository.UserRepository
import de.sharetopia.productservice.product.service.UserService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*


@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should save and return user`() {
        val userToCreate = UserModel(
            profilePictureURL = "www.test.de/1234",
            forename = "Thomas",
            surname = "Test",
            address = "Testweg 12",
            rating = "5",
            postalCode = "404"
        )
        whenever(userRepository.save(any<UserModel>())).thenReturn(userToCreate)
        //test
        userService.save(userToCreate, "1234")
        verify(userRepository, times(1)).save(argThat {
            (id === "1234") &&
            (profilePictureURL === userToCreate.profilePictureURL) &&
            (forename === userToCreate.forename) &&
            (surname === userToCreate.surname) &&
            (address === userToCreate.address) &&
            (rating === userToCreate.rating) &&
            (postalCode === userToCreate.postalCode)
        })
    }

    @Test
    fun `should update or create user`() {
        val userToCreate = UserModel(
            profilePictureURL = "www.test.de/1234",
            forename = "Thomas",
            surname = "Test",
            address = "Testweg 12",
            rating = "5",
            postalCode = "404"
        )

        whenever(userRepository.save(any<UserModel>())).thenReturn(userToCreate)

        //test
        userService.updateOrInsert("1234", userToCreate)

        verify(userRepository, times(1)).save(argThat {
            (id === "1234") &&
            (profilePictureURL === "www.test.de/1234") &&
            (forename === "Thomas") &&
            (surname === "Test") &&
            (address === "Testweg 12") &&
            (rating === "5") &&
            (postalCode === "404")
        })
    }

    @Test
    fun `should perform partial update on user`() {
        val mockedUserInDb = UserModel(
            id = "1234",
            profilePictureURL = "www.test.de/1234",
            forename = "Thomas",
            surname = "Test",
            address = "Testweg 12",
            rating = "5",
            postalCode = "404"
        )

        val updatedFieldsUser =
            UserDTO(
                rating = "3"
            )

        whenever(userRepository.save(any<UserModel>())).doAnswer { it.arguments[0] as UserModel }

        //test
        val userReturnedByUser = userService.partialUpdate("1234", mockedUserInDb, updatedFieldsUser)

        verify(userRepository, times(1)).save(argThat { userModel: UserModel ->
            (userModel.id === mockedUserInDb.id) &&
            (userModel.profilePictureURL === mockedUserInDb.profilePictureURL) &&
            (userModel.forename === mockedUserInDb.forename) &&
            (userModel.surname === mockedUserInDb.surname) &&
            (userModel.address === mockedUserInDb.address) &&
            (userModel.rating === updatedFieldsUser.rating) &&
            (userModel.postalCode === mockedUserInDb.postalCode)
        })
    }

    @Test
    fun `should find user by id`() {
        val mockedUserInDb = UserModel(
            id = "1234",
            profilePictureURL = "www.test.de/1234",
            forename = "Thomas",
            surname = "Test",
            address = "Testweg 12",
            rating = "5",
            postalCode = "404"
        )
        whenever(userRepository.findById(any<String>())).thenReturn(Optional.of(mockedUserInDb))

        //test
        val userReturnedByUser = userService.findById("1234")
        verify(userRepository, times(1)).findById("1234")
    }
}