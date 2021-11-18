package it.polito.mad.backToNokia.carpooling.ui.trip.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.FirebaseService
import it.polito.mad.backToNokia.carpooling.model.Booking
import it.polito.mad.backToNokia.carpooling.model.Rating
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class UserInterestedAdapter(val booking: Booking, val listUsers: List<User>, val trip: Trip, val fragment: Fragment, val rating:Boolean): RecyclerView.Adapter<UserInterestedAdapter.UserInterestedViewHolder>() {

    class UserInterestedViewHolder(v: View, val fragment: Fragment, val booking: Booking, val trip: Trip, val rating:Boolean, val notifyDataChanged: ()->Unit): RecyclerView.ViewHolder(v) {
        private val nicknameView: TextView = v.findViewById(R.id.interested_user_nickname)
        private val buttonConfirmBooking: Button = v.findViewById(R.id.confirm_booking)
        private val confirmedIconView: ImageView = v.findViewById(R.id.confirmed_icon)
        private val card: CardView = v.findViewById(R.id.card_users_interested)
        val rate_button:Button = v.findViewById(R.id.button_rate_passengers)
        private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

        fun bind(user: User){
            if (!user.nickname.isNullOrEmpty()){
                nicknameView.text = user.nickname
            } else nicknameView.text = user.email
            if (booking.bookedUsers.contains(user.id)){
                buttonConfirmBooking.visibility = GONE
                confirmedIconView.visibility = VISIBLE
            }
            card.setOnClickListener {
                val bundle = bundleOf("user" to user)
                fragment.findNavController().navigate(R.id.action_nav_user_interested_to_nav_show_profile, bundle)
            }

            if(rating){
                buttonConfirmBooking.visibility= GONE
                confirmedIconView.visibility=GONE
                rate_button.visibility= VISIBLE
                MainScope().launch{
                    if(FirebaseService.passengerRatingExist(trip.id!!, user.id!!))
                        rate_button.isEnabled = false
                }
                rate_button.setOnClickListener {
                    MainScope().launch {
                        openRateDialog(user, false, notifyDataChanged)
                    }
                }
            }
            else{
                if (trip.available_seats==0) {
                    buttonConfirmBooking.isEnabled = false
                    buttonConfirmBooking.setBackgroundColor(fragment.resources.getColor(R.color.dirty_white_blue))
                }

                buttonConfirmBooking.setOnClickListener {
                    MainScope().launch {
                        //Adding userId to bookedUser list of the booking and updating available_seats of the trip
                        FirebaseService.setBookedUser(booking.id!!, user.id!! )
                        booking.bookedUsers.add(user.id!!)
                        trip.available_seats = trip.available_seats!!.minus(1)
                        notifyDataChanged()

                    }
                }
            }
        }

        private suspend fun openRateDialog(user: User, driver:Boolean, notifyDataChanged: () -> Unit) {
            val builder =  MaterialAlertDialogBuilder(fragment.requireContext())
            val view:View = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.rating_layout, null, false)

            val b = builder.setView(view)
            val userimage = view.findViewById<ImageView>(R.id.image_user_to_rate)
            val username = view.findViewById<TextView>(R.id.username_to_rate)

            if(user.nickname==null)
                username.text = fragment.context?.getString(R.string.rate)+"\n"+user.name  //TODO: or email?
            else
                username.text = fragment.context?.getString(R.string.rate)+" "+user.nickname

            if (!user.imageRef.isNullOrEmpty()) {
                val ref: StorageReference = storageRef.child("profile_images/${user.imageRef}")
                Glide.with(fragment.requireContext()).load(ref).circleCrop().into(userimage)
            }
            else
                userimage.setImageResource(R.drawable.user_icon)

            b.setPositiveButton(fragment.context?.getString(R.string.rate)) { dialog, _ ->
                val userAuth = Firebase.auth.currentUser!!
                val score = view.findViewById<RatingBar>(R.id.rating_bar).rating
                val comment = view.findViewById<EditText>(R.id.comment).text.toString()
                MainScope().launch(){
                    val rating = Rating(user.id, userAuth.uid, booking.id, score, comment, driver)
                    FirebaseService.addRating(rating)
                    notifyDataChanged()
                }
                dialog.dismiss()
            }
            .setNegativeButton(fragment.context?.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserInterestedViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.user_interested_card, parent, false)
        return UserInterestedViewHolder(v, fragment, booking, trip, rating, notifyDataChanged)
    }

    override fun onBindViewHolder(
        holder: UserInterestedViewHolder,
        position: Int
    ) {
        if(rating){
            val u = booking.bookedUsers[position]
            val user = listUsers.find{it.id==u}!!
            holder.bind(user)
        }
        else{
            val u = booking.interestedUsers[position]
            val user = listUsers.find{it.id==u}!!
            holder.bind(user)
        }


    }

    override fun getItemCount(): Int {
        return listUsers.size
    }

    val notifyDataChanged = {
        this.notifyDataSetChanged()
    }
}
