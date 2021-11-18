package it.polito.mad.backToNokia.carpooling.model

import com.google.firebase.firestore.GeoPoint
import java.io.Serializable

data class Trip(
        var id: String? = "",
        var user_id: String?="",
        var car_image: String? = "",
        var departure_location: String = "",
        var departure_coordinates: GeoPoint? = null,
        var arrival_location: String = "",
        var arrival_coordinates: GeoPoint? = null,
        var departure_date: String = "",
        var departure_time: String = "",
        var hours_duration: Int? = null,
        var minutes_duration:Int? = null,
        var available_seats: Int? = null,
        var price: Double? = null,
        var description: String = "",
        var stopList: List<Stop>? = null,
) : Serializable

class Stop(
        var location:String = "",
        var location_coordinates: GeoPoint? = null,
        var time:String=""
)
