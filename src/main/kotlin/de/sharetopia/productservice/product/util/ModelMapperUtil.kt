package de.sharetopia.productservice.product.util

import org.modelmapper.ModelMapper
import org.modelmapper.convention.MatchingStrategies
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import java.util.stream.Collectors


object ObjectMapperUtils {
    @Autowired
    private var modelMapper: ModelMapper = ModelMapper()

    fun <D, T> map(entity: T, outClass: Class<D>): D {
        return modelMapper.map(entity, outClass)
    }

    fun <D, T> mapAll(entityList: Collection<T>, outCLass: Class<D>): List<D> {
        return entityList.stream()
            .map { entity: T -> map(entity, outCLass) }
            .collect(Collectors.toList())
    }

    fun <D, T> mapEntityPageIntoDtoPage(entities: Page<T>, dtoClass: Class<D>): Page<D> {
        return entities.map { objectEntity: T ->
            modelMapper.map(
                objectEntity,
                dtoClass
            )
        }
    }

    init {
        modelMapper.configuration.matchingStrategy = MatchingStrategies.STRICT
        modelMapper.configuration.isFieldMatchingEnabled = true
        modelMapper.configuration.isSkipNullEnabled = true
    }
}