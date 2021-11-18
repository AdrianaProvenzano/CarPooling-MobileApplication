    package it.polito.mad.backToNokia.carpooling.ui.trip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.R
import it.polito.mad.backToNokia.carpooling.data.FirebaseService
import it.polito.mad.backToNokia.carpooling.data.TripsViewModel
import it.polito.mad.backToNokia.carpooling.model.Stop
import it.polito.mad.backToNokia.carpooling.model.Trip
import kotlinx.coroutines.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.io.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.text.*


    @ExperimentalCoroutinesApi
class TripEditFragment : Fragment(R.layout.fragment_trip_edit), PopupMenu.OnMenuItemClickListener {
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private val currentUser: FirebaseUser = Firebase.auth.currentUser!!
    private lateinit var departureButton: Button
    private lateinit var arrivalButton: Button
    private lateinit var imageView: ImageView
    private lateinit var departureLocView: TextInputLayout
    private lateinit var arrivalLocView: TextInputLayout
    private lateinit var departureDateView: TextInputLayout
    private lateinit var departureTimeView: TextInputLayout
    private lateinit var hoursView:TextInputLayout
    private lateinit var minutesView: TextInputLayout
    private lateinit var seatsView: TextInputLayout
    private lateinit var priceView: TextInputLayout
    private lateinit var descriptionView: TextInputLayout
    private lateinit var deleteButton: Button
    private lateinit var stops_layout: LinearLayout
    private lateinit var addStopButton: Button
    private var mapStopButton: MaterialButton? = null
    private var imageRef: String? = ""
    private var photoPath: Uri? = null
    private lateinit var tripIndex: String

    private var departureCoordinates: GeoPoint? = null
    private var arrivalCoordinates: GeoPoint? = null
    private lateinit var stopsCoordinates:MutableList<GeoPoint>
    private var contStop:Int=0

