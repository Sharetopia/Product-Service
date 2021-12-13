package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.model.ProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

public interface ElasticProductRepository: ElasticsearchRepository<ElasticProductModel, String> {
    @Query("{\"bool\": { \n" +
            "      \"must\": [\n" +
            "        { \"match_phrase_prefix\": { \"title\":   \"?0\"        }}\n" +
            "      ],\n" +
            "      \"filter\": [ \n" +
            "        {\n" +
            "        \"geo_distance\": {\n" +
            "          \"distance\": \"?1" + "km\",\n" +
            "          \"location\": {\n" +
            "            \"lat\": ?2,\n" +
            "            \"lon\": ?3\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "      ]\n" +
            "    }}")
    fun findByTitleAndNear(searchTerm: String, Distance: Int, pageable:Pageable, lat:Double, lon: Double): Page<ElasticProductModel>

    @Query("{\"match_phrase_prefix\": {\n" +
            "          \"title\": \"?0\"\n" +
            "        }}")
    fun findByTitle(searchTerm: String, pageable:Pageable): Page<ElasticProductModel>

}