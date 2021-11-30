package de.sharetopia.productservice.product.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "products")
public data class ProductModel(@Id var id: ObjectId = ObjectId.get(), var title: String="", var description: String="", var tags: List<String> = listOf())
