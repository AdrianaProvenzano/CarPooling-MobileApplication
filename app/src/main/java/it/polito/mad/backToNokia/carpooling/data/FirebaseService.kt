package it.polito.mad.backToNokia.carpooling.data

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.model.Booking
import it.polito.mad.backToNokia.carpooling.model.Rating
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.FileInputStream

@ExperimentalCoroutinesApi
object FirebaseService {
    private const val TAG = "FirebaseProfileService"

    /*** USERS METHODS ***/

    suspend fun getCurrentUserProfile(): User? {
        val db = FirebaseFirestore.getInstance()
        val userAuth: FirebaseUser = Firebase.auth.currentUser!!
        return try {
            db.collection("users")
                .document(userAuth.uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user details", e)
            null
        }
    }

    suspend fun getUser(id: String): User? {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("users")
                .document(id).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user details", e)
            null
        }
    }

    suspend fun getInterestedUser(bookingId: String): List<User>?{
        val db = FirebaseFirestore.getInstance()
        return try {
            val booking = db.collection("bookings")
                .document(bookingId).get().await().toObject(Booking::class.java)
            if (booking!!.interestedUsers.isNotEmpty()){
                val result = db.collection("users")
                    .whereIn("id", booking.interestedUsers).get().await().toObjects(User::class.java)
                result
            } else {
                listOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting interested users", e)
            null
        }
    }

    suspend fun getBookedUsers(bookingId: String): List<User>?{
        val db = FirebaseFirestore.getInstance()
        return try {
            val booking = db.collection("bookings")
                .document(bookingId).get().await().toObject(Booking::class.java)
            if (booking!!.bookedUsers.isNotEmpty()){
                val result = db.collection("users")
                    .whereIn("id", booking.bookedUsers).get().await().toObjects(User::class.java)
                result
            } else {
                listOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting interested users", e)
            null
        }
    }

    suspend fun updateCurrentUser(user: User){
        val db = FirebaseFirestore.getInstance()
        try{
            db.collection("users").document(user.id!!)
                .set(user, SetOptions.mergeFields(listOf("email", "id", "imageRef", "location", "name", "nickname"))).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading user details", e)
        }
    }

    /*** TRIPS METHODS ***/


    fun getTrips(): Flow<List<Trip>> {
        val db = FirebaseFirestore.getInstance()
        return callbackFlow {
            val listenerRegistration = db.collection("trips")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshots!=null){
                        val map = snapshots.documents.mapNotNull { it.toObject(Trip::class.java) }
                        offer(map)
                    }

                }
            awaitClose {
                Log.d(TAG, "Cancelling posts listener")
                listenerRegistration.remove()
            }
        }
    }

    suspend fun addTrip(trip: Trip): String{
        val db = FirebaseFirestore.getInstance()
        return try{
            val ref = db.collection("trips").document()
            trip.id = ref.id
            ref.set(trip).await()
            val booking = Booking(id = ref.id)
            db.collection("bookings").document(ref.id).set(booking).await()
            ref.id
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading trip details", e)
            ""
        }
    }

    suspend fun updateTrip(trip: Trip){
        val db = FirebaseFirestore.getInstance()
        try{
            db.collection("trips").document(trip.id!!)
                .set(trip, SetOptions.mergeFields(listOf("car_image", "departure_location", "arrival_location", "departure_date","departure_coordinates", "arrival_coordinates",
                                                        "departure_time", "description", "hours_duration", "minutes_duration", "available_seats", "price", "stopList")))
                .await()
        } catch(e: Exception){
            Log.e(TAG, "Error uploading trip details", e)
        }
    }

    fun deleteTrip(trip: Trip){
        val db = FirebaseFirestore.getInstance()
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference
        try {
            db.collection("trips").document(trip.id!!).delete()
            db.collection("bookings").document(trip.id!!).delete()
            if (!trip.car_image.isNullOrEmpty()){
                val imageRef = storageRef.child("trips_images/${trip.car_image}")
                MainScope().launch { deleteImage(imageRef) }
            }

        } catch (e: java.lang.Exception){
            Log.e(TAG, "Error deleting a trip", e)
        }
    }

    suspend fun getTripsByUserId(userId: String): List<Trip>? {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("trips")
                .whereEqualTo("user_id", userId).get().await().toObjects(Trip::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booking details", e)
            null
        }
    }

    /*** BOOKINGS METHODS ***/

    fun getBookings(): Flow<List<Booking>> {
        val db = FirebaseFirestore.getInstance()
        return callbackFlow {
            val listenerRegistration = db.collection("bookings")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshots!=null){
                        val map = snapshots.documents.mapNotNull { it.toObject(Booking::class.java) }
                        offer(map)
                    }

                }
            awaitClose {
                Log.d(TAG, "Cancelling posts listener")
                listenerRegistration.remove()
            }
        }
    }

    suspend fun getBooking(id: String): Booking? {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("bookings")
                .document(id).get().await().toObject(Booking::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booking details", e)
            null
        }
    }

    suspend fun setInterestedUser(bookingId: String, userId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            val booking = db.collection("bookings")
                .document(bookingId).get().await().toObject(Booking::class.java)
            if (!booking?.interestedUsers!!.contains(userId)){
                booking.interestedUsers.add(userId)
                db.collection("bookings").document(bookingId).set(booking, SetOptions.mergeFields("interestedUsers")).await()
                true
            } else
                false
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user details", e)
            false
        }
    }

    suspend fun setBookedUser(bookingId: String, userId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            val booking = db.collection("bookings")
                .document(bookingId).get().await().toObject(Booking::class.java)
            if (!booking?.bookedUsers!!.contains(userId)){
                booking.bookedUsers.add(userId)
                db.collection("bookings").document(bookingId).set(booking, SetOptions.mergeFields("bookedUsers")).await()
                val trip = db.collection("trips").document(bookingId).get().await().toObject(Trip::class.java)
                trip!!.available_seats = trip.available_seats!!.minus(1)
                db.collection("trips").document(bookingId).set(trip, SetOptions.mergeFields("available_seats")).await()
                true
            } else
                false
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user details", e)
            false
        }
    }

    suspend fun getBookedListByUserId(userId: String): List<Booking>? {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("bookings")
                .whereArrayContains("bookedUsers", userId).get().await().toObjects(Booking::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booking details", e)
            null
        }
    }

    /*** RATING METHODS ***/

