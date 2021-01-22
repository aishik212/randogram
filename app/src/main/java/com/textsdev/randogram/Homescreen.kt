package com.textsdev.randogram

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.textsdev.randogram.MainActivity.Companion.hideBar
import com.textsdev.randogram.MainActivity.Companion.startA
import com.textsdev.randogram.adapters.ViewPagerAdapter
import com.textsdev.randogram.databinding.HomeLayoutBinding
import com.textsdev.randogram.databinding.HomeToolbarBinding
import com.textsdev.randogram.databinding.UploadImageFragmentLayoutBinding
import com.textsdev.randogram.fragments.UploadImageFragment.Companion.setImage


class Homescreen : AppCompatActivity() {

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private lateinit var binding: HomeLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeLayoutBinding.inflate(layoutInflater)
        val v = binding.root
        setContentView(v)
        hideBar(supportActionBar)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
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
        binding.homeViewpager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.homeViewpager.adapter = adapter
    }

    private fun continueApp(currentUser: FirebaseUser) {
        setDP(currentUser)
        initNavigation()

    }


    private fun initNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            if (it.itemId == R.id.menu_home) {
                binding.homeViewpager.setCurrentItem(0, true)
            } else if (it.itemId == R.id.menu_upload) {
                binding.homeViewpager.setCurrentItem(1, true)
            }
            false
        }

        binding.homeViewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    1 -> {
                        binding.bottomNavigation.menu.findItem(R.id.menu_upload).isChecked = true
                    }
                    else -> {
                        binding.bottomNavigation.menu.findItem(R.id.menu_home).isChecked = true
                    }
                }
            }
        })
    }

    private fun setDP(currentUser: FirebaseUser) {
        val toolbarBinding = HomeToolbarBinding.inflate(layoutInflater)
        val photoUrl = currentUser.photoUrl
        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).circleCrop().into(toolbarBinding.profileDP)
        } else {
            Glide.with(this)
                .load(ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher))
                .circleCrop().into(toolbarBinding.profileDP)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("texts", "onActivityResult: $resultCode $requestCode $data ${data?.extras}")
        val uploadImageFragmentLayoutBinding: UploadImageFragmentLayoutBinding =
            UploadImageFragmentLayoutBinding.inflate(layoutInflater)
        if (requestCode == SELECT_IMAGE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            setImage(imageBitmap, uploadImageFragmentLayoutBinding.imageViewer, this, this.binding)
        } else if (requestCode == SELECT_GALLERY && resultCode == RESULT_OK) {
            val imageBitmap = data?.data as Uri
            setImage(imageBitmap, uploadImageFragmentLayoutBinding.imageViewer, this, this.binding)

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
