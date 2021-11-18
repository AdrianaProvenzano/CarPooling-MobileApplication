package it.polito.mad.backToNokia.carpooling

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import it.polito.mad.backToNokia.carpooling.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class LoginActivity : AppCompatActivity() {

    companion object {
        const val RC_SIGN_IN = 117
    }
    private var auth: FirebaseAuth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        val mGoogleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(this, gso)

        //Sign out possible user already signed in
        mGoogleSignInClient.signOut()

        val signInButton = findViewById<SignInButton>(R.id.login)
        signInButton.setOnClickListener {
            signIn(mGoogleSignInClient)
        }
    }

    //Every time we launch the app we sign in with the current user, if present. If not, account will be null.
    override fun onStart() {
        super.onStart()
        val account = auth.currentUser
        updateUI(account)
    }


    private fun updateUI(account: FirebaseUser?){
        if (account!=null){
            //Launch MainActivity with user already signed in
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            //Required to sign in
            Toast.makeText(this, "Sign-in or register required!", Toast.LENGTH_SHORT).show()
        }
    }

    //When button sign in is pressed, launch Google activity to sign in.
    private fun signIn(mGoogleSignInClient: GoogleSignInClient) {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach a listener
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    //Retrieve the user account from the task returned by GoogleSignIn
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account!!.idToken!!)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(null,"signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }

    //It exchanges a token given by the google account with the credentials that we will use to sign in with Firebase
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        //Sign in to Firebase
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(null, "signInWithCredential:success")
                        val user = auth.currentUser
                        addUserToDB(user)
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(null, "signInWithCredential:failure", task.exception)
                        updateUI(null)
                    }
                }
    }

    //Every time we sign in we add the user to the db
    private fun addUserToDB(user: FirebaseUser?){
        if (user!=null){
            val db: FirebaseFirestore = FirebaseFirestore.getInstance()
            val userDB = User(id= user.uid, name = user.displayName, email = user.email)
            val userRef = db.collection("users").document(user.uid)
            userRef.get()
                    .addOnSuccessListener { document ->
                        if (!document.exists()) {
                            userRef.set(userDB)
                        } //if the user already exists we ignore
                    }
                    .addOnFailureListener { exception ->
                        Log.d(null, "Error getting documents: ", exception)
                    }
        }

    }

}
