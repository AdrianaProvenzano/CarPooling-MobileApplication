package it.polito.mad.backToNokia.carpooling.ui.trip

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.BookingsViewModel
import it.polito.mad.backToNokia.carpooling.data.UserViewModel
import it.polito.mad.backToNokia.carpooling.data.TripsViewModel
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.UserInterestedAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class TripUserInterestedFragment: Fragment(R.layout.fragment_trip_user_interested) {

    private val userViewModel: UserViewModel by activityViewModels()
    private val tripsViewModel: TripsViewModel by activityViewModels()
    private val bookingsViewModel: BookingsViewModel by activityViewModels()
    private val frag = this

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)
        val tripId = arguments?.getString("tripId")
        val rating = arguments?.getBoolean("rating")

        if(rating!=true){
            MainScope().launch {
                val trip = tripsViewModel.getTrip(tripId!!).value
                val booking = bookingsViewModel.getBooking(tripId)
                val interestedUsers = userViewModel.getInterestedUsers(tripId)
                val rv = itemView.findViewById<RecyclerView>(R.id.recycler_view)
                rv.layoutManager = LinearLayoutManager(frag.context)
                if (booking?.interestedUsers?.isNotEmpty() == true) {
                    rv.visibility = VISIBLE
                    val itemAdapter = UserInterestedAdapter(booking, interestedUsers!!, trip!!, frag, false)
                    rv.adapter = itemAdapter
                }
            }
        }
        else{
            MainScope().launch {
                val trip = tripsViewModel.getTrip(tripId!!).value //??
                val booking = bookingsViewModel.getBooking(tripId)
                val bookedUsers = userViewModel.getBookedUsers(tripId)
                val rv = itemView.findViewById<RecyclerView>(R.id.recycler_view)
                rv.layoutManager = LinearLayoutManager(frag.context)
                if (booking?.bookedUsers?.isNotEmpty()==true) {
                    rv.visibility = VISIBLE
                    val itemAdapter = UserInterestedAdapter(booking, bookedUsers!!, trip!!, frag, true)
                    rv.adapter = itemAdapter
                }
            }
        }

    }
}