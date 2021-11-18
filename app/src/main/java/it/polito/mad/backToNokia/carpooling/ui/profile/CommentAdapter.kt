package it.polito.mad.backToNokia.carpooling.ui.profile

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.FirebaseService
import it.polito.mad.backToNokia.carpooling.model.Booking
import it.polito.mad.backToNokia.carpooling.model.Rating
import it.polito.mad.backToNokia.carpooling.model.Trip
import it.polito.mad.backToNokia.carpooling.model.User
import it.polito.mad.backToNokia.carpooling.ui.trip.adapter.UserInterestedAdapter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class CommentAdapter(val ratings: List<Rating>, val fragment: Fragment) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    class CommentViewHolder(v: View, val ratings: List<Rating>, val fragment: Fragment): RecyclerView.ViewHolder(v){
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference

        val score = v.findViewById<RatingBar>(R.id.score)
        val rating_user = v.findViewById<TextView>(R.id.rating_user)
        val comment = v.findViewById<TextView>(R.id.comment_rating)
        val picture = v.findViewById<ImageView>(R.id.rating_user_picture)
        suspend fun bind(rating: Rating) {
            score.rating= rating.score!!
            val user= FirebaseService.getUser(rating.rating_user!!)
            rating_user.setText(user?.nickname)
            val ref: StorageReference = storageRef.child("profile_images/${user?.imageRef}")
            Glide.with(fragment.requireContext()).load(ref).circleCrop().into(picture)
            if(rating.driver)
                comment.setText("[As driver]\n"+rating.comment!!)
            else
                comment.setText("[As passenger]\n"+rating.comment!!)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.rating_card_layout, parent, false)
        return CommentViewHolder(v, ratings, fragment)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        MainScope().launch {
            holder.bind(ratings[position])
        }
    }

    override fun getItemCount(): Int {
        return ratings.size
    }
}


