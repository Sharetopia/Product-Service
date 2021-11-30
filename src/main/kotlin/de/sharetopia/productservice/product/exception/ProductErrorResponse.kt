package de.sharetopia.productservice.product.exception

class ProductErrorResponse(var status : Int? = null,
                           var message : String? = null,
                           var timeStamp : Long? = null) {

}