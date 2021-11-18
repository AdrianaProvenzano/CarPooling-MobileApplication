package it.polito.mad.backToNokia.carpooling.ui.booking

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.BookingsViewModel
import it.polito.mad.backToNokia.carpooling.data.TripsViewModel
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.TripAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class TripsOfInterestListFragment : Fragment(R.layout.fragment_trips_of_interest_list) {
    private val tripsViewModel: TripsViewModel by activityViewModels()
    private val bookingsViewModel: BookingsViewModel by activityViewModels()

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)

        bookingsViewModel.getMyInterestedList().observe(viewLifecycleOwner,{ bookingsList ->
            val interestedBookings = bookingsList.toMutableList()
            tripsViewModel.getListTrip().observe(viewLifecycleOwner, { tripList ->
                //Display trips
                val emptyList = itemView.findViewById<TextView>(R.id.empty_booking_list)
                val rv = itemView.findViewById<RecyclerView>(R.id.recycler_view_booking)
                rv.layoutManager = LinearLayoutManager(this.context)

                val trips = tripList.toMutableList()

                var interestedTrips = mutableListOf<Trip>()

                if (!interestedBookings.isNullOrEmpty()) {
                    val bookingsId = interestedBookings.map {it.id}
                    interestedTrips = trips.filter {
                        bookingsId.contains(it.id)
                    }.toMutableList()
                }
                val itemAdapter = TripAdapter(interestedTrips, this, true)
                rv.adapter = itemAdapter

                if (interestedTrips.isNotEmpty()) {
                    emptyList.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                }

            })
        })
    }
}