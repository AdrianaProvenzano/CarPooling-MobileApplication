package it.polito.mad.backToNokia.carpooling.model

data class Booking(
    var id: String? = "",
    var interestedUsers: MutableList<String> = mutableListOf(),
    var bookedUsers: MutableList<String> = mutableListOf(),
)

