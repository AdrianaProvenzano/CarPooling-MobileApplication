package it.polito.mad.backToNokia.carpooling.ui.profile

import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.BookingsViewModel
import it.polito.mad.backToNokia.carpooling.data.RatingsViewModel
import it.polito.mad.backToNokia.carpooling.data.TripsViewModel
import it.polito.mad.backToNokia.carpooling.data.UserViewModel
import it.polito.mad.backToNokia.carpooling.model.Rating
import it.polito.mad.backToNokia.carpooling.model.User
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.TripAdapter
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.UserInterestedAdapter
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class ShowProfileFragment : Fragment() {
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private lateinit var nameView: TextView
    private lateinit var nicknameView: TextView
    private lateinit var emailView: TextView
    private lateinit var locationView: TextView
    private lateinit var imageView: ImageView
    private val userViewModel: UserViewModel by activityViewModels()
    private val ratingsViewModel: RatingsViewModel by activityViewModels()
    private val bookingsViewModel: BookingsViewModel by activityViewModels()
    private val tripsViewModel: TripsViewModel by activityViewModels()
    private lateinit var driver:RatingBar
    private lateinit var numRateDriver:TextView
    private lateinit var passenger:RatingBar
    private lateinit var numRatePassenger:TextView
    private lateinit var rv_comment:RecyclerView
    private lateinit var frag: Fragment
    private lateinit var job: Job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        frag=this

        nameView = view.findViewById(R.id.nameView)
        nicknameView = view.findViewById(R.id.nicknameView)
        emailView = view.findViewById(R.id.emailView)
        locationView = view.findViewById(R.id.locationView)
        imageView = view.findViewById(R.id.imageView)

        driver = view.findViewById<RatingBar>(R.id.driver_rating)
        numRateDriver= view.findViewById<TextView>(R.id.num_rating_driver)
        passenger = view.findViewById<RatingBar>(R.id.passenger_rating)
        numRatePassenger= view.findViewById<TextView>(R.id.num_rating_passenger)
        rv_comment=view.findViewById(R.id.rv_comments)

        rv_comment.layoutManager = LinearLayoutManager(this.context)

        //Get authenticated profile data to populate the fragment from the ProfileViewModel
        if( arguments?.get("user")==null) {
            setHasOptionsMenu(true)
            userViewModel.getCurrentUser().observe(viewLifecycleOwner, { user ->
                nameView.text = user.name
                emailView.text = user.email
                if (!user.nickname.isNullOrEmpty() &&  user.nickname != getString(R.string.nickname)) nicknameView.text = user.nickname
                else nicknameView.visibility = GONE
                if (!user.location.isNullOrEmpty() && user.location != getString(R.string.location)) locationView.text = user.location
                else locationView.visibility = GONE
                if (!user.imageRef.isNullOrEmpty()) {
                    val name = user.imageRef
                    val ref: StorageReference = storageRef.child("profile_images/$name")
                    Glide.with(this).load(ref).circleCrop().into(imageView)
                }
                job = MainScope().launch {
                    populateRatingStat(user)
                }
            })
        //Showing profile of others users (only name, nickname, image)
        }else {
            //Disable edit button
            setHasOptionsMenu(false)
            val user = arguments?.get("user") as User
            nameView.text = user.name
            emailView.visibility=GONE
            locationView.visibility=GONE
            if (!user.nickname.isNullOrEmpty() && user.nickname != getString(R.string.nickname)) nicknameView.text = user.nickname
            else nicknameView.visibility = GONE
            if (!user.imageRef.isNullOrEmpty()) {
                val name = user.imageRef
                val ref: StorageReference = storageRef.child("profile_images/$name")
                Glide.with(this).load(ref).circleCrop().into(imageView)
            }
            job = MainScope().launch {
                populateRatingStat(user)
            }
        }

    }

    private suspend fun populateRatingStat(user: User) {
        //MainScope().launch {
            var bookedList = bookingsViewModel.getBookedListByUserId(user.id!!)?.map { it.id!! }
            if (!bookedList.isNullOrEmpty()){
                val p = ratingsViewModel.getPassengerRatings(user.id!!, bookedList)
                var sum_p= 0.0f
                numRatePassenger.text = p?.size.toString()+" "+ requireActivity().getString(R.string.ratings)
                p?.forEach {
                    sum_p+= it.score!!
                }
                passenger.rating= (Math.round(sum_p/p?.size!!*2)/2.0).toFloat()
            } else
                bookedList = listOf()
            var myTrips = tripsViewModel.getTripsByUserId(user.id!!)?.map { it.id!! }
            if (!myTrips.isNullOrEmpty()){
                val d = ratingsViewModel.getDriverRatings(user.id!!, myTrips)
                var sum_d = 0.0f
                numRateDriver.text = d?.size.toString()+" " + requireActivity().getString(R.string.ratings)
                d?.forEach {
                    sum_d+= it.score!!
                }
                driver.rating= (Math.round(sum_d/d?.size!!*2)/2.0).toFloat()
            } else
                myTrips = listOf()

            rv_comment.adapter = CommentAdapter(ratingsViewModel.getRatingsByUserId(user.id!!, bookedList, myTrips)!!, frag)
       // }


    }

    //Click on edit button will launch the EditProfileFragment
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.edit_button -> {
                val navController = requireView().findNavController()
                navController.navigate(R.id.action_nav_show_profile_to_nav_edit_profile)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
    }

}