/*    suspend fun getRatings(): Flow<List<Rating>> {
        val db = FirebaseFirestore.getInstance()
        return callbackFlow {
            val listenerRegistration = db.collection("ratings")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshots!=null){
                        val map = snapshots.documents.mapNotNull { it.toObject(Rating::class.java) }
                        offer(map)
                    }

                }
            awaitClose {
                Log.d(TAG, "Cancelling posts listener")
                listenerRegistration.remove()
            }
        }
    }*/

    suspend fun addRating(rating: Rating): String?{
        val db = FirebaseFirestore.getInstance()
        return try{
            db.collection("ratings/trips/${rating.booking}").document().set(rating).await()
            rating.booking
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading trip details", e)
            ""
        }
    }

/*    suspend fun getRating(rated_user: String, rating_user: String?, booking: String?): Rating? {
        val db = FirebaseFirestore.getInstance()
        return try {
            val list = db.collection("ratings")
                .whereEqualTo("booking", booking)
                .whereEqualTo("rated_user", rated_user)
                .whereEqualTo("rating_user",  rating_user)
                .get().await().toMutableList()
            return list[0].toObject(Rating::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booking details", e)
            null
        }
    }*/

    suspend fun getDriverRatings(userId: String, tripList: List<String>): List<Rating>? {
        val db = FirebaseFirestore.getInstance()
        val listRatings = mutableListOf<Rating>()
        tripList.forEach {
            try{
                val list = db.collection("ratings/trips/$it")
                    .whereEqualTo("rated_user", userId)
                    .whereEqualTo("driver",  true)
                    .get().await().toObjects(Rating::class.java)
                listRatings.addAll(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ratings details", e)
                return null
            }
        }
        return listRatings
    }

    suspend fun getPassengerRatings(userId: String, bookedList: List<String>): List<Rating>? {
        val db = FirebaseFirestore.getInstance()
        val listRatings = mutableListOf<Rating>()
        bookedList.forEach {
            try{
                val list = db.collection("ratings/trips/$it")
                    .whereEqualTo("rated_user", userId)
                    .whereEqualTo("driver",  false)
                    .get().await().toObjects(Rating::class.java)
                listRatings.addAll(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ratings details", e)
                return null
            }
        }
        return listRatings
    }

    suspend fun getRatingsByUserId(userId: String, bookedList: List<String>, myTrips: List<String>): List<Rating>? {
        val db = FirebaseFirestore.getInstance()
        val listRatings = mutableListOf<Rating>()
        bookedList.forEach {
            try {
                val list = db.collection("ratings/trips/$it")
                    .whereEqualTo("rated_user", userId)
                    .whereEqualTo("driver", false)
                    .get().await().toObjects(Rating::class.java)
                listRatings.addAll(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ratings details", e)
                return null
            }
        }
        myTrips.forEach {
            try{
                val list = db.collection("ratings/trips/$it")
                    .whereEqualTo("rated_user", userId)
                    .whereEqualTo("driver",  true)
                    .get().await().toObjects(Rating::class.java)
                listRatings.addAll(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ratings details", e)
                return null
            }
        }
        return listRatings
    }

    suspend fun driverRatingExist(id: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        val userAuth: FirebaseUser = Firebase.auth.currentUser!!
        return try {
            val rating = db.collection("ratings/trips/${id}")
                .whereEqualTo("rating_user", userAuth.uid)
                .whereEqualTo("driver", true)
                .get().await().toObjects(Rating::class.java)
            !rating.isEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting rating details", e)
            false
        }
    }

    suspend fun allPassengerRatingsExist(id: String, passengers: Int): Boolean {
        val db = FirebaseFirestore.getInstance()
        val userAuth: FirebaseUser = Firebase.auth.currentUser!!
        return try {
            val ratings = db.collection("ratings/trips/${id}")
                .whereEqualTo("rating_user", userAuth.uid)
                .whereEqualTo("driver", false)
                .get().await()
            ratings.size()==passengers
        } catch (e: Exception) {
            Log.e(TAG, "Error getting rating details", e)
            false
        }
    }

    suspend fun passengerRatingExist(tripId: String, userId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        val userAuth: FirebaseUser = Firebase.auth.currentUser!!
        return try {
            val rating = db.collection("ratings/trips/${tripId}")
                .whereEqualTo("rating_user", userAuth.uid)
                .whereEqualTo("driver", false)
                .whereEqualTo("rated_user", userId)
                .get().await()
            !rating.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error getting rating details", e)
            false
        }
    }

    /*** STORAGE METHODS ***/

    suspend fun deleteImage(ref: StorageReference){
        ref.delete().await()
    }

    suspend fun uploadImage(ref: StorageReference, file: FileInputStream){
        ref.putStream(file).await()
    }

}