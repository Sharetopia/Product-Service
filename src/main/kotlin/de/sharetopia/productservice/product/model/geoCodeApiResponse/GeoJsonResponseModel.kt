package de.sharetopia.productservice.product.model.geoCodeApiResponse

data class GeoCodedAddress (
    val type: String,
    val licence: String,
    val features: List<Feature>
)

data class Feature (
    val type: String,
    val properties: Properties,
    val bbox: List<Double>,
    val geometry: Geometry
)

data class Geometry (
    val type: String,
    val coordinates: List<Double>
)

data class Properties (
    val place_id: Long?,

    val osm_type: String?,

    val osm_id: Long?,

    val display_name: String?,

    val place_rank: Long?,

    val category: String?,
    val type: String?,
    val importance: Double?,
    val icon: String?
)