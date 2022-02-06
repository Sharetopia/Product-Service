package de.sharetopia.productservice.product.dto

import java.time.LocalDate

class RentRequestView {
    lateinit var id: String
    lateinit var fromDate: LocalDate
    lateinit var toDate: LocalDate
    lateinit var requesterUserId: String
    lateinit var rentRequestReceiverUserId: String
    lateinit var requestedProductId: String
    lateinit var status: String
}