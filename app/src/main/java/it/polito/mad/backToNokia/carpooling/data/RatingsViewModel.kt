package it.polito.mad.backToNokia.carpooling.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.backToNokia.carpooling.model.Booking
import it.polito.mad.backToNokia.carpooling.model.Rating
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
class RatingsViewModel: ViewModel() {
/*    private val ratingsList: MutableLiveData<MutableList<Rating>> by lazy {
        MutableLiveData<MutableList<Rating>>()
    }

    init {
        viewModelScope.launch {
            FirebaseService.getRatings().collect{ listRatings ->
                ratingsList.value = listRatings.toMutableList()
                }
        }
    }*/

/*    fun getRatingList(): LiveData<MutableList<Rating>> {
        return ratingsList
    }

    suspend fun getRating(rated_user:String, rating_user:String, booking:String): Rating? {
        return withContext(Dispatchers.IO) { FirebaseService.getRating(rated_user, rating_user, booking) }
    }*/

    suspend fun addRating(rating: Rating): String{
        var newId: String
        withContext(Dispatchers.IO) {
            newId = FirebaseService.addRating(rating).toString()
        }
        return newId
    }

    suspend fun getDriverRatings(userId: String, tripList: List<String>): List<Rating>?{
        return withContext(Dispatchers.IO) {
            FirebaseService.getDriverRatings(userId, tripList)
        }
    }

    suspend fun getPassengerRatings(userId: String, bookedList: List<String>): List<Rating>?{
        return withContext(Dispatchers.IO) {
            FirebaseService.getPassengerRatings(userId, bookedList)
        }
    }

    suspend fun getRatingsByUserId(userId: String, bookedList: List<String>, myTrips: List<String>): List<Rating>? {
        return withContext(Dispatchers.IO) {
            FirebaseService.getRatingsByUserId(userId, bookedList, myTrips)
        }
    }

    suspend fun driverRatingExist(id: String): Boolean {
        return withContext(Dispatchers.IO){
            FirebaseService.driverRatingExist(id)
        }
    }

    suspend fun allPassengerRatingsExist(id: String, passengers: Int): Boolean {
        return withContext(Dispatchers.IO){
            FirebaseService.allPassengerRatingsExist(id, passengers)
        }
    }


    /*suspend fun getBooking(id: String): Booking?{
        return withContext(Dispatchers.IO) { FirebaseService.getBooking(id) }
    }

    suspend fun setInterestedUser(bookingId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) { FirebaseService.setInterestedUser(bookingId, userId) }
    }*/
}