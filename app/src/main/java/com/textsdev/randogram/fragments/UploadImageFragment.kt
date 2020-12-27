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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.textsdev.randogram.Homescreen.Companion.SELECT_GALLERY
import com.textsdev.randogram.Homescreen.Companion.SELECT_IMAGE
import com.textsdev.randogram.R
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
        uploadLl = upload_ll
        uploadL2 = upload_ll_2
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
            showUploadOptions()
            Glide.with(requireActivity()).load(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_cloud_upload_24
                )
            ).centerInside().into(image_viewer)
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
        var uploadLl: LinearLayout? = null
        var uploadL2: LinearLayout? = null

        private fun showUploadOptions() {
            uploadLl?.visibility = View.VISIBLE
            uploadL2?.visibility = View.GONE
        }

        private fun showUploadButton() {
            uploadLl?.visibility = View.GONE
            uploadL2?.visibility = View.VISIBLE
        }

        fun setImage(imageBitmap: Bitmap, image_viewer: ImageView, activity: Activity) {
            val f = convertToFile(imageBitmap, activity)
            if (f != null) {
                Glide.with(activity).load(f).centerInside().into(image_viewer)
                showUploadButton()
                initUpload(f)
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

        private fun initUpload(f: File) {
            Log.d("texts", "initUpload: " + f.length())
            Log.d("texts", "initUpload: " + f.absolutePath)
        }

        fun setImage(imageBitmap: Uri, image_viewer: ImageView, activity: Activity) {
            val f = convertToFile(imageBitmap, activity)
            Glide.with(activity).load(f).centerInside().into(image_viewer)
            showUploadButton()
            initUpload(f)
        }

        private fun convertToFile(bitmap: Bitmap, context: Activity): File? {
            val f = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
            if (f.exists()) {
                f.delete()
            }
            if (f.createNewFile()) {
                return try {
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75 /*ignored for PNG*/, bos)
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
            return null
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