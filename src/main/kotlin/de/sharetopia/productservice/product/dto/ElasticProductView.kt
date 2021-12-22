package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.Address

class ElasticProductView {
    lateinit var id: String
    lateinit var title: String
    lateinit var description: String
    lateinit var tags: List<String>
    lateinit var location: List<Double>
}
