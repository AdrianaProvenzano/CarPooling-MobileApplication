package it.polito.mad.backToNokia.carpooling.model

import java.io.Serializable

data class User(
    var id: String? = null,
    var name: String? = null,
    var nickname: String? = null,
    var email: String? = null,
    var location: String? = null,
    var imageRef: String? = null
): Serializable

