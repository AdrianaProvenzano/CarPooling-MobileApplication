package it.polito.mad.backToNokia.carpooling

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.mad.backToNokia.carpooling.data.UserViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.osmdroid.config.Configuration

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(){
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private lateinit var navController: NavController

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //load/initialize the osmdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        navController = findNavController(R.id.nav_host_fragment)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val userImage: ImageView = headerView.findViewById(R.id.userImage)
        val userName: TextView = headerView.findViewById(R.id.userName)
        val userEmail: TextView = headerView.findViewById(R.id.userEmail)
        val logoutButton: Button = headerView.findViewById(R.id.button_logout)

        //Get profile data to populate the NavDrawer header from the ProfileViewModel
        val userViewModel: UserViewModel by viewModels()
        userViewModel.getCurrentUser().observe(this, { user ->
            userName.text = user.name
            userEmail.text = user.email
            if (!user.imageRef.isNullOrEmpty()) {
                val name = user.imageRef
                val ref: StorageReference = storageRef.child("profile_images/$name")
                Glide.with(this).load(ref).circleCrop().into(userImage)
            }
        })

        //Passing each menu ID as a set of Ids because each menu should be considered as top level destinations
        appBarConfiguration = AppBarConfiguration(
                setOf(
                        R.id.nav_trip_list_other, R.id.nav_trip_list, R.id.nav_interest_list, R.id.nav_booking_list, R.id.nav_show_profile
                ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //Handling logout
        logoutButton.setOnClickListener{
            logout()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun logout(){
        Firebase.auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    // permission for osmdroid
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>();
        var i = 0;
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i]);
            i++;
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

}