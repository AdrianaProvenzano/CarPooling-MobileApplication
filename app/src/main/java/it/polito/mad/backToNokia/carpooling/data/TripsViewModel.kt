package it.polito.mad.backToNokia.carpooling.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.backToNokia.carpooling.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.Comparator

@ExperimentalCoroutinesApi
class TripsViewModel: ViewModel() {
    private val currentUser: FirebaseUser = Firebase.auth.currentUser!!
    private val comparatorDate = ComparatorDate()

    private val tripList: MutableLiveData<MutableList<Trip>> by lazy {
        MutableLiveData<MutableList<Trip>>()
    }

    private val myTripList: MutableLiveData<List<Trip>> by lazy {
        MutableLiveData<List<Trip>>()
    }

    private val otherTripList: MutableLiveData<List<Trip>> by lazy {
        MutableLiveData<List<Trip>>()
    }
    
    init {
        viewModelScope.launch {
            FirebaseService.getTrips().collect{ listTrip ->
                tripList.value = listTrip.sortedWith(comparatorDate).toMutableList()
                myTripList.value = tripList.value?.filter { it.user_id==currentUser.uid }
                otherTripList.value = tripList.value?.filter {
                    val convertedDate = LocalDate.parse(it.departure_date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    val today = LocalDate.now()
                    convertedDate.isAfter(today) && it.available_seats!! >0 && it.user_id!=currentUser.uid
                }
            }
        }
    }

    fun getListTrip(): LiveData<MutableList<Trip>> {
        return tripList
    }

    fun getMyTrips(): LiveData<List<Trip>>{
        return myTripList
    }

    fun getOthersTrips(): LiveData<List<Trip>>{
        return otherTripList
    }

    suspend fun getTripsByUserId(userId: String): List<Trip>?{
        return withContext(Dispatchers.IO) { FirebaseService.getTripsByUserId(userId) }
    }

    fun getTrip(id: String): LiveData<Trip> {
        return MutableLiveData(tripList.value?.find { it.id == id })
    }

    suspend fun addTrip(trip: Trip): String{
        var newId: String
        withContext(Dispatchers.IO) {
            newId = FirebaseService.addTrip(trip)
        }
        return newId
    }

    suspend fun updateTrip(trip: Trip){
        val old = tripList.value?.find { it.id == trip.id }
        val index = tripList.value?.indexOf(old)
        tripList.value?.set(index!!, trip)
        withContext(Dispatchers.IO){
            FirebaseService.updateTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip){
        tripList.value?.removeIf { it.id == trip.id }
        FirebaseService.deleteTrip(trip)
    }

    class ComparatorDate: Comparator<Trip>{
        override fun compare(o1: Trip?, o2: Trip?): Int {
            if (!o1!!.departure_date.isEmpty() && !o2!!.departure_date.isEmpty()){
                val d1 = LocalDate.parse(o1.departure_date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val d2 = LocalDate.parse(o2.departure_date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                if (d1.isAfter(d2))
                    return -1
                else return +1
            }
            return +1
        }
    }

}