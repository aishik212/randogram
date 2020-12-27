package com.textsdev.randogram

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.textsdev.randogram.MainActivity.Companion.getDBRef
import com.textsdev.randogram.MainActivity.Companion.hideBar
import com.textsdev.randogram.MainActivity.Companion.startA
import kotlinx.android.synthetic.main.login.*
import java.lang.Exception
import kotlin.collections.HashMap


class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        hideBar(supportActionBar)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        glogin.setOnClickListener {
            val signInIntent: Intent = googleSignInClient.getSignInIntent()
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
        alogin.setOnClickListener {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startSignupLogin(FirebaseAuth.getInstance().currentUser, this)
                    } else {
                        Toast.makeText(this, "Sign In Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.d("texts", "onActivityResult: " + e.localizedMessage)

            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth = Firebase.auth
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        Toast.makeText(applicationContext, "Login Successful", Toast.LENGTH_SHORT)
                            .show()
                        startSignupLogin(currentUser, this)
                    }
                } else {
                    Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        fun startSignupLogin(currentUser: FirebaseUser?, activity: Activity) {
            if (currentUser != null) {
                if (currentUser.isAnonymous) {
                    goToMainScreen(activity, "Welcome")
                } else {
                    val ref = getDBRef(activity, "users")
                    val userMap: HashMap<String, Any> = hashMapOf()
                    userMap["email"] = currentUser.email + ""
                    userMap["name"] = currentUser.displayName + ""
                    userMap["image"] = currentUser.photoUrl.toString()
                    ref.child(currentUser.uid).setValue(userMap).addOnCompleteListener {
                        if (it.isSuccessful) {
                            goToMainScreen(activity, "Welcome " + currentUser.displayName)
                        } else {
                            Toast.makeText(activity, "Login Failed", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(activity, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                goToMainScreen(activity, "Welcome Anonymous User")
            }
        }

        private fun goToMainScreen(
            activity: Activity,
            s: String
        ) {
            Toast.makeText(activity, s, Toast.LENGTH_SHORT).show()
            val i = Intent(activity, Homescreen::class.java)
            startA(activity, i)
        }
    }
}
