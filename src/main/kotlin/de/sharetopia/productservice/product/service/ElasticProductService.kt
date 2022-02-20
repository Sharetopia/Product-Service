package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.exception.InvalidDateRangeSearchException
import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.util.GeoCoder
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ElasticProductService {
    @Autowired
    private lateinit var elasticProductRepository: ElasticProductRepository

    private val log: Logger = LoggerFactory.getLogger(ElasticProductService::class.java)

    fun save(elasticProduct: ElasticProductModel): ElasticProductModel {
        return elasticProductRepository.save(elasticProduct)
    }

    fun findByTitle(searchTerm: String, paging: Pageable): Page<ElasticProductModel> {
        return elasticProductRepository.findByTitle(searchTerm, paging)
    }

    fun findByTitleAndNearCoordinates(
        searchTerm: String,
        distance: Int,
        lat: Double,
        lon: Double,
        pageable: Pageable
    ): Page<ElasticProductModel> {
        return elasticProductRepository.findByTitleAndNear(searchTerm, distance, lat, lon, pageable)
    }

    fun findByTitleAndNearCityWithOptionalDate(
        searchTerm: String,
        distance: Int,
        cityIdentifier: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): Page<ElasticProductModel> {
        val geoCodedCoordinates = GeoCoder.getCoordinatesForCity(cityIdentifier)
        if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
            log.error("Error searching for product by search term, distance, cityIdentifer and Date. {error=invalidDateRangeSearchException, searchTerm=$searchTerm, cityIdentifier=$cityIdentifier, distance=$distance, startDate=$startDate, endDate=$endDate")
            throw InvalidDateRangeSearchException()
        } else if (startDate != null && endDate != null) {
            return elasticProductRepository.findByTitleOrTagsAndAvailabilityAndNear(
                searchTerm,
                distance,
                geoCodedCoordinates[1],
                geoCodedCoordinates[0],
                startDate,
                endDate,
                pageable
            )
        } else {
            return elasticProductRepository.findByTitleAndNear(
                searchTerm,
                distance,
                geoCodedCoordinates[1],
                geoCodedCoordinates[0],
                pageable
            )
        }
    }

    fun deleteById(productId: String) {
        elasticProductRepository.deleteById(productId)
    }

}