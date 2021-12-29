package de.sharetopia.productservice

import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "products")
data class ProductModel(var title: String, var description: String, val tags: List<String>)
