package it.polito.mad.backToNokia.carpooling.ui.trip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.FirebaseService
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.ui.booking.BoughtTripsListFragment
import it.polito.mad.backToNokia.carpooling.ui.booking.TripsOfInterestListFragment
import it.polito.mad.backToNokia.carpooling.ui.trip.OthersTripListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// the adapter owns the data (in fact we pass them to it as parameter) and tells to the RecyclerView how many items need to be displayed;
// it also fills the view each time it is requested
@ExperimentalCoroutinesApi
class TripAdapter(private val data: MutableList<Trip>, private val fragment: Fragment, private val other:Boolean): RecyclerView.Adapter<TripAdapter.TripViewHolder>(){

    // the viewHolder encapsulates the sub-part of visual hierarchy
    class TripViewHolder(v: View, private val frag: Fragment, private val other:Boolean): RecyclerView.ViewHolder(v) {
        private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
        private val currentUser = Firebase.auth.currentUser!!
        private val imageView: ImageView = v.findViewById(R.id.image_car_view)
        private val locationView: TextView = v.findViewById(R.id.departure_arrival_location_view)
        private val timeView: TextView = v.findViewById(R.id.departure_date_time_view)
        private val seatsView: TextView = v.findViewById(R.id.available_seats_view)
        private val priceView: TextView = v.findViewById(R.id.price_view)
        private val card: CardView = v.findViewById(R.id.card_adv)
        private val userimage = card.findViewById<ImageView>(R.id.other_user)
        private val publisher = card.findViewById<TextView>(R.id.other_user_nick)
        private val rateNow = card.findViewById<TextView>(R.id.rate_now)
        private val bookedIcon = card.findViewById<ImageView>(R.id.booked_icon)
        private val interestedIcon = card.findViewById<ImageView>(R.id.interested_icon)

        fun bind(trip: Trip) {

            if (!trip.car_image.isNullOrEmpty()) {
                val ref: StorageReference = storageRef.child("trips_images/${trip.car_image}")
                Glide.with(frag).load(ref).into(imageView)
            }

            locationView.text = frag.getString(R.string.from) + " " + trip.departure_location + " " + frag.getString(R.string.to) + " " + trip.arrival_location
            timeView.text = frag.getString(R.string.departure_on) + " " + trip.departure_date + " " + frag.getString(R.string.at) + " " + trip.departure_time
            seatsView.text = frag.getString(R.string.number_of_available_seats) + ": " + trip.available_seats
            priceView.text = frag.getString(R.string.price) + ": %.2f".format(trip.price) + " €"


            //"Rate now"
            val convertedDate = LocalDate.parse(trip.departure_date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val today = LocalDate.now()
            if(frag is BoughtTripsListFragment && convertedDate.isBefore(today)){
                MainScope().launch {
                    if(FirebaseService.driverRatingExist(trip.id!!))
                        rateNow.setText(frag.context?.getString(R.string.rated))
                    else
                        rateNow.setText(frag.context?.getString(R.string.rate_now))

                }
            }

            if(!other){
                val button: Button? = this.card.findViewById(R.id.edit_button)
                if (convertedDate.isAfter(today) || convertedDate.isEqual(today)){
                    // edit a trip
                    button?.setOnClickListener {
                        val bundle2 = bundleOf("tripIndex" to trip.id)
                        frag.findNavController().navigate(R.id.action_nav_trip_list_to_nav_trip_edit, bundle2)
                    }
                } else {
                    button!!.visibility = GONE
                    priceView.setPadding(0,0,0,20)
                }

            }else{
                val userimage = card.findViewById<ImageView>(R.id.other_user)
                val publisher = card.findViewById<TextView>(R.id.other_user_nick)

                MainScope().launch {
                    val user =  FirebaseService.getUser(trip.user_id!!)
                    if(user!!.nickname!=null){
                        val username = user.nickname.toString()
                        publisher.text = username
                    }
                    else {
                        val email = user.email.toString()
                        publisher.text = email
                    }
                    if(user.imageRef!=null)
                        Glide.with(frag).load(storageRef.child("profile_images/${user.imageRef}")).circleCrop().into(userimage)
                    else
                        Glide.with(frag).load(R.drawable.user_icon).circleCrop().into(userimage)

                    userimage.setOnClickListener{
                        val bundle = bundleOf("user" to user)
                        frag.findNavController().navigate(R.id.action_nav_trip_list_other_to_nav_show_profile, bundle)
                    }

                    val booking = FirebaseService.getBooking(trip.id!!)
                    val currentUser = FirebaseService.getCurrentUserProfile()
                    if(booking?.bookedUsers!!.contains(currentUser?.id) && frag is OthersTripListFragment) {
                        rateNow.visibility = GONE
                        interestedIcon.visibility = GONE
                        bookedIcon.visibility = VISIBLE
                    } else if (booking.interestedUsers.contains(currentUser?.id) && !booking.bookedUsers.contains(currentUser?.id) && frag is OthersTripListFragment){
                        rateNow.visibility = GONE
                        interestedIcon.visibility = VISIBLE
                    }
                }
            }


            // see details
            card.setOnClickListener {
                val bundle1 = bundleOf("tripIndex" to trip.id)
                if (!other) {
                    bundle1.putBoolean("other", false)
                    frag.findNavController().navigate(R.id.action_nav_trip_list_to_nav_trip_details, bundle1)
                } else {
                    bundle1.putBoolean("other", true)
                    when (frag) {
                        is TripsOfInterestListFragment -> {
                            frag.findNavController().navigate(R.id.action_nav_interest_list_to_nav_trip_details,bundle1)
                        }
                        is BoughtTripsListFragment -> {
                            bundle1.putBoolean("booked", true)
                            frag.findNavController().navigate(R.id.action_nav_booking_list_to_nav_trip_details, bundle1)
                        }
                        else ->{
                            if(interestedIcon.visibility==VISIBLE)
                                bundle1.putBoolean("booked", true)
                            frag.findNavController().navigate(R.id.action_nav_trip_list_other_to_nav_trip_details,bundle1
                            )
                        }

                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        return if(other){
            val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.other_card_trip_layout, parent, false)
            TripViewHolder(layout, fragment, true)
        } else{
            val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_trip_layout, parent, false)
            TripViewHolder(layout, fragment, false)
        }
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun add(trip: Trip): MutableList<Trip> {
        data.add(trip)
        this.notifyItemInserted(data.size - 1)
        return data
    }



}
