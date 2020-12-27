package com.textsdev.randogram.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.textsdev.randogram.Homescreen.Companion.SELECT_GALLERY
import com.textsdev.randogram.Homescreen.Companion.SELECT_IMAGE
import com.textsdev.randogram.R
import kotlinx.android.synthetic.main.upload_image_fragment_layout.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class UploadImageFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_image_fragment_layout, container, false)
    }

    override fun onStart() {
        super.onStart()
        cameraButton.setOnClickListener {
            val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            activity?.startActivityForResult(intentCamera, SELECT_IMAGE)
        }

        galleryButton.setOnClickListener {
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activity?.startActivityForResult(pickIntent, SELECT_GALLERY)
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
        fun setImage(imageBitmap: Bitmap, image_viewer: ImageView, activity: Activity) {
            val f = convertToFile(imageBitmap, activity)
            if (f != null) {
                Glide.with(activity).load(f).centerInside().into(image_viewer)
            } else {
                Glide.with(activity).load(
                    ContextCompat.getDrawable(
                        activity,
                        R.drawable.ic_baseline_error
                    )
                ).centerInside().into(image_viewer)
                Toast.makeText(activity, "Failed to Save Image", Toast.LENGTH_SHORT).show()
            }
        }

        fun setImage(imageBitmap: Uri, image_viewer: ImageView, activity: Activity) {
            Glide.with(activity).load(imageBitmap).centerInside().into(image_viewer)
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
    }
}