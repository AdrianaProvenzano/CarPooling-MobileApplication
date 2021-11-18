package it.polito.mad.backToNokia.carpooling.ui.trip

import android.os.Bundle
import android.view.View
import android.view.View.*
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.TripsViewModel
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.TripAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class TripListFragment : Fragment(R.layout.fragment_trip_list) {
    private val tripsViewModel: TripsViewModel by activityViewModels()

    override fun onViewCreated(itemView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(itemView, savedInstanceState)

        tripsViewModel.getMyTrips().observe(viewLifecycleOwner, { tripList ->
            //Display trips
            val emptyList = itemView.findViewById<TextView>(R.id.empty_list_textView)
            val rv = itemView.findViewById<RecyclerView>(R.id.recycler_view)
            rv.layoutManager = LinearLayoutManager(this.context)
            val itemAdapter = TripAdapter(tripList.toMutableList(), this, false)
            rv.adapter = itemAdapter

            if(tripList.isNotEmpty()) {
                emptyList.visibility = GONE
                rv.visibility = VISIBLE
            }

        })

        //Set the 'add new trip' button
        val fab: FloatingActionButton = itemView.findViewById(R.id.fab_new_trip)
        fab.setOnClickListener {
            val bundle = bundleOf("tripIndex" to "")
            findNavController().navigate(R.id.action_nav_trip_list_to_nav_trip_edit, bundle)
        }

    }

}
