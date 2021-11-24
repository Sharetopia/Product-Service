package de.sharetopia.productservice

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
public data class Product(var title: String, var description: String, val tags: List<String>)
