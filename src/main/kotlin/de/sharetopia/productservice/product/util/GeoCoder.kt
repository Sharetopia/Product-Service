package de.sharetopia.productservice.product.util

import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.geoCodeApiResponse.GeoCodedAddress
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

object GeoCoder {
    private val urlString = "https://nominatim.openstreetmap.org/search?q={q}&format={format}&countrycodes={countrycodes}&limit={limit}"
    private var vars: MutableMap<String, String> = hashMapOf("format" to "geojson", "countrycodes" to "de", "limit" to "1")
    private val restTemplate = RestTemplate()
    private var headers = HttpHeaders()
    private val entity: HttpEntity<String>

    init{
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.contentType = MediaType.APPLICATION_JSON
        headers.add("user-agent", "Mozilla/5.0 Firefox/26.0")
        entity = HttpEntity<String>("parameters", headers)
    }

    fun getCoordinatesForAddress(address: Address): List<Double>{
        vars["q"] = "${address.street},${address.zip},${address.city}"

        val response = restTemplate.exchange(
            urlString, HttpMethod.GET, entity,
            GeoCodedAddress::class.java,
            vars
        )

        val geoResult = response.body
        return if (geoResult != null) {
            geoResult.features[0].geometry.coordinates
        } else{
            (listOf(0.0,0.0)) //TODO throw exception?
        }

    }

    fun getCoordinatesForCity(nameOrZip: String): List<Double>{
        vars["q"] = nameOrZip

        val response = restTemplate.exchange(
            urlString, HttpMethod.GET, entity,
            GeoCodedAddress::class.java,
            vars
        )

        val geoResult = response.body
        return if (geoResult != null) {
            geoResult.features[0].geometry.coordinates
        } else{
            (listOf(0.0,0.0)) //TODO throw exception?
        }

    }
}