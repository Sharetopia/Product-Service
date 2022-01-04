package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.util.GeoCoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ElasticProductService {
    @Autowired
    private lateinit var elasticProductRepository: ElasticProductRepository

    fun save(elasticProduct: ElasticProductModel): ElasticProductModel{
        return elasticProductRepository.save(elasticProduct)
    }

    fun findByTitle(searchTerm: String, paging: Pageable): Page<ElasticProductModel> {
        return elasticProductRepository.findByTitle(searchTerm, paging)
    }

    fun findByTitleAndNearCoordinates(searchTerm: String, distance: Int, lat:Double, lon: Double, pageable:Pageable): Page<ElasticProductModel>{
        return elasticProductRepository.findByTitleAndNear(searchTerm, distance, lat, lon, pageable)
    }

    fun findByTitleAndNearCity(searchTerm: String, distance: Int ,cityIdentifier: String, pageable: Pageable): Page<ElasticProductModel>{
        val geoCodedCoordinates = GeoCoder.getCoordinatesForCity(cityIdentifier)
        return elasticProductRepository.findByTitleAndNear(searchTerm, distance, geoCodedCoordinates[1], geoCodedCoordinates[0], pageable)
    }

    fun findByTitleAndNearCityWithDate(searchTerm: String, distance: Int, cityIdentifier: String, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<ElasticProductModel>{
        val geoCodedCoordinates = GeoCoder.getCoordinatesForCity(cityIdentifier)
        return elasticProductRepository.findByTitleOrTagsAndAvailabilityAndNear(searchTerm, distance, geoCodedCoordinates[1], geoCodedCoordinates[0],startDate, endDate, pageable)
    }

    fun deleteById(productId: String){
        elasticProductRepository.deleteById(productId)
    }

}