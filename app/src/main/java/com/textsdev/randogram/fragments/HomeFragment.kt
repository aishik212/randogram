package com.textsdev.randogram.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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
        showInstructionsIfNotShown()
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
                    Log.d("texts", "onCardSwiped: " + direction)
                    if (posts_rv.childCount == 0) {
                        alterVisibilityPosts(activity, false)
                    }
                    val liked = Direction.Right
                    val tag1 = v?.tag
                    if (tag1 != null) {
                        val tag = tag1 as DatabaseReference
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
                                                Log.d("texts", "onDataChange: $snapshot")
                                            }

                                            override fun onCancelled(error: DatabaseError) {

                                            }
                                        })
                                }
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
                    if (view != null) {
                        val postimv = view.findViewById<ImageView>(R.id.post_imv)
                        if (postimv != null) {
                            postimv.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCardDisappeared(view: View?, position: Int) {
                    if (view != null) {
                        val postimv = view.findViewById<ImageView>(R.id.post_imv)
                        if (postimv != null) {
                            postimv.visibility = View.GONE
                        }
                    }
                    v = view
                }

            })
            cardStackLayoutManager.setCanScrollVertical(false)
            cardStackLayoutManager.setDirections(Direction.HORIZONTAL)
            cardStackLayoutManager.setVisibleCount(3)
            cardStackLayoutManager.setMaxDegree(0F)
            cardStackLayoutManager.setTranslationInterval(12F)
            cardStackLayoutManager.setSwipeThreshold(0.15F)
            cardStackLayoutManager.setOverlayInterpolator(AccelerateInterpolator())
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

    private fun showInstructionsIfNotShown() {
        val sharedPreferences = activity?.getSharedPreferences("app_data", 0)
        if (sharedPreferences != null) {
            val instructionShown: Boolean = sharedPreferences.getBoolean("instruction", false)
            val instructionsCL =
                requireActivity().findViewById<ConstraintLayout>(R.id.instructionsCL)
            val ok = requireActivity().findViewById<Button>(R.id.ok)
            if (!instructionShown) {
                instructionsCL.visibility = View.VISIBLE
            } else {
                instructionsCL.visibility = View.GONE
            }
            ok.setOnClickListener {
                instructionsCL.visibility = View.GONE
                sharedPreferences.edit().putBoolean("instruction", true).apply()
            }
        }
    }

    companion object {
        lateinit var postList: ArrayList<HashMap<String, Any>?>

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
                            if (postList.size % 5 == 0) {
                                postList.add(null)
                                postList.add(hmap)
                            } else {
                                postList.add(hmap)
                            }
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