    package it.polito.mad.backToNokia.carpooling.ui.trip

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.*
import it.polito.mad.backToNokia.carpooling.model.Rating
import it.polito.mad.backToNokia.carpooling.model.Stop
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@ExperimentalCoroutinesApi
class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val userAuth = Firebase.auth.currentUser
    private lateinit var imageView: ImageView
    private lateinit var departureDateView: TextView
    private lateinit var departureTimeView: TextView
    private lateinit var departureLocView: TextView
    private lateinit var arrivalLocView: TextView
    private lateinit var hoursView: TextView
    private lateinit var seatsView: TextView
    private lateinit var priceView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var stopsView: LinearLayout
    private lateinit var interestedUsersButton: Button

    private val tripsViewModel: TripsViewModel by activityViewModels()
    private val bookingsViewModel: BookingsViewModel by activityViewModels()
    private val ratingsViewModel: RatingsViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private lateinit var user: User
    private lateinit var tripIndex : String

    private lateinit var map : MapView
    private var startMarker: Marker? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        user = userViewModel.getCurrentUser().value!!
        super.onViewCreated(view, savedInstanceState)
        imageView = view.findViewById(R.id.image_car_det_view)
        departureLocView = view.findViewById<EditText>(R.id.departure_location_view)
        arrivalLocView = view.findViewById<EditText>(R.id.arrival_location_view)
        departureDateView = view.findViewById<EditText>(R.id.departure_date_view)
        departureTimeView = view.findViewById<EditText>(R.id.departure_time_view)
        hoursView = view.findViewById<EditText>(R.id.duration_view)
        seatsView = view.findViewById<EditText>(R.id.seats_view)
        priceView = view.findViewById<EditText>(R.id.price_view)
        descriptionView = view.findViewById<EditText>(R.id.description_view)
        stopsView = view.findViewById<LinearLayout>(R.id.stops)
        map = view.findViewById(R.id.trip_detail_map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        tripIndex = arguments?.getString("tripIndex")!!

        //If bundle does have other=false -> show MY trip details
        if(arguments?.getBoolean("other")==false) {
            //Edit button enabled
            setHasOptionsMenu(true)
            interestedUsersButton = view.findViewById(R.id.interested_users_button)
            interestedUsersButton.visibility = VISIBLE
            interestedUsersButton.setOnClickListener {
                findNavController().navigate(R.id.action_nav_trip_details_to_nav_trip_users_interested, bundleOf("tripId" to tripIndex))
            }
        //If bundle does have other=true -> show OTHER trip details
        }else {
            //Edit button disabled
            setHasOptionsMenu(false)
            if(arguments?.getBoolean("booked")!=true){
                val fab: View = view.findViewById(R.id.fab_interested)
                fab.visibility = VISIBLE
                fab.setOnClickListener { viewF ->
                    MainScope().launch {
                        val res = bookingsViewModel.setInterestedUser(tripIndex, userAuth!!.uid)
                        if (res) {
                            Snackbar.make(viewF, getString(R.string.interested), Snackbar.LENGTH_LONG)
                                .setTextColor(Color.CYAN)
                                .show()
                        }
                        if (!res) {
                            Snackbar.make(viewF, getString(R.string.already_interested), Snackbar.LENGTH_LONG)
                                .setTextColor(Color.CYAN)
                                .show()
                        }
                    }
                }
            }
        }

        tripsViewModel.getTrip(tripIndex).observe(viewLifecycleOwner, { trip ->
            if (trip!=null){
                if (!trip.car_image.isNullOrEmpty()) {
                    val ref: StorageReference = storageRef.child("trips_images/${trip.car_image}")
                    Glide.with(this).load(ref).into(imageView)
                }
                else
                    imageView.visibility=GONE;

                //handle map
                val mapController = map.controller
                mapController.setZoom(7.0)
                //val startPoint = GeoPoint(41.87194, 12.56738)
                //val endPoint = GeoPoint(41.86150, 12.56550)
                val startPoint = GeoPoint(trip.departure_coordinates!!.latitude, trip.departure_coordinates!!.longitude)
                val endPoint = GeoPoint(trip.arrival_coordinates!!.latitude, trip.arrival_coordinates!!.longitude)

                val startMarker = Marker(map)
                val endMarker = Marker(map)
                startMarker.position = startPoint
                endMarker.position = endPoint
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                startMarker.icon = context?.let { ContextCompat.getDrawable(it, R.drawable.map_start_marker) }
                endMarker.icon = context?.let { ContextCompat.getDrawable(it, R.drawable.map_end_marker) }
                map.overlays.add(startMarker)
                map.overlays.add(endMarker)

                map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                //TODO: maybe: disable the navigation
                map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
/*                        val bundle = Bundle()
                        bundle.putDouble("departureCoordinatesLat", trip.departure_coordinates!!.latitude)
                        bundle.putDouble("departureCoordinatesLong", trip.departure_coordinates!!.longitude)
                        bundle.putDouble("arrivalCoordinatesLat", trip.arrival_coordinates!!.latitude)
                        bundle.putDouble("arrivalCoordinatesLong", trip.arrival_coordinates!!.longitude)*/
                        val directions = TripDetailsFragmentDirections.actionNavTripDetailsToNavShowMap(trip)
                        findNavController().navigate(directions)
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean {
/*                        val bundle = Bundle()
                        bundle.putDouble("departureCoordinatesLat", trip.departure_coordinates!!.latitude)
                        bundle.putDouble("departureCoordinatesLong", trip.departure_coordinates!!.longitude)
                        bundle.putDouble("arrivalCoordinatesLat", trip.arrival_coordinates!!.latitude)
                        bundle.putDouble("arrivalCoordinatesLong", trip.arrival_coordinates!!.longitude)*/
                        val directions = TripDetailsFragmentDirections.actionNavTripDetailsToNavShowMap(trip)
                        findNavController().navigate(directions)
                        return false
                    }
                }))

                map.addOnFirstLayoutListener { v, left, top, right, bottom ->
                    val b: BoundingBox = BoundingBox.fromGeoPointsSafe(listOf(startPoint, endPoint))
                    map.zoomToBoundingBox(b, false,120)
                    map.invalidate()
                }

                departureLocView.text = getString(R.string.from) + " " + trip.departure_location
                arrivalLocView.text = getString(R.string.to) + " " + trip.arrival_location
                departureDateView.text = trip.departure_date
                departureTimeView.text = trip.departure_time
                hoursView.text = trip.hours_duration.toString() + " " + getString(R.string.hours_and) + " " + trip.minutes_duration.toString() + " " + getString(R.string.minutes)
                seatsView.text = trip.available_seats.toString() + " " + getString(R.string.min_available_seats)
                priceView.text = "%.2f".format(trip.price) + " €"
                descriptionView.text = trip.description
                if (!trip.stopList.isNullOrEmpty()) {
                    val label = view.findViewById<TextView>(R.id.label_stops)
                    stopsView.visibility = VISIBLE
                    label.visibility = VISIBLE
                    for(s in trip.stopList!!){
                        addStopView(s)
                    }

                }
                val convertedDate = LocalDate.parse(trip.departure_date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val today = LocalDate.now()
                val rateButton = view.findViewById<MaterialButton>(R.id.button_rate)
                if(arguments?.getBoolean("booked")==true && convertedDate.isBefore(today)) {
                    rateButton.visibility= VISIBLE
                    rateButton.setOnClickListener {
                        MainScope().launch {
                            if(ratingsViewModel.driverRatingExist(trip.id!!))
                                Snackbar.make(view, getString(R.string.already_rated), Snackbar.LENGTH_LONG).setTextColor(Color.CYAN).show()
                            else
                                openRateDialog(trip, true)
                        }
                    }

                } else if (arguments?.getBoolean("other")==false && convertedDate.isBefore(today)){
                    setHasOptionsMenu(false)
                    interestedUsersButton.visibility = GONE
                    MainScope().launch {
                        val booking = bookingsViewModel.getBooking(trip.id!!)
                        val userBooked = booking?.bookedUsers
                        if(userBooked?.size==0)
                            rateButton.visibility=GONE
                        else{
                            rateButton.visibility=VISIBLE
                            rateButton.text = getString(R.string.rate_passengers)
                            rateButton.setOnClickListener {
                                MainScope().launch {
                                    if(ratingsViewModel.allPassengerRatingsExist(trip.id!!, booking!!.bookedUsers.size)){
                                        Snackbar.make(view, getString(R.string.already_rated_passengers), Snackbar.LENGTH_LONG).setTextColor(Color.CYAN).show()
                                    }
                                    else{
                                        val bundle = bundleOf("rating" to true)
                                        bundle.putString("tripId",tripIndex)
                                        findNavController().navigate(R.id.action_nav_trip_details_to_nav_trip_users_interested, bundle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun addStopView(s: Stop) {
        val stopView: View = layoutInflater.inflate(R.layout.row_stop, null, false)
        val locationStop:TextInputLayout = stopView.findViewById(R.id.stop_loc_view)
        val timeStop:TextInputLayout = stopView.findViewById(R.id.stop_time_view)
        locationStop.editText?.setText(s.location)
        timeStop.editText?.setText(s.time)
        stopsView.addView(stopView)
    }

    private suspend fun openRateDialog(trip: Trip, driver:Boolean) {
        val builder = this.context?.let { MaterialAlertDialogBuilder(it) }
        val view:View = LayoutInflater.from(this.context)
            .inflate(R.layout.rating_layout, null, false)

        val b = builder?.setView(view)
        val userimage = view.findViewById<ImageView>(R.id.image_user_to_rate)
        val username = view.findViewById<TextView>(R.id.username_to_rate)
        val tempUser = userViewModel.getUserById(trip.user_id!!)
        if (tempUser != null){
            username.text = getString(R.string.rate)+" "+tempUser.nickname
            if (!tempUser.imageRef.isNullOrEmpty()) {
                val ref: StorageReference = storageRef.child("profile_images/${tempUser.imageRef}")
                Glide.with(this).load(ref).circleCrop().into(userimage)
            }
        }

        b?.setPositiveButton(getString(R.string.rate)) { dialog, _ ->
            val score = view.findViewById<RatingBar>(R.id.rating_bar).rating
            val comment = view.findViewById<EditText>(R.id.comment).text.toString()
            val rating = Rating(tempUser?.id, user.id, trip.id, score, comment, driver)
            MainScope().launch(){
                ratingsViewModel.addRating(rating)
            }
            dialog.dismiss()
        }
            ?.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            ?.show()
    }

    // save trip menu implementation
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    // by tapping on the saving icon we go back to tripListFragment
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.edit_button -> {
                val bundle = bundleOf("tripIndex" to tripIndex)
                findNavController().navigate(R.id.action_nav_trip_details_to_nav_trip_edit, bundle)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}