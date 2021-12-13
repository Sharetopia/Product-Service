package de.sharetopia.productservice.product.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Document

/*@Document(collection = "products")
public data class ProductModel(@Id var id: ObjectId = ObjectId.get(), var title: String="",
                               var description: String="", var tags: List<String> = listOf(),
                               @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE) var location: GeoJsonPoint?=null)*/

@Document(collection = "products")
public data class ProductModel(@Id var id: ObjectId = ObjectId.get(), var title: String="",
                               var description: String="", var tags: List<String> = listOf(),
                               @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE) var location: List<Double>?=null)