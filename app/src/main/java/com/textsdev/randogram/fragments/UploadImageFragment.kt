package com.textsdev.randogram.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.textsdev.randogram.databinding.HomeLayoutBinding
import com.textsdev.randogram.databinding.UploadImageFragmentLayoutBinding
import com.textsdev.randogram.fragments.HomeFragment.Companion.fetchData
import com.textsdev.randogram.ml.Nsfwown
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*


class UploadImageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        uploadImageFragmentLayoutBinding =
            UploadImageFragmentLayoutBinding.inflate(layoutInflater, container, false)
        return uploadImageFragmentLayoutBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uploadL1 = uploadImageFragmentLayoutBinding.uploadLl
        captionET = uploadImageFragmentLayoutBinding.captionEt
        uploadL2 = uploadImageFragmentLayoutBinding.uploadLl2
        uploadL3 = uploadImageFragmentLayoutBinding.uploadLl3
        upload_btn = uploadImageFragmentLayoutBinding.uploadImageButton
        upload_prog = uploadImageFragmentLayoutBinding.uploadProgress
        showUploadOptions()
    }


    override fun onStart() {
        super.onStart()
        uploadImageFragmentLayoutBinding.cameraButton.setOnClickListener {
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

        uploadImageFragmentLayoutBinding.galleryButton.setOnClickListener {
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activity?.startActivityForResult(pickIntent, SELECT_GALLERY)
        }

        uploadImageFragmentLayoutBinding.cancelImageButton.setOnClickListener {
            resetUploadPage(requireActivity())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("texts", "onDestroy: ")
    }


    companion object {
        const val CAMERA_PERMISSION = 100
        var uploadL1: LinearLayout? = null
        var captionET: EditText? = null
        var uploadL2: LinearLayout? = null
        var uploadL3: LinearLayout? = null
        var upload_btn: Button? = null
        var upload_prog: LinearProgressIndicator? = null
        private lateinit var uploadImageFragmentLayoutBinding: UploadImageFragmentLayoutBinding

        private fun resetUploadPage(activity: Activity) {
            showUploadOptions()
            Glide.with(activity).load(
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.ic_baseline_cloud_upload_24
                )
            ).centerInside().into(uploadImageFragmentLayoutBinding.imageViewer)
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

        fun addWatermark(source: Bitmap, watermark: Bitmap, ratio: Float): Bitmap {
            val canvas: Canvas
            val paint: Paint
            val bmp: Bitmap
            val matrix: Matrix
            val r: RectF
            val width: Int
            val height: Int
            val scale: Float
            width = source.width
            height = source.height

            // Create the new bitmap
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Copy the original bitmap into the new one
            canvas = Canvas(bmp)
            canvas.drawBitmap(source, 0F, 0F, paint)

            // Scale the watermark to be approximately to the ratio given of the source image height
            scale = (height.toFloat() * ratio / watermark.height.toFloat())

            // Create the matrix
            matrix = Matrix()
            matrix.postScale(scale, scale)

            // Determine the post-scaled size of the watermark
            r = RectF(0F, 0F, watermark.width.toFloat(), watermark.height.toFloat())
            matrix.mapRect(r)

            // Move the watermark to the bottom right corner
            matrix.postTranslate(width - r.width(), height - r.height())

            // Draw the watermark
            canvas.drawBitmap(watermark, matrix, paint)
            return bmp
        }


        fun setImage(
            imageBitmap: Bitmap,
            activity: Activity,
            binding: HomeLayoutBinding
        ) {
            val f: File? = convertToFile(imageBitmap, activity)
            continueAfterSetImage(f, activity, binding)
        }

        private fun checkNSFW1(
            newBMP: Bitmap?,
            activity: Activity
        ): Boolean {
            if (newBMP != null) {
                val resizedBMAP = Bitmap.createScaledBitmap(newBMP, 224, 224, true)
                val model2 = Nsfwown.newInstance(activity)
                val floatArray = getMLResult(resizedBMAP, model2)
                val nonsfwChance = floatArray[0]
                val nsfwChance = floatArray[1]
                Log.d("texts", "checkNSFW1: " + nonsfwChance + " " + nsfwChance)
                return nonsfwChance < nsfwChance
            } else {
                return false
            }
        }

        private fun getMLResult(
            resizedBMAP: Bitmap?,
            model: Nsfwown
        ): FloatArray {
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
            val tensorBuffer = TensorImage.fromBitmap(resizedBMAP)
            val byteBuffer = tensorBuffer.buffer
            inputFeature0.loadBuffer(byteBuffer)
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            model.close()
            return outputFeature0.floatArray
        }

        fun addWMARK(
            activity: Activity,
            imageBitmap: Bitmap
        ): Bitmap? {
            val watermark =
                BitmapFactory.decodeResource(activity.resources, R.drawable.randogram_transparent)
            var imageBMPW: Bitmap? = null
            if (watermark != null) {
                imageBMPW = addWatermark(
                    imageBitmap,
                    watermark,
                    0.1F
                )
            }
            return imageBMPW
        }

        private fun initUpload(f: File, activity: Activity, binding: HomeLayoutBinding) {
            upload_btn?.setOnClickListener {
                showProgress()
                val pkgname = MainActivity.getPkgName(activity)
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
                    val userMap: HashMap<String, Any?> = hashMapOf()
                    userMap["uid"] = uid + ""
                    userMap["location"] = uploadTask.storage.path
                    if (captionET != null && captionET!!.text != null) {
                        if (captionET!!.text.trim() == "") {
                            userMap["caption"] = null
                        } else {
                            userMap["caption"] = captionET!!.text.toString()
                        }
                    } else {
                        userMap["caption"] = null
                    }
                    userMap["like"] = 0
                    userMap["time"] = ServerValue.TIMESTAMP
                    postsRef.push().setValue(userMap).addOnCompleteListener {
                        Toast.makeText(activity, "Upload Successful", Toast.LENGTH_SHORT).show()
                        updateUI(activity, binding)
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

        private fun updateUI(activity: Activity, binding: HomeLayoutBinding) {
            binding.homeViewpager.setCurrentItem(0, true)
            resetUploadPage(activity)
        }


        fun setImage(
            imageUri: Uri,
            activity: Activity,
            binding: HomeLayoutBinding
        ) {
            val f: File = convertToFile(imageUri, activity)
            continueAfterSetImage(f, activity, binding)
        }

        private fun continueAfterSetImage(
            f: File?,
            activity: Activity,
            binding: HomeLayoutBinding
        ) {
            var f1 = f
            if (f1 != null) {
                val bmp = BitmapFactory.decodeFile(f1.absolutePath)
                val imageViewer = activity.findViewById<ImageView>(R.id.image_viewer)
                val newBMP = addWMARK(activity, bmp)
                if (newBMP != null) {
                    val isNSFW = checkNSFW1(newBMP, activity)
                    if (!isNSFW) {
                        f1 = compressImage(newBMP, f1)
                        if (f1 != null) {
                            Glide.with(activity).load(f1).centerInside().into(imageViewer)
                            showUploadButton()
                            initUpload(f1, activity, binding)
                        } else {
                            Glide.with(activity).load(
                                ContextCompat.getDrawable(
                                    activity,
                                    R.drawable.ic_baseline_error
                                )
                            ).centerInside().into(imageViewer)
                            Toast.makeText(activity, "Failed to Save Image", Toast.LENGTH_SHORT)
                                .show()
                            showUploadOptions()
                        }
                    } else {
                        Toast.makeText(
                            activity,
                            "Warning Vulgar Images are not allowed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
            var imageVar = image
            return if (maxHeight > 0 && maxWidth > 0) {
                val width = imageVar.width
                val height = imageVar.height
                val ratioBitmap = width.toFloat() / height.toFloat()
                val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
                var finalWidth = maxWidth
                var finalHeight = maxHeight
                if (ratioMax > ratioBitmap) {
                    finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
                }
                imageVar = Bitmap.createScaledBitmap(imageVar, finalWidth, finalHeight, true)
                imageVar
            } else {
                imageVar
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