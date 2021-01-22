package com.textsdev.randogram.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.textsdev.randogram.MainActivity
import com.textsdev.randogram.R
import com.textsdev.randogram.fragments.HomeFragment
import com.textsdev.randogram.fragments.UploadImageFragment
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackView
import org.json.JSONObject
import java.io.File


class PostAdapter(
    private val posts: ArrayList<HashMap<String, Any?>?>,
    private val context: Context,
    private val activity: Activity,
    private val cardStackLayoutManager: CardStackLayoutManager,
    private val posts_rv: CardStackView
) :

    RecyclerView.Adapter<PostAdapter.ViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        return if (posts[position] == null) {
            1
        } else {
            0
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var nameTv: TextView = v.findViewById(R.id.name_row_tv)
        private var timeTv: TextView = v.findViewById(R.id.time_row_tv)
        private var postTopCL: ConstraintLayout = v.findViewById(R.id.postTopCL)
        private var postImv: ImageView = v.findViewById(R.id.post_imv)
        private var moreImageView: ImageButton = v.findViewById(R.id.more_menu_btn)
        var like: Button = v.findViewById(R.id.like)
        var skip: Button = v.findViewById(R.id.skip)
        var nicebtn: Button = v.findViewById(R.id.nice_btn)
        var caption_tv: TextView = v.findViewById(R.id.caption_tv)


        fun bind(
            absolutePath: String?,
            UName: String,
            hashMap: java.util.HashMap<String, Any?>,
            context: Context,
            activity: Activity,
            cardStackLayoutManager: CardStackLayoutManager,
            posts_rv: CardStackView,
        ) {
            nameTv.text = UName
            val time = hashMap["time"].toString()
            val caption = hashMap["caption"]
            if (caption != null) {
                caption_tv.visibility = View.VISIBLE
                caption_tv.text = caption.toString()
            } else {
                caption_tv.visibility = View.GONE
            }
            timeTv.text = convertLongToDuration(time.toLong())
            val ref = hashMap["reference"] as DatabaseReference
            val niceness = hashMap["likes"].toString()
            val child = MainActivity.getDBRef(context, "likes").child(ref.key.toString())
            updateLikeTop(child, niceness, context)
            if (absolutePath != null) {
                val file = File(absolutePath)
                if (file.exists() && file.length() > 0) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    val wmark = UploadImageFragment.addWMARK(activity, bmp)
                    if (wmark != null) {
                        Glide.with(context).load(wmark).into(postImv)
                    } else {
                        try {
                            Glide.with(context).load(bmp).into(postImv)
                        } catch (e: Exception) {
                            Glide.with(context).load(file).into(postImv)
                        }
                    }
                } else {
                    file.delete()
                    Glide.with(context)
                        .load(ContextCompat.getDrawable(context, R.drawable.ic_baseline_removed))
                        .into(postImv)
                }
            } else {
                Glide.with(context)
                    .load(ContextCompat.getDrawable(context, R.drawable.ic_baseline_removed))
                    .into(postImv)
            }
            postTopCL.tag = ref
            postImv.setOnClickListener {
                val i = Intent(context, photoViewerActivity::class.java)
                val location = hashMap["location"]
                if (location != null) {
                    i.putExtra("fileName", location.toString())
                    if (caption != null) {
                        i.putExtra("caption", caption.toString())
                    }
                    context.startActivity(i)
                }
            }

            like.setOnClickListener {
                HomeFragment.swipeLike(cardStackLayoutManager, posts_rv)
            }
            skip.setOnClickListener {
                HomeFragment.swipeSkip(cardStackLayoutManager, posts_rv)
            }
        }

        private fun updateLikeTop(
            child: DatabaseReference,
            niceness: String,
            context: Context
        ) {
            child.child(FirebaseAuth.getInstance().currentUser?.uid + "")
                .addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                updateLikes(
                                    niceness.toInt(),
                                    context,
                                    true
                                )
                            } else {
                                updateLikes(
                                    niceness.toInt(),
                                    context,
                                    false
                                )
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            updateLikes(
                                niceness.toInt(),
                                context,
                                false
                            )
                        }
                    })
        }

        private fun updateLikes(
            niceCount: Int,
            context: Context,
            likedByUser: Boolean
        ) {
            if (!likedByUser) {
                like.text = "Like"
                var drawable: Drawable? = ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_outline_thumb_up_alt_24
                )
                drawable = setDTint(drawable, Color.BLACK)
                like.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                if (niceCount == 1) {
                    nicebtn.text = "$niceCount Like"
                } else {
                    nicebtn.text = "$niceCount Likes"
                }
            } else {
                like.text = "Liked"
                when {
                    niceCount == 69 -> {
                        nicebtn.text = "Noice"
                        var drawable: Drawable? = ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_baseline_favorite_24
                        )
                        drawable = setDTint(drawable, Color.parseColor("#F60041"))
                        nicebtn.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                    }
                    (niceCount < 69 || niceCount > 69) -> {
                        if (niceCount == 1) {
                            nicebtn.text = "$niceCount Like"
                        } else {
                            nicebtn.text = "$niceCount Likes"
                        }
                    }
                }
            }
        }

        private fun setDTint(drawable: Drawable?, color: Int): Drawable? {
            DrawableCompat.wrap(drawable!!)
            DrawableCompat.setTint(drawable, color)
            return drawable
        }

        private fun convertLongToDuration(time: Long): String {
            val curtime = System.currentTimeMillis()
            val diff = (curtime - time) / 1000
            return when {
                diff < 60 -> {
                    "Seconds Ago"
                }
                diff < 60 * 60 -> {
                    "Minutes Ago"
                }
                diff < 60 * 60 * 24 -> {
                    "Hours Ago"
                }
                diff < 60 * 60 * 24 * 7 -> {
                    "Days Ago"
                }
                diff < 60 * 60 * 24 * 365 -> {
                    "Lifetimes Ago"
                }
                else -> {
                    "Decades Ago"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.posts_row, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.posts_native_ad, parent, false)

        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hashMap = posts[position]
        if (hashMap != null) {
            val uid = hashMap["uid"]
            val reference: DatabaseReference = hashMap["reference"] as DatabaseReference
            val pref = context.getSharedPreferences("users", 0)
            if (pref.contains(uid.toString())) {
                val userRawData = pref.getString(uid.toString(), "")
                continueWithUserData(JSONObject(userRawData.toString()), hashMap, holder, reference)
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
                                pref.edit().putString(uid.toString(), jsonObject.toString()).apply()
                                continueWithUserData(jsonObject, hashMap, holder, reference)
                            } else {
                                continueWithUserData(null, hashMap, holder, reference)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
            }
        } else {
            //inflate ad
            holder.skip.setOnClickListener {
                HomeFragment.swipeSkip(cardStackLayoutManager, posts_rv)
            }
            holder.like.setOnClickListener {
                HomeFragment.swipeLike(cardStackLayoutManager, posts_rv)
            }
        }
    }

    private fun continueWithUserData(
        userRawData: JSONObject?,
        hashMap: java.util.HashMap<String, Any?>,
        holder: ViewHolder,
        reference: DatabaseReference
    ) {
        var uname = "Anonymous"
        //var dp: String? = null
        if (userRawData != null) {
            uname = userRawData.get("name").toString()
            //dp = userRawData.get("image").toString()
        }
        val location: String = hashMap["location"].toString()
        val file = File("${context.cacheDir}$location")
        if (file.exists()) {
            holder.bind(
                file.absolutePath,
                uname,
                hashMap,
                context,
                activity,
                cardStackLayoutManager,
                posts_rv
            )
        } else {
            val pathname = file.parent
            if (pathname != null) {
                if (File(pathname).mkdirs()) {
                    if (file.createNewFile()) {
                        downloadAndContinue(location, file, holder, uname, hashMap, reference)
                    }
                } else {
                    if (file.createNewFile()) {
                        downloadAndContinue(location, file, holder, uname, hashMap, reference)
                    }
                }
            }
        }
    }

    private fun downloadAndContinue(
        location: String,
        file: File,
        holder: ViewHolder,
        UName: String,
        hashMap: java.util.HashMap<String, Any?>,
        reference: DatabaseReference
    ) {
        FirebaseStorage.getInstance().getReference(location).getFile(file)
            .addOnSuccessListener {
                holder.bind(
                    file.absolutePath,
                    UName,
                    hashMap,
                    context,
                    activity,
                    cardStackLayoutManager,
                    posts_rv
                )
            }.addOnFailureListener {
                Log.d(
                    "texts",
                    "downloadAndContinue: " + it.cause.toString() + " " + (it.cause == null)
                )
                if (it.cause == null) {
                    reference.removeValue()
                }
                holder.bind(
                    null,
                    UName,
                    hashMap,
                    context,
                    activity,
                    cardStackLayoutManager,
                    posts_rv
                )
            }
    }


    override fun getItemCount() = posts.size
}