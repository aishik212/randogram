package com.textsdev.randogram.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.textsdev.randogram.MainActivity
import com.textsdev.randogram.R
import com.textsdev.randogram.adapters.PostAdapter
import com.yuyakaido.android.cardstackview.*
import kotlinx.android.synthetic.main.home_fragment_layout.*


class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment_layout, container, false)
    }

    var shownA = false
    var shownB = false


    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postList = arrayListOf()

        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val cardStackView = view.findViewById<CardStackView>(R.id.posts_rv)
        if (cardStackView != null) {
            cardStackLayoutManager = CardStackLayoutManager(context, object : CardStackListener {
                var v: View? = null
                override fun onCardDragging(direction: Direction?, ratio: Float) {
                    val liked = Direction.Left
                    val skipped = Direction.Right
                    if (ratio < 0.05) {
                        if (liked == direction) {
                            Log.d("texts", "onCardDragging: liked")
                            if (!shownA) {
                                Toast.makeText(
                                    requireContext(),
                                    "Swipe Left to Skip",
                                    Toast.LENGTH_SHORT
                                ).show()
                                shownA = true
                            }
                        } else if (skipped == direction) {
                            Log.d("texts", "onCardDragging: skipped")
                            if (!shownB) {
                                Toast.makeText(
                                    requireContext(),
                                    "Swipe Right to Like",
                                    Toast.LENGTH_SHORT
                                ).show()
                                shownB = true
                            }
                        }
                    }
                }

                override fun onCardSwiped(direction: Direction?) {
                    if (posts_rv.childCount == 0) {
                        alterVisibilityPosts(activity, false)
                    }
                    val liked = Direction.Right
                    val tag = v?.tag as DatabaseReference
                    if (direction == liked) {
                        val child = MainActivity.getDBRef(requireContext(), "likes")
                            .child(tag.key.toString())
                        child.child(FirebaseAuth.getInstance().currentUser?.uid + "")
                            .setValue("")
                            .addOnSuccessListener {
                                child.addListenerForSingleValueEvent(
                                    object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val niceCount = snapshot.childrenCount
                                            tag.child("like").setValue(niceCount)
                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                        }
                                    })
                            }
                    }
                }

                override fun onCardRewound() {
                    Log.d("texts", "onCardRewound: ")
                }

                override fun onCardCanceled() {
                    Log.d("texts", "onCardCanceled: ")
                }

                override fun onCardAppeared(view: View?, position: Int) {
                    Log.d("texts", "onCardAppeared: ")
                }

                override fun onCardDisappeared(view: View?, position: Int) {
                    v = view
                }

            })
            cardStackLayoutManager.setCanScrollVertical(false)
            cardStackLayoutManager.setDirections(Direction.HORIZONTAL)
            cardStackLayoutManager.setScaleInterval(0.9F)
            cardStackLayoutManager.setTranslationInterval(8.0f)
            cardStackLayoutManager.setVisibleCount(3)
            cardStackLayoutManager.setStackFrom(StackFrom.Right)
            cardStackView.layoutManager = cardStackLayoutManager
            postAdapter = PostAdapter(
                postList,
                requireContext(),
                requireActivity(),
                cardStackLayoutManager,
                posts_rv
            )
            cardStackView.adapter = postAdapter
        }
        fetchData(requireContext(), requireActivity())
        val swipeLayout = activity?.findViewById<SwipeRefreshLayout>(R.id.swipe)

        swipeLayout?.setOnRefreshListener {
            fetchData(requireContext(), requireActivity())
        }
    }

    companion object {
        lateinit var postList: ArrayList<HashMap<String, Any>>

        fun swipeLike(cardStackLayoutManager: CardStackLayoutManager, posts_rv: CardStackView) {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            cardStackLayoutManager.setSwipeAnimationSetting(setting)
            posts_rv.swipe()
        }

        fun swipeSkip(cardStackLayoutManager: CardStackLayoutManager, posts_rv: CardStackView) {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            cardStackLayoutManager.setSwipeAnimationSetting(setting)
            posts_rv.swipe()
        }

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
                        hmap["reference"] = it.ref
                        if (hmap.keys.size >= 4) {
                            postList.add(hmap)
                        }
                    }
                    postList.reverse()
                    checkPostAndShow(activity)
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }

        private fun checkPostAndShow(activity: Activity) {
            try {
                val swipeLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swipe)
                swipeLayout.isRefreshing = false
                if (postList.size > 0) {
                    alterVisibilityPosts(activity, true)
                    postAdapter?.notifyDataSetChanged()
                } else {
                    alterVisibilityPosts(activity, false)
                }
            } catch (e: Exception) {
                Log.d("texts", "checkPostAndShow: " + e.localizedMessage)
            }
        }

        private fun alterVisibilityPosts(
            activity: Activity?,
            b: Boolean
        ) {
            if (activity != null) {
                val noPostTV = activity.findViewById<TextView>(R.id.no_post_tv)
                val postRV = activity.findViewById<RecyclerView>(R.id.posts_rv)
                if (b) {
                    noPostTV.visibility = View.GONE
                    postRV.visibility = View.VISIBLE
                } else {
                    noPostTV.visibility = View.VISIBLE
                    postRV.visibility = View.GONE
                }
            }
        }

    }
}