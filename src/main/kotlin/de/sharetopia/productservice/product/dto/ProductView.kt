package de.sharetopia.productservice.product.dto

import org.springframework.data.annotation.Id

class ProductView {
    lateinit var id: String
    lateinit var title: String
    lateinit var description: String
    lateinit var tags: List<String>
}