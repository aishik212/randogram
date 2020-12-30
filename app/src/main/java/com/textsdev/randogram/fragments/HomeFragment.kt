package com.textsdev.randogram.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.textsdev.randogram.MainActivity
import com.textsdev.randogram.R
import com.textsdev.randogram.adapters.PostAdapter
import kotlinx.android.synthetic.main.home_fragment_layout.*


class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment_layout, container, false)
    }


    override fun onStart() {
        super.onStart()
        postList = arrayListOf()
        postAdapter = PostAdapter(postList, requireContext())
        val linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        posts_rv.layoutManager = linearLayoutManager
        posts_rv.adapter = postAdapter
        fetchData(requireContext(), requireActivity())
    }

    companion object {
        lateinit var postList: ArrayList<HashMap<String, Any>>

        var postAdapter: PostAdapter? = null
        fun fetchData(context: Context, activity: Activity) {
            val postRef = MainActivity.getDBRef(context, "posts")
            postRef.addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postList.clear()
                    snapshot.children.forEach {
                        val location = it.child("location").value.toString()
                        val uid = it.child("uid").value.toString()
                        val likes = it.child("like").value.toString()
                        val time = it.child("time").value.toString()
                        val hmap: HashMap<String, Any> = hashMapOf()
                        hmap["location"] = location
                        hmap["uid"] = uid
                        hmap["likes"] = likes
                        hmap["time"] = time
                        postList.add(hmap)
                    }
                    val noPostTV = activity.findViewById<TextView>(R.id.no_post_tv)
                    val postRV = activity.findViewById<RecyclerView>(R.id.posts_rv)
                    if (postList.size > 0) {
                        noPostTV?.visibility = View.GONE
                        postRV?.visibility = View.VISIBLE
                        postAdapter?.notifyDataSetChanged()
                    } else {
                        noPostTV?.visibility = View.VISIBLE
                        postRV?.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }

    }
}