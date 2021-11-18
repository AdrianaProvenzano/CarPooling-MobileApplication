package it.polito.mad.backToNokia.carpooling.ui.trip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.model.User
import it.polito.mad.backToNokia.carpooling.data.TripsViewModel
import it.polito.mad.backToNokia.carpooling.model.Booking
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.TripAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi

import java.util.*

@ExperimentalCoroutinesApi
class OthersTripListFragment : Fragment(R.layout.fragment_other_trip_list) {
    private lateinit var trips:MutableList<Trip>
    private lateinit var users:MutableList<User>
    private lateinit var myBookings:MutableList<Booking>
    private val tripsViewModel: TripsViewModel by activityViewModels()
    private lateinit var rv:RecyclerView
    private lateinit var emptyList:TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trips= mutableListOf()
        users = mutableListOf()
        myBookings = mutableListOf()
        emptyList = view.findViewById(R.id.empty_list_other)
        rv = view.findViewById(R.id.recycler_view_other)

        tripsViewModel.getOthersTrips().observe(viewLifecycleOwner, { tripList ->
            trips = tripList.toMutableList()
            rv.layoutManager = LinearLayoutManager(this.context)
            rv.adapter = TripAdapter(trips, this, true)

            if(trips.isNotEmpty()) {
                emptyList.visibility = View.GONE
                rv.visibility = VISIBLE
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.search_icon -> {
                openDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openDialog() {
        val builder = this.context?.let { MaterialAlertDialogBuilder(it) }
        val view:View = LayoutInflater.from(this.context)
            .inflate(R.layout.filter_layout, null, false)

        val b = builder?.setView(view)
        val date = view.findViewById<EditText>(R.id.filter_departure_date)
        date.onFocusChangeListener = View.OnFocusChangeListener{ _, hasFocus ->
            if (hasFocus){
                showDataPicker(view)
            }
        }
        val time = view.findViewById<EditText>(R.id.filter_departure_time)
        time.onFocusChangeListener = View.OnFocusChangeListener{ _, hasFocus ->
            if (hasFocus){
                showTimePicker(view)
            }
        }

        b?.setPositiveButton(getString(R.string.apply)) { dialog, _ ->
            val filterDepLoc = view.findViewById<EditText>(R.id.filter_departure_location).text
            val filterArrLoc = view.findViewById<EditText>(R.id.filter_arrival_location).text
            val filterDepDate = date.text
            val filterDepTime = time.text
            val filterSeats = view.findViewById<Slider>(R.id.slider_seats).value
            val filterPrice = view.findViewById<RangeSlider>(R.id.slider_price).values
            var newTrips: MutableList<Trip>

            tripsViewModel.getOthersTrips().observe(viewLifecycleOwner, { tripList ->
               newTrips= tripList.filter {
                   var ok=true
                   if (it.departure_location!=filterDepLoc.toString() && filterDepLoc.toString()!="")
                       ok = false
                   if (it.arrival_location!=filterArrLoc.toString() && filterArrLoc.toString()!="")
                       ok = false
                   if (it.departure_date!=filterDepDate.toString() && filterDepDate.toString()!="")
                       ok = false
                   if (it.departure_time<filterDepTime.toString() && filterDepTime.toString()!="")
                       ok = false
                   if (it.available_seats!! <filterSeats.toInt() && filterSeats!=0.0f)
                       ok = false
                   if( it.price!!<filterPrice[0] ||  it.price!! >filterPrice[1])
                       ok = false
                   ok
               } as MutableList<Trip>
                rv.adapter = TripAdapter(newTrips, this, true)
                dialog.dismiss()
            })
        }
            ?.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            ?.show()
    }

    private fun showDataPicker(view: View) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val date = view.findViewById<EditText>(R.id.filter_departure_date)
        date.showSoftInputOnFocus = false
        val datepicker = DatePickerDialog(requireView().context, { _, yearP, monthP, dayOfMonth ->
            date.setText("%02d".format(dayOfMonth) + "/%02d".format(monthP.plus(1)) + "/%02d".format(yearP))
        }, year, month, day)
        datepicker.show()
    }

    private fun showTimePicker(view:View) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val time = view.findViewById<EditText>(R.id.filter_departure_time)
        time.showSoftInputOnFocus=false
        val timepicker = TimePickerDialog(requireView().context, { _, hourOfDay, minuteP ->
            time.setText("%02d".format(hourOfDay)+":%02d".format(minuteP))
        }, hour, minute, true)
        timepicker.show()
    }

}