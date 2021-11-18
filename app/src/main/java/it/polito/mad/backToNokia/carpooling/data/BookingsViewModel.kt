package it.polito.mad.backToNokia.carpooling.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.backToNokia.carpooling.model.Booking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
class BookingsViewModel: ViewModel() {

    private val bookingList: MutableLiveData<List<Booking>> by lazy {
        MutableLiveData<List<Booking>>()
    }

    private val interestedList: MutableLiveData<List<Booking>> by lazy {
        MutableLiveData<List<Booking>>()
    }

    private val bookedList: MutableLiveData<List<Booking>> by lazy {
        MutableLiveData<List<Booking>>()
    }

    init {
        val currentUser= Firebase.auth.currentUser
        viewModelScope.launch {
            FirebaseService.getBookings().collect{ listBookings ->
                bookingList.value = listBookings
                interestedList.value = bookingList.value?.filter { it.interestedUsers.contains(currentUser!!.uid) && !it.bookedUsers.contains(currentUser!!.uid)}
                bookedList.value = bookingList.value?.filter { it.bookedUsers.contains(currentUser!!.uid) }
            }
        }
    }

    fun getBookingList(): LiveData<List<Booking>> {
        return bookingList
    }

    fun getMyInterestedList(): LiveData<List<Booking>> {
        return interestedList
    }

    fun getMyBookedList(): LiveData<List<Booking>> {
        return bookedList
    }

    suspend fun getBookedListByUserId(userId: String): List<Booking>?{
        return withContext(Dispatchers.IO) { FirebaseService.getBookedListByUserId(userId) }
    }

    suspend fun getBooking(id: String): Booking?{
        return withContext(Dispatchers.IO) { FirebaseService.getBooking(id) }
    }

    suspend fun setInterestedUser(bookingId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) { FirebaseService.setInterestedUser(bookingId, userId) }
    }
}