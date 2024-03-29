package com.textsdev.randogram

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.textsdev.randogram.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        hideBar(supportActionBar)

        try {
            Firebase.database.setPersistenceEnabled(true)
        } catch (e: Exception) {

        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            FirebaseApp.initializeApp(applicationContext)
            maintainance.removeOldImages()
            maintainance.removeLikes(applicationContext)
            maintainance.removeOldDB(applicationContext)
            mainBinding.letsgo.visibility = View.GONE
            Toast.makeText(this, "Welcome to RandoGram", Toast.LENGTH_SHORT).show()
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    this@MainActivity.runOnUiThread {
                        val i = Intent(this@MainActivity, Homescreen::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startA(this@MainActivity, i)
                    }
                }
            }, 1000)
        } else {
            mainBinding.letsgo.setOnClickListener {
                val i = Intent(this, Login::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startA(this, i)
            }
        }
    }


    companion object {
        fun startA(activity: Activity, intent: Intent) {
            val toBundle = animBundle(activity)
            activity.startActivity(intent, toBundle)
        }

        private fun animBundle(activity: Activity): Bundle? {
            return ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
        }


        fun hideBar(supportActionBar: ActionBar?) {
            supportActionBar?.hide()
        }

        fun getPkgName(context: Context): String {
            return (context.packageName).replace(".", "_")
        }

        fun getDBRef(context: Context, child: String): DatabaseReference {
            val packageName = getPkgName(context)
            return FirebaseDatabase.getInstance().getReference(packageName)
                .child(BuildConfig.BUILD_TYPE).child(child)
        }

    }
}