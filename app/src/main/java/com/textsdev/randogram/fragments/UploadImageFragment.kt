package com.textsdev.randogram.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ServerValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.textsdev.randogram.Homescreen.Companion.SELECT_GALLERY
import com.textsdev.randogram.Homescreen.Companion.SELECT_IMAGE
import com.textsdev.randogram.MainActivity
import com.textsdev.randogram.R
import com.textsdev.randogram.fragments.HomeFragment.Companion.fetchData
import kotlinx.android.synthetic.main.home_layout.*
import kotlinx.android.synthetic.main.upload_image_fragment_layout.*
import java.io.*


class UploadImageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_image_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uploadL1 = upload_ll
        uploadL2 = upload_ll_2
        uploadL3 = upload_ll_3
        upload_btn = upload_image_button
        upload_prog = upload_progress
        showUploadOptions()
    }


    override fun onStart() {
        super.onStart()
        cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                activity?.startActivityForResult(intentCamera, SELECT_IMAGE)
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION
                )
            }
        }

        galleryButton.setOnClickListener {
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activity?.startActivityForResult(pickIntent, SELECT_GALLERY)
        }

        cancel_image_button.setOnClickListener {
            resetUploadPage(requireActivity())
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("texts", "onResume: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("texts", "onDestroy: ")
    }


    companion object {
        const val CAMERA_PERMISSION = 100
        const val STORAGE_PERMISSION = 101
        var uploadL1: LinearLayout? = null
        var uploadL2: LinearLayout? = null
        var uploadL3: LinearLayout? = null
        var upload_btn: Button? = null
        var upload_prog: LinearProgressIndicator? = null

        private fun resetUploadPage(activity: Activity) {
            showUploadOptions()
            Glide.with(activity).load(
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.ic_baseline_cloud_upload_24
                )
            ).centerInside().into(activity.image_viewer)
            fetchData(activity.applicationContext, activity)
        }


        private fun showUploadOptions() {
            uploadL1?.visibility = View.VISIBLE
            uploadL2?.visibility = View.GONE
            uploadL3?.visibility = View.GONE
        }

        private fun showUploadButton() {
            uploadL1?.visibility = View.GONE
            uploadL3?.visibility = View.GONE
            uploadL2?.visibility = View.VISIBLE
        }

        private fun showProgress() {
            uploadL1?.visibility = View.GONE
            uploadL3?.visibility = View.VISIBLE
            uploadL2?.visibility = View.GONE
        }


        fun setImage(imageBitmap: Bitmap, image_viewer: ImageView, activity: Activity) {
            val f = convertToFile(imageBitmap, activity)
            if (f != null) {
                Glide.with(activity).load(f).centerInside().into(image_viewer)
                showUploadButton()
                initUpload(f, activity)
            } else {
                Glide.with(activity).load(
                    ContextCompat.getDrawable(
                        activity,
                        R.drawable.ic_baseline_error
                    )
                ).centerInside().into(image_viewer)
                Toast.makeText(activity, "Failed to Save Image", Toast.LENGTH_SHORT).show()
                showUploadOptions()
            }
        }

        private fun initUpload(f: File, activity: Activity) {
            upload_btn?.setOnClickListener {
                showProgress()
                val pkgname = MainActivity.GetPkgName(activity)
                val uid = Firebase.auth.uid.toString()
                val ref =
                    Firebase.storage.reference.child(pkgname).child(uid)
                        .child(f.name).putFile(f.toUri())
                ref.addOnProgressListener {
                    val fl = (it.bytesTransferred.toFloat() / it.totalByteCount.toFloat()) * 100
                    upload_prog?.setProgressCompat(fl.toInt(), true)
                    Toast.makeText(
                        activity,
                        "Uploaded $fl%",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnSuccessListener { uploadTask ->
                    val postsRef = MainActivity.getDBRef(activity, "posts")
                    val userMap: HashMap<String, Any> = hashMapOf()
                    userMap["uid"] = uid + ""
                    userMap["location"] = uploadTask.storage.path
                    userMap["like"] = 0
                    userMap["time"] = ServerValue.TIMESTAMP
                    postsRef.push().setValue(userMap).addOnCompleteListener {
                        Toast.makeText(activity, "Upload Successful", Toast.LENGTH_SHORT).show()
                        updateUI(activity)
                    }.addOnFailureListener {
                        uploadTask.storage.delete()
                        Toast.makeText(activity, "Upload Failed Try Again", Toast.LENGTH_SHORT)
                            .show()
                    }.addOnCanceledListener {
                        uploadTask.storage.delete()
                        Toast.makeText(activity, "Upload Failed Try Again", Toast.LENGTH_SHORT)
                            .show()
                    }
                }.addOnCanceledListener {
                    Toast.makeText(activity, "Upload Cancelled", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        activity,
                        "Upload Failed - " + it.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        private fun updateUI(activity: Activity) {
            activity.home_viewpager.setCurrentItem(0, true)
            resetUploadPage(activity)
        }


        fun setImage(imageBitmap: Uri, image_viewer: ImageView, activity: Activity) {
            var f: File? = convertToFile(imageBitmap, activity)
            if (f != null) {
                val bmp = BitmapFactory.decodeFile(f.absolutePath)
                f = compressImage(bmp, f)
                Glide.with(activity).load(f).centerInside().into(image_viewer)
                showUploadButton()
                if (f != null) {
                    initUpload(f, activity)
                }
            }
        }

        private fun convertToFile(bitmap: Bitmap, context: Activity): File? {
            val f = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
            if (f.exists()) {
                f.delete()
            }
            if (f.createNewFile()) {
                return compressImage(bitmap, f)
            }
            return null
        }

        private fun compressImage(bitmap: Bitmap, f: File): File? {
            var bmp: Bitmap? = resize(bitmap, 2000, 2000)
            if (bmp == null) {
                bmp = bitmap
            }
            return try {
                val bos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 80 /*ignored for PNG*/, bos)
                val bitmapdata = bos.toByteArray()
                val fos = FileOutputStream(f)
                fos.write(bitmapdata)
                fos.flush()
                fos.close()
                f
            } catch (e: Exception) {
                null
            }
        }

        private fun resize(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap? {
            var image = image
            return if (maxHeight > 0 && maxWidth > 0) {
                val width = image.width
                val height = image.height
                val ratioBitmap = width.toFloat() / height.toFloat()
                val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
                var finalWidth = maxWidth
                var finalHeight = maxHeight
                if (ratioMax > ratioBitmap) {
                    finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
                }
                image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
                image
            } else {
                image
            }
        }

        private fun convertToFile(uri: Uri, context: Activity): File {
            val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
            if (!file.exists()) {
                file.createNewFile()
            }
            try {
                val ins: InputStream? = context.getContentResolver().openInputStream(uri)
                val out: OutputStream = FileOutputStream(file)
                val buf = ByteArray(1024)
                var len: Int
                if (ins != null) {
                    while (ins.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                    out.close()
                    ins.close()
                }
            } catch (e: Exception) {
                Log.d("texts", "savefile: " + e.localizedMessage)
            }
            return file
        }
    }
}