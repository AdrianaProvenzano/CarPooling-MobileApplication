package it.polito.mad.backToNokia.carpooling.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
class UserViewModel : ViewModel() {
    private val currentUser: MutableLiveData<User> by lazy {
        MutableLiveData<User>()
    }

    init {
        viewModelScope.launch {
            currentUser.value = FirebaseService.getCurrentUserProfile()
        }
    }

    fun getCurrentUser(): LiveData<User> {
        return currentUser
    }

    suspend fun updateCurrentUser(newUser: User){
        currentUser.value = newUser
        withContext(Dispatchers.IO){
            FirebaseService.updateCurrentUser(newUser)
        }
    }

    suspend fun getInterestedUsers(bookingId: String): List<User>?{
        return withContext(Dispatchers.IO) { FirebaseService.getInterestedUser(bookingId) }
    }

    suspend fun getBookedUsers(bookingId: String): List<User>?{
        return withContext(Dispatchers.IO) { FirebaseService.getBookedUsers(bookingId) }
    }

    suspend fun getUserById(userId: String): User?{
        return withContext(Dispatchers.IO) { FirebaseService.getUser(userId) }
    }

}