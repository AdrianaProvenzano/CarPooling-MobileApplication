package it.polito.mad.backToNokia.carpooling.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.FirebaseService
import it.polito.mad.backToNokia.carpooling.data.UserViewModel
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalCoroutinesApi
class EditProfileFragment : Fragment(), PopupMenu.OnMenuItemClickListener {
    companion object {
        const val REQUEST_IMAGE_CAPTURE = 100
        const val REQUEST_IMAGE_LOAD = 101
    }
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val currentUser: FirebaseUser = Firebase.auth.currentUser!!
    private lateinit var editName: EditText
    private lateinit var editNickname: EditText
    private lateinit var editEmail: EditText
    private lateinit var editLocation: EditText
    private lateinit var imageView: ImageView
    private lateinit var imageButton: ImageButton
    private var imageRef: String? = null
    private var photoPath: Uri? = null
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //need this to handle save button on fragment
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageButton = view.findViewById(R.id.image_button)
        editName = view.findViewById(R.id.edit_name)
        editNickname = view.findViewById(R.id.edit_nick)
        editEmail = view.findViewById(R.id.edit_email)
        editLocation = view.findViewById(R.id.edit_location)
        imageView = view.findViewById(R.id.edit_image)

        //Get profile data to populate the fragment from the ProfileViewModel
        if (savedInstanceState==null){
            val user = userViewModel.getCurrentUser().value
            editName.setText(user!!.name)
            editNickname.setText(user.nickname)
            editEmail.setText(user.email)
            editLocation.setText(user.location)
            imageRef = user.imageRef
            if (!user.imageRef.isNullOrEmpty()) {
                val name = user.imageRef
                val ref: StorageReference = storageRef.child("profile_images/$name")
                Glide.with(this).load(ref).circleCrop().into(imageView)
            }

            //If a saved instance state exists, get image from it
        } else {
            photoPath = savedInstanceState.getParcelable("new_profile_image")
            imageRef = savedInstanceState.getString("old_profile_image")

            //If new image has been setted, show it
            if (photoPath!=null){
                Glide.with(this).load(photoPath.toString()).circleCrop().into(imageView)
                //Else if old image was already setted, show it
            } else {
                if (!imageRef.isNullOrEmpty()){
                    val ref: StorageReference = storageRef.child("profile_images/$imageRef")
                    Glide.with(this).load(ref).circleCrop().into(imageView)
                }
            }
        }

        //Popup menu to pick/take image profile
        imageButton.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.setOnMenuItemClickListener(this)
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.load_picture_menu, popup.menu)
            popup.show()

        }
    }

    //Needs it to maintain the status of the fragment when we rotate the device (only for images, edit text views keeps it by themselves)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("new_profile_image", photoPath)
        outState.putString("old_profile_image", imageRef)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_menu, menu)
    }

    // Tapping on the saving icon it saves all the changes on the db and go back to ShowProfileFragment
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.save_button -> {
                    saveProfile()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveProfile(){
        MainScope().launch(Dispatchers.IO) {
            val user = User(currentUser.uid, editName.text.toString(), editNickname.text.toString(), editEmail.text.toString(), editLocation.text.toString(), imageRef)
            //If a new image has been setted, photoPath must not be null
            if (photoPath!=null) {
                //imageRef exists if an image was already setted and it must be deleted from the db
                if (!imageRef.isNullOrEmpty()) {
                    //old image location
                    val oldRef = storageRef.child("profile_images/$imageRef")
                    FirebaseService.deleteImage(oldRef)
                }
                //A timestamp will be used as name of the image file in the db
                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
                //Location of the new image in the db
                val newRef = storageRef.child("profile_images/$timeStamp.jpg")
                user.imageRef = "$timeStamp.jpg"
                val imageStream = FileInputStream(File(photoPath.toString()))
                //After the old image is deleted, the new one is uploaded
                FirebaseService.uploadImage(newRef, imageStream )
            }
            withContext(Dispatchers.Main){
                //Uploading user changes
                userViewModel.updateCurrentUser(user)
                //Go back to showProfileFragment
                val navController = requireView().findNavController()
                navController.popBackStack()
            }
        }

    }

    //Handle the popup menu to pick/take a picture
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId ?: false) {
            R.id.take_picture_from_camera -> {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                try{
                    val photoFile: File = createImageFile()
                    photoFile.also {
                        val photoURI = FileProvider.getUriForFile(requireContext(), getString(R.string.file_provider), photoFile)
                        // MediaStore.EXTRA_OUTPUT means that I am passing photoURI as a container file where to store the requested picture from the camera
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent,
                                REQUEST_IMAGE_CAPTURE
                        )
                    }
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                true
            }
            R.id.pick_picture_from_gallery -> {
                val pickPictureIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                try{
                    startActivityForResult(pickPictureIntent,
                            REQUEST_IMAGE_LOAD
                    )
                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
                true
            }
            else -> false
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            photoPath = Uri.parse(absolutePath)
        }
    }

    // Retrieve picture from intents and show it on screen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            Glide.with(this).load(photoPath.toString()).circleCrop().into(imageView)
        }
        if (requestCode == REQUEST_IMAGE_LOAD && resultCode == AppCompatActivity.RESULT_OK && data!=null) {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(data.data!!)
            val fileOutputStream = FileOutputStream(createImageFile())
            inputStream!!.copyTo(fileOutputStream)
            fileOutputStream.close()
            inputStream.close()
            Glide.with(this).load(photoPath.toString()).circleCrop().into(imageView)
        }

    }
}