    private val tripsViewModel: TripsViewModel by activityViewModels()

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 100
        const val REQUEST_IMAGE_LOAD = 101
    }

    // I need if for the OptionMenu
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        departureButton = view.findViewById(R.id.pin_deploc)
        arrivalButton = view.findViewById(R.id.pin_arrloc)
        imageView = view.findViewById(R.id.car_image)
        departureLocView = view.findViewById(R.id.departure_location_view)
        arrivalLocView = view.findViewById(R.id.arrival_location_view)
        departureDateView = view.findViewById(R.id.departure_date_view)
        departureTimeView = view.findViewById(R.id.departure_time_view)
        hoursView = view.findViewById(R.id.hours_duration_view)
        minutesView = view.findViewById(R.id.minutes_duration_view)
        seatsView = view.findViewById(R.id.available_seats_view)
        priceView = view.findViewById(R.id.price_view)
        descriptionView = view.findViewById(R.id.description_view)
        deleteButton = view.findViewById(R.id.delete_trip_button)

        stopsCoordinates = mutableListOf<GeoPoint>()
        contStop=0

        stops_layout = view.findViewById(R.id.stops_view)
        addStopButton = view.findViewById(R.id.button_add_stop)

        addStopButton.setOnClickListener{
            addStopView(null, contStop)
            contStop++
        }

        val button = view.findViewById<ImageButton>(R.id.edit_imageButton)
        button.setOnClickListener { View -> showPopup(View) }

        tripIndex = arguments?.getString("tripIndex")!!

        departureButton.setOnClickListener{
            openMapInputDialog(departureCoordinates, true, null)
        }
        arrivalButton.setOnClickListener{
            openMapInputDialog(arrivalCoordinates, false, null)
        }

        //input validation
        departureLocView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (departureLocView.editText?.text?.isBlank() == true)
                    departureLocView.error = getString(R.string.error_loc)
            }else
                departureLocView.error=null
        }
        arrivalLocView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (arrivalLocView.editText?.text?.isBlank() == true)
                    arrivalLocView.error = getString(R.string.error_loc)
            }else
                arrivalLocView.error=null
        }
        departureDateView.editText?.onFocusChangeListener = View.OnFocusChangeListener{ _, hasFocus ->
            if (hasFocus){
                departureDateView.error=null
                showDataPicker()
            } else {
                if (departureDateView.editText?.text?.isBlank() == true)
                    departureDateView.error = getString(R.string.error_date)
                else {
                    val convertedDate = LocalDate.parse(departureDateView.editText?.text, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    val today = LocalDate.now()
                    if (convertedDate.isBefore(today))
                        departureDateView.error = getString(R.string.error_date_past)
                }
            }
        }
        departureTimeView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (hasFocus){
                departureTimeView.error=null
                showTimePicker(R.id.departure_time)
            } else {
                if (departureTimeView.editText?.text?.isBlank() == true)
                    departureTimeView.error = getString(R.string.error_time)
            }
        }
        hoursView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (hoursView.editText?.text?.isBlank() == true)
                    hoursView.error = getString(R.string.error_hours)
                else if((hoursView.editText?.text?.toString()?.toInt())!!>23)
                    hoursView.error = getString(R.string.error_hours_valid)
            }else
                hoursView.error=null
        }
        minutesView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (minutesView.editText?.text?.isBlank() == true)
                    minutesView.error =  getString(R.string.error_minutes)
                else if((minutesView.editText?.text?.toString()?.toInt())!!>59)
                    minutesView.error =  getString(R.string.error_minutes_valid)
            }else
                minutesView.error=null
        }
        seatsView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (seatsView.editText?.text?.isBlank() == true)
                    seatsView.error = getString(R.string.error_seats)
                else if(seatsView.editText?.text?.toString()?.toInt()!! <=0)
                    seatsView.error = getString(R.string.error_seats_0)
            }else
                seatsView.error=null
        }
        priceView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (priceView.editText?.text?.isBlank() == true)
                    priceView.error = getString(R.string.error_price)
            }else
                priceView.error=null
        }
        descriptionView.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (descriptionView.editText?.text?.isBlank() == true)
                    descriptionView.error = getString(R.string.error_description)
            }else
                descriptionView.error=null
        }

        val trip = tripsViewModel.getTrip(tripIndex).value
        if (savedInstanceState==null){
            if (tripIndex.isNotEmpty()){
                if (!trip!!.car_image.isNullOrEmpty()) {
                    imageRef=trip.car_image
                    val ref: StorageReference = storageRef.child("trips_images/${trip.car_image}")
                    Glide.with(this).load(ref).into(imageView)
                }

                departureCoordinates = GeoPoint(trip.departure_coordinates!!.latitude, trip.departure_coordinates!!.longitude)
                arrivalCoordinates = GeoPoint(trip.arrival_coordinates!!.latitude, trip.arrival_coordinates!!.longitude)
                departureLocView.editText?.setText(trip.departure_location)
                arrivalLocView.editText?.setText(trip.arrival_location)
                departureDateView.editText?.setText(trip.departure_date)
                departureTimeView.editText?.setText(trip.departure_time)
                hoursView.editText?.setText(trip.hours_duration.toString())
                minutesView.editText?.setText(trip.minutes_duration.toString())
                seatsView.editText?.setText(trip.available_seats.toString())
                priceView.editText?.setText(DecimalFormat("#.##").format(trip.price).toString())
                descriptionView.editText?.setText(trip.description)

                val list = trip.stopList
                if (list != null) {
                    for( s in list){
                        stopsCoordinates.add(GeoPoint(s.location_coordinates?.latitude!!, s.location_coordinates?.longitude!!))
                        addStopView(s, contStop)
                        contStop++
                    }
                    Log.d("adri", stopsCoordinates.toString())
                }
            } else {
                deleteButton.visibility = GONE
            }
        } else {
            photoPath = savedInstanceState.getParcelable("new_image")
            //If new image has been setted, show it
            if (photoPath!=null){
                Glide.with(this).load(photoPath.toString()).into(imageView)
                //Else if old image was already setted, show it
            } else {
                if (!trip!!.car_image.isNullOrEmpty()){
                    imageRef=trip.car_image
                    val ref: StorageReference = storageRef.child("trips_images/$imageRef")
                    Glide.with(this).load(ref).into(imageView)
                }
            }
        }

        //Handling delete trip button
        deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Trip")
                .setMessage("Do you want to delete the trip?")
                .setNeutralButton("Cancel") { dialog, which ->

                }
                .setPositiveButton("Delete") { dialog, which ->
                    tripsViewModel.deleteTrip(trip!!)
                    val navController = requireView().findNavController()
                    navController.navigate(R.id.action_nav_trip_edit_to_nav_trip_list)
                }
                .show()
        }

    }

        private fun addStopView(stop:Stop?, indexStop:Int?) {
            val stopView: View = layoutInflater.inflate(R.layout.row_add_stop, null, false)
            val locationStop:TextInputLayout = stopView.findViewById(R.id.new_stop_loc)
            val timeStop:TextInputLayout = stopView.findViewById(R.id.new_stop_hour)
            val removeStop:Button = stopView.findViewById(R.id.remove_stop)
            mapStopButton = stopView.findViewById(R.id.pin_stop)

            if(stop!=null){
                locationStop.editText?.setText(stop.location)
                timeStop.editText?.setText(stop.time)
            }

            removeStop.setOnClickListener{
                stops_layout.removeView(stopView);
            }
            timeStop.editText?.onFocusChangeListener=View.OnFocusChangeListener{ _, hasFocus ->
                if (hasFocus)
                    showTimePicker(timeStop.editText?.id!!)
            }
            mapStopButton?.setOnClickListener{
                if(locationStop.editText?.text.toString()!="" && stop==null)
                    openMapInputDialog(null, false, indexStop)
                else
                    openMapInputDialog(GeoPoint(stop?.location_coordinates?.latitude!!, stop.location_coordinates?.longitude!!), false, indexStop)
            }
            stops_layout.addView(stopView)
        }

        private fun openMapInputDialog(coordinates: GeoPoint?, isDepartureLocation: Boolean, indexStop:Int?) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val view: View = LayoutInflater.from(requireContext())
            .inflate(R.layout.location_input_layout, null, false)
        val b = builder.setView(view)

        //handle map
        val map = view.findViewById<MapView>(R.id.map_input)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        var newMarker: Marker? = null
        var point: GeoPoint? = null

        if(coordinates != null) {
            //set the current point selected
            newMarker = Marker(map)
            point = GeoPoint(coordinates.latitude, coordinates.longitude)
            newMarker.position = point
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(newMarker)
            mapController.setZoom(15.0)
            mapController.setCenter(coordinates)
        } else {
            // TODO: maybe: get the current position instead of Italy
            val currentPosition = GeoPoint(41.87194, 12.56738)
            mapController.setZoom(7.0)
            mapController.setCenter(currentPosition)
        }

        map.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt())

                if (newMarker != null)
                    mapView.overlays.remove(newMarker)
                newMarker = Marker(mapView)
                point = GeoPoint(geoPoint.latitude, geoPoint.longitude)
                Log.d("OSM", "${geoPoint.latitude}, ${geoPoint.longitude}")
                newMarker!!.position = point
                newMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(newMarker)
                if (map.zoomLevelDouble.equals(7.0))
                    mapController.animateTo(point, 12.0, 2500)
                else
                    mapController.animateTo(point)
                mapView.invalidate()
                return true
            }
        })

        b.
            setPositiveButton("confirm") { dialog, _ ->
                if (newMarker != null) {
                    if(isDepartureLocation)
                        departureCoordinates = newMarker!!.position
                    else if (indexStop==null)
                        arrivalCoordinates = newMarker!!.position
                    else {
                        stopsCoordinates.add(indexStop, newMarker!!.position)
                        Log.d("adri", stopsCoordinates.toString())
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

    }

    private fun showTimePicker(id: Int) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val time = requireView().findViewById<TextInputEditText>(id)
        time.showSoftInputOnFocus=false
        val timepicker = TimePickerDialog(requireView().context, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute_ ->
            time.setText("%02d".format(hourOfDay) + ":%02d".format(minute_))
        }, hour, minute, true)
        timepicker.show()
    }

    private fun showDataPicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val date = requireView().findViewById<TextInputEditText>(R.id.departure_date)
        date.showSoftInputOnFocus = false
        val datepicker = DatePickerDialog(requireView().context, { _, year_, month_, dayOfMonth ->
            date.setText("%02d".format(dayOfMonth) + "/%02d".format(month_.plus(1)) + "/%02d".format(year_))
        }, year, month, day)
        datepicker.show()
    }

    // save trip menu implementation
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.save_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    // by tapping on the saving icon we go back to tripListFragment
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.save_button -> {
                if (checkFormIsFilledOut()) {
                    saveTrip()
                } else {
                    Snackbar.make(requireView(), getString(R.string.missing_field), Snackbar.LENGTH_SHORT).setTextColor(
                        Color.CYAN).show()
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkFormIsFilledOut(): Boolean{
        var stopsOK = true
        for (i in 0 until stops_layout.getChildCount()) {
            val v: View = stops_layout.getChildAt(i)
            if (v is LinearLayout) {
                val location = v.findViewById<TextInputLayout>(R.id.new_stop_loc)
                val time = v.findViewById<TextInputLayout>(R.id.new_stop_hour)
                if ( location.editText!!.text.isNullOrEmpty() || time.editText!!.text.isNullOrEmpty() || stopsCoordinates.size<i)
                    stopsOK = false
            }
        }

        return !( departureLocView.editText!!.text.isNullOrEmpty() ||
                arrivalLocView.editText!!.text.isNullOrEmpty() ||
                departureDateView.editText!!.text.isNullOrEmpty() ||
                departureTimeView.editText!!.text.isNullOrEmpty() ||
                hoursView.editText!!.text.isNullOrEmpty() ||
                minutesView.editText!!.text.isNullOrEmpty() ||
                seatsView.editText!!.text.isNullOrEmpty() ||
                priceView.editText!!.text.isNullOrEmpty() ||
                descriptionView.editText!!.text.isNullOrEmpty() ||
                departureCoordinates==null ||
                arrivalCoordinates==null ||
                !stopsOK)
    }

    private fun saveTrip() {
        val departureLoc = departureLocView.editText?.text.toString()
        val arrivalLoc = arrivalLocView.editText?.text.toString()
        val departureDate = departureDateView.editText?.text.toString()
        val departureTime = departureTimeView.editText?.text.toString()
        val hours = hoursView.editText?.text.toString().toInt()
        val minutes = minutesView.editText?.text.toString().toInt()
        val seats = seatsView.editText?.text.toString().toInt()
        val price = priceView.editText?.text.toString().toDouble()
        val description = descriptionView.editText?.text.toString()

        val stopList = mutableListOf<Stop>()
        var cont = 0
        for (i in 0 until stops_layout.getChildCount()) {
            val v: View = stops_layout.getChildAt(i)
            if (v is LinearLayout) {
                val location = v.findViewById<TextInputLayout>(R.id.new_stop_loc)
                val time = v.findViewById<TextInputLayout>(R.id.new_stop_hour)
                val geo = stopsCoordinates[cont]
                val stop = Stop(location.editText?.text.toString(), com.google.firebase.firestore.GeoPoint(geo!!.latitude, geo!!.longitude), time.editText?.text.toString())
                stopList.add(stop)
                cont++
            }

        }

        Log.d("adri", stopList.toString())

        val trip = Trip(
                departure_location = departureLoc,
                arrival_location = arrivalLoc,
                departure_coordinates = com.google.firebase.firestore.GeoPoint(departureCoordinates!!.latitude, departureCoordinates!!.longitude),
                arrival_coordinates = com.google.firebase.firestore.GeoPoint(arrivalCoordinates!!.latitude, arrivalCoordinates!!.longitude),
                departure_date = departureDate,
                departure_time = departureTime,
                hours_duration = hours,
                minutes_duration = minutes,
                available_seats = seats,
                price = price,
                description = description,
                stopList = stopList,
                user_id = currentUser.uid,
                car_image = imageRef
        )


        MainScope().launch(Dispatchers.IO) {
            //If a new image has been setted, photoPath must not be null
            if (photoPath != null) {
                //imageRef exists if an image was already setted and it must be deleted from the db
                if (!imageRef.isNullOrEmpty()) {
                    //old image location
                    val oldRef = storageRef.child("trips_images/$imageRef")
                    FirebaseService.deleteImage(oldRef)
                }
                //A timestamp will be used as name of the image file in the db
                val timeStamp: String =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
                val imageName = "$timeStamp${currentUser.uid}.jpg"
                //Location of the new image in the db
                val newRef = storageRef.child("trips_images/$imageName")
                trip.car_image = imageName
                val imageStream = FileInputStream(File(photoPath.toString()))
                //val imageStream = FileInputStream(File(photoPath.toString()))
                //After the old image is deleted, the new one is uploaded
                withContext(Dispatchers.Main){
                    FirebaseService.uploadImage(newRef, imageStream)
                }
            }
            withContext(Dispatchers.Main){
                //Update existent trip
                if (tripIndex.isNotEmpty()) {
                    trip.id = tripIndex
                    //Uploading trip changes
                    tripsViewModel.updateTrip(trip)
                    //New Trip
                } else {
                    //Adding new trip to "/trips/id" and corresponding booking to "/bookings/id"
                    tripIndex = tripsViewModel.addTrip(trip)
                }
                //Go back to showProfileFragment
                val navController = requireView().findNavController()
                navController.popBackStack()
            }

        }

    }

    private fun showPopup(v: View) {
        val popup = PopupMenu(this.context, v)
        popup.setOnMenuItemClickListener(this)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.load_picture_menu, popup.menu)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId ?: false) {
            R.id.take_picture_from_camera -> {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                try {
                    val photoFile: File = createImageFile()
                    photoFile.also {
                        val photoURI = FileProvider.getUriForFile(this.requireContext(), getString(R.string.file_provider), photoFile)
                        // MediaStore.EXTRA_OUTPUT means that I am passing photoURI as a container file where to store the requested picture from the camera
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
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
                try {
                    startActivityForResult(pickPictureIntent, REQUEST_IMAGE_LOAD)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
                true
            }
            else -> false
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
        val storageDir: File? = this.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            photoPath = Uri.parse(absolutePath)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            imageView.setImageURI(photoPath)
        }

        if (requestCode == REQUEST_IMAGE_LOAD && resultCode == AppCompatActivity.RESULT_OK && data!=null) {
            val inputStream: InputStream? = this.requireContext().contentResolver.openInputStream(data.data!!)
            val fileOutputStream = FileOutputStream(createImageFile())
            inputStream!!.copyTo(fileOutputStream)
            fileOutputStream.close()
            inputStream.close()
            imageView.setImageURI(photoPath)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("new_image", photoPath)
        //TODO: handle state of geopoints markers
    }
}