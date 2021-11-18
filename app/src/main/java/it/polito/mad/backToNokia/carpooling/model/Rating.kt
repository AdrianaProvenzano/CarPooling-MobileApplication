package it.polito.mad.backToNokia.carpooling.model

data class Rating(
        var rated_user:String?="",
        var rating_user:String?="",
        var booking:String?="",
        var score:Float?=null,
        var comment:String?="",
        var driver:Boolean=false
)


