package com.textsdev.randogram.adapters

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.textsdev.randogram.MainActivity
import com.textsdev.randogram.R
import com.textsdev.randogram.databinding.PhotoViewerLayoutBinding
import com.textsdev.randogram.fragments.UploadImageFragment
import com.textsdev.randogram.utilities.DownloadFiles
import com.textsdev.randogram.utilities.DownloadFiles.Companion.downloadToDownload
import com.textsdev.randogram.utilities.DownloadFiles.Companion.writeFileContent
import java.io.File

class photoViewerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: PhotoViewerLayoutBinding;

    private var file: File? = null

    private var fname: String? = null
    private var caption: String? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DownloadFiles.CREATE_FILE && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            val externName =
                File(("${cacheDir}/${fname}"))
            if (uri != null) {
                writeFileContent(uri, this, externName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PhotoViewerLayoutBinding.inflate(layoutInflater)
        val v: View = binding.root
        setContentView(v)
        MainActivity.hideBar(supportActionBar)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        if (intent != null) {
            fname = intent.getStringExtra("fileName")
            caption = intent.getStringExtra("caption")
            if (caption != null) {
                binding.captionTv.visibility = View.VISIBLE
                binding.captionTv.text = caption
            } else {
                binding.captionTv.visibility = View.GONE
            }
            file = File(cacheDir.absolutePath + "" + fname)
            if (file != null) {
                val bmp = BitmapFactory.decodeFile(file?.absolutePath)
                val wmark = UploadImageFragment.addWMARK(this, bmp)
                Glide.with(applicationContext).load(wmark).fitCenter().into(binding.largeImage)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            if (v.id == R.id.home) {
                finish()
            } else if (v.id == R.id.download) {
                if (file != null) {
                    downloadToDownload(fname, this, cacheDir)
                }
            }
        }
    }
}
