package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.util.GeoCoder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.lang.Integer.parseInt

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

    /*fun findByTitleAndNearCoordinates(searchTerm: String, distance: Int, pageable:Pageable, lat:Double, lon: Double): Page<ElasticProductModel>{
        return elasticProductRepository.findByTitleAndNear(searchTerm, distance, pageable, lat, lon)
    }*/

    fun findByTitleAndNearCity(searchTerm: String, distance: Int, pageable: Pageable ,cityIdentifier: String): Page<ElasticProductModel>{
        val geoCodedCoordinates = GeoCoder.getCoordinatesForCity(cityIdentifier)
        return elasticProductRepository.findByTitleAndNear(searchTerm, distance, pageable, geoCodedCoordinates[1], geoCodedCoordinates[0])
    }

    fun deleteById(productId: String){
        elasticProductRepository.deleteById(productId)
    }

}