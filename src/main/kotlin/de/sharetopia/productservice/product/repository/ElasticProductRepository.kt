package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.ElasticProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import java.time.LocalDate

public interface ElasticProductRepository: ElasticsearchRepository<ElasticProductModel, String> {
    @Query("{ \n" +
            "    \"bool\": { \n" +
            "      \"should\": [\n" +
            "        { \"match_phrase_prefix\": { \"title\":   \"?0\"        }},\n" +
            "        { \"match_phrase_prefix\": { \"tags\":   \"?0\"        }}\n" +
            "      ],\n" +
            "      \"minimum_should_match\" : 1,\n" +
            "      \"filter\": [ \n" +
            "        {\n" +
            "        \"geo_distance\": {\n" +
            "          \"distance\": \"?1"+"km\",\n" +
            "          \"location\": {\n" +
            "            \"lat\": ?2,\n" +
            "            \"lon\": ?3\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "      ]\n" +
            "    }\n" +
            "  }")
    fun findByTitleAndNear(searchTerm: String, distance: Int, lat:Double, lon: Double, pageable: Pageable): Page<ElasticProductModel>

    @Query("{\n" +
            "        \"bool\": {\n" +
            "            \"should\": [\n" +
            "                {\n" +
            "                    \"match_phrase_prefix\": {\n" +
            "                        \"title\": \"?0\"\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"match_phrase_prefix\": {\n" +
            "                        \"tags\": \"?0\"\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"minimum_should_match\": 1,\n" +
            "            \"filter\": [\n" +
            "                {\n" +
            "                    \"geo_distance\": {\n" +
            "                        \"distance\": \"?1"+"km\",\n" +
            "                        \"location\": {\n" +
            "                            \"lat\": ?2,\n" +
            "                            \"lon\": ?3\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"nested\": {\n" +
            "                        \"path\": \"rents\",\n" +
            "                        \"query\": {\n" +
            "                            \"bool\": {\n" +
            "                                \"must_not\": [\n" +
            "                                    {\n" +
            "                                        \"range\": {\n" +
            "                                            \"rents.rentDuration\": {\n" +
            "                                                \"gte\": \"?4\",\n" +
            "                                                \"lte\": \"?5\",\n" +
            "                                                \"relation\": \"intersects\"\n" +
            "                                            }\n" +
            "                                        }\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                {\n" +
            "                    \"range\": {\n" +
            "                        \"rentableDateRange\": {\n" +
            "                            \"gte\": \"?4\",\n" +
            "                            \"lt\": \"?5\",\n" +
            "                            \"relation\": \"contains\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    }")
    fun findByTitleOrTagsAndAvailabilityAndNear(searchTerm: String, distance: Int, lat:Double, lon: Double, startDate: LocalDate, endDate: LocalDate, pageable: Pageable): Page<ElasticProductModel>

    @Query("{\"match_phrase_prefix\": {\n" +
            "          \"title\": \"?0\"\n" +
            "        }}")
    fun findByTitle(searchTerm: String, pageable:Pageable): Page<ElasticProductModel>

}