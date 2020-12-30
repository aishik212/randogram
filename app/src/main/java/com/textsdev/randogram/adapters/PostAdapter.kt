package com.textsdev.randogram.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.textsdev.randogram.MainActivity
import com.textsdev.randogram.R
import org.json.JSONObject
import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PostAdapter(
    private val posts: ArrayList<HashMap<String, Any>>,
    private val context: Context
) :

    RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var nameTv: TextView = v.findViewById(R.id.name_row_tv)
        private var timeTv: TextView = v.findViewById(R.id.time_row_tv)
        private var niceTv: TextView = v.findViewById(R.id.nice_tv)
        private var postImv: ImageView = v.findViewById(R.id.post_imv)
        private var niceImv: ImageView = v.findViewById(R.id.nice_imv)
        private var moreImageView: ImageButton = v.findViewById(R.id.more_menu_btn)

        init {
            moreImageView.setOnClickListener {
                onClick(it)
            }
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.more_menu_btn -> {

                }
                (R.id.nice_imv or R.id.nice_tv) -> {

                }
            }
        }

        fun bind(
            absolutePath: String?,
            UName: String,
            hashMap: java.util.HashMap<String, Any>,
            context: Context,
        ) {
            nameTv.text = UName
            val time = hashMap["time"].toString()

            timeTv.text = convertLongToDuration(time.toLong())
            val niceness = hashMap["likes"].toString()
            if (niceness.toInt() == 69) {
                niceTv.text = context.getString(R.string.nice)
            } else if (niceness.toInt() < 69) {
                niceTv.text = niceness
            }
            if (absolutePath != null) {
                val file = File(absolutePath)
                if (file.exists() && file.length() > 0) {
                    Glide.with(context).load(file).into(postImv)
                } else {
                    Glide.with(context)
                        .load(ContextCompat.getDrawable(context, R.drawable.ic_baseline_removed))
                        .into(postImv)
                }
            } else {
                Glide.with(context)
                    .load(ContextCompat.getDrawable(context, R.drawable.ic_baseline_removed))
                    .into(postImv)
            }
        }

        private fun convertLongToDuration(time: Long): String {
            val curtime = System.currentTimeMillis()
            val diff = (curtime - time)/1000
            Log.d("texts", "convertLongToDuration: $diff")
            return when {
                diff < 60 -> {
                    "Uploaded In the Last Minute"
                }
                diff < 60 -> {
                    "Uploaded In the Last Hour"
                }
                diff < 60 * 60 -> {
                    "Uploaded In the Last Day"
                }
                diff < 60 * 60 * 24 -> {
                    "Uploaded In the Last Week"
                }
                diff < 60 * 60 * 24 * 7 -> {
                    "Uploaded Lifetimes Ago"
                }
                else -> {
                    "Uploaded In the Last Decade"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.posts_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hashMap = posts[position]
        val uid = hashMap["uid"]
        val pref = context.getSharedPreferences("users", 0)
        if (pref.contains("uid")) {
            val userRawData = pref.getString("uid", "")
            Log.d("texts", "onBindViewHolder: exists")
            continueWithUserData(JSONObject(userRawData.toString()), hashMap, holder)
        } else {
            MainActivity.getDBRef(context, "users").child(uid.toString())
                .addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            val jsonObject = JSONObject()
                            snapshot.children.forEach {
                                jsonObject.put("${it.key}", it.value)
                            }
                            continueWithUserData(jsonObject, hashMap, holder)
                        } else {
                            continueWithUserData(null, hashMap, holder)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
    }

    private fun continueWithUserData(
        userRawData: JSONObject?,
        hashMap: java.util.HashMap<String, Any>,
        holder: ViewHolder
    ) {
        var uname = "Anonymous"
        var dp: String? = null
        if (userRawData != null) {
            uname = userRawData.get("name").toString()
            dp = userRawData.get("image").toString()
        }
        val location: String = hashMap["location"].toString()
        val file: File = File("${context.cacheDir}$location")
        if (file.exists()) {
            holder.bind(file.absolutePath, uname, hashMap, context)
        } else {
            Log.d("texts", "continueWithUserData: " + file.absolutePath)
            val pathname = file.parent
            if (pathname != null) {
                if (File(pathname).mkdirs()) {
                    if (file.createNewFile()) {
                        downloadAndContinue(location, file, holder, uname, hashMap)
                    }
                } else {
                    if (file.createNewFile()) {
                        downloadAndContinue(location, file, holder, uname, hashMap)
                    }
                }
            }
        }
        Log.d("texts", "continueWithUserData: " + hashMap)
    }

    private fun downloadAndContinue(
        location: String,
        file: File,
        holder: ViewHolder,
        UName: String,
        hashMap: java.util.HashMap<String, Any>
    ) {
        FirebaseStorage.getInstance().getReference(location).getFile(file)
            .addOnSuccessListener {
                holder.bind(file.absolutePath, UName, hashMap, context)
            }.addOnFailureListener {
                holder.bind(null, UName, hashMap, context)
            }
    }


    override fun getItemCount() = posts.size
}