package com.textsdev.randogram

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.textsdev.randogram.MainActivity.Companion.hideBar
import com.textsdev.randogram.MainActivity.Companion.startA
import com.textsdev.randogram.adapters.PostAdapter
import com.textsdev.randogram.adapters.ViewPagerAdapter
import com.textsdev.randogram.fragments.UploadImageFragment.Companion.setImage
import kotlinx.android.synthetic.main.home_fragment_layout.*
import kotlinx.android.synthetic.main.home_layout.*
import kotlinx.android.synthetic.main.home_toolbar.*
import kotlinx.android.synthetic.main.upload_image_fragment_layout.*
import java.io.File


class Homescreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_layout)
        hideBar(supportActionBar)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            setupViewPager()
            continueApp(currentUser)
        } else {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {

            }
            val i = Intent(this, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            startA(this, i)
        }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this, 2)
        home_viewpager.orientation = ViewPager2.ORIENTATION_VERTICAL
        home_viewpager.adapter = adapter
    }

    private fun continueApp(currentUser: FirebaseUser) {
        setDP(currentUser)
        initNavigation()

    }


    private fun initNavigation() {
        bottom_navigation.setOnNavigationItemSelectedListener {
            if (it.itemId == R.id.menu_home) {
                home_viewpager.setCurrentItem(0, true)
            } else if (it.itemId == R.id.menu_upload) {
                home_viewpager.setCurrentItem(1, true)
            }
            false
        }

        home_viewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    1 -> {
                        bottom_navigation.menu.findItem(R.id.menu_upload).isChecked = true
                    }
                    else -> {
                        bottom_navigation.menu.findItem(R.id.menu_home).isChecked = true
                    }
                }
            }
        })
    }

    private fun setDP(currentUser: FirebaseUser) {
        val photoUrl = currentUser.photoUrl
        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).circleCrop().into(profileDP)
        } else {
            Glide.with(this)
                .load(ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher))
                .circleCrop().into(profileDP)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("texts", "onActivityResult: $resultCode $requestCode $data ${data?.extras}")
        if (requestCode == SELECT_IMAGE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            setImage(imageBitmap, image_viewer, this)
        } else if (requestCode == SELECT_GALLERY && resultCode == RESULT_OK) {
            val imageBitmap = data?.data as Uri
            setImage(imageBitmap, image_viewer, this)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    this,
                    "Camera Permission Granted Please try Again",
                    Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                    this,
                    "Camera Permission Denied",
                    Toast.LENGTH_SHORT
                ).show();
            }
        } else if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    this,
                    "File Permission Granted Please try Again",
                    Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                    this,
                    "File Permission Denied",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    companion object {
        const val SELECT_IMAGE = 548
        const val SELECT_GALLERY = 549
    }
}
