package de.sharetopia.productservice.product.util

import de.sharetopia.productservice.product.exception.LocationNotFoundException
import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.geoCodeApiResponse.GeoCodedAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    private val log: Logger = LoggerFactory.getLogger(GeoCoder::class.java)

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
        return if (geoResult != null && geoResult.features.isNotEmpty()) {
            geoResult.features[0].geometry.coordinates
        } else{
            log.error("Error geocoding address to coordinates. {error=LocationNotFoundException}")
            throw LocationNotFoundException(address.toString())
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
        return if (geoResult != null && geoResult.features.isNotEmpty()) {
            geoResult.features[0].geometry.coordinates
        } else{
            log.error("Error geocoding city identifier to coordinates. {error=LocationNotFoundException}")
            throw LocationNotFoundException(nameOrZip)
        }

    }
}