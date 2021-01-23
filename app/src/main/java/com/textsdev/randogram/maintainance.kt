package com.textsdev.randogram

import android.content.Context
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class maintainance {
    companion object {
        const val MAXPOSTS = 20

        fun removeOldImages() {
            val s = "com_textsdev_randogram/" + (Firebase.auth.currentUser?.uid)
            Firebase.storage.reference.child(s)
                .listAll()
                .addOnSuccessListener { listResult ->
                    val items = listResult.items
                    if (items.size > MAXPOSTS) {
                        items.iterator().forEach { storageReference ->
                            storageReference.metadata.addOnSuccessListener {
                                val toLong = it.creationTimeMillis
                                val l = (System.currentTimeMillis() - toLong) / 1000
                                val i = 60 * 60 * 24 * 5
                                Log.d("texts", "removeOldImages: $l $i")
                                if (l > i) {
                                    val reference = it.reference
                                    reference?.delete()?.addOnCompleteListener {
                                        Log.d("texts", "removeOldImages: $reference Deleted")
                                    }
                                }
                            }
                        }
                    }
                }
        }

        fun removeOldDB(context: Context) {
            val dbRef = MainActivity.getDBRef(context, "posts")
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null && snapshot.childrenCount > MAXPOSTS) {
                        dbRef.limitToFirst((snapshot.childrenCount - MAXPOSTS).toInt())
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.value != null) {
                                        snapshot.children.forEach {
                                            val value = it.child("time").value
                                            if (value != null) {
                                                val toLong = value.toString().toLongOrNull()
                                                if (toLong != null) {
                                                    val l = System.currentTimeMillis() - toLong
                                                    if (l > 60 * 60 * 24) {
                                                        val location = it.child("location").value
                                                        if (location != null) {
                                                            deleteFromStorage(location, it.ref)
                                                        } else {
                                                            deleteRef(it.ref)
                                                        }
                                                    }
                                                }
                                            } else {
                                                deleteRef(it.ref)
                                            }
                                        }
                                    }

                                }

                                override fun onCancelled(error: DatabaseError) {

                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }

        fun removeLikes(context: Context) {
            MainActivity.getDBRef(context, "likes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value != null) {
                            snapshot.children.forEach { likeSS ->
                                MainActivity.getDBRef(context, "posts").child(likeSS.key.toString())
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (snapshot.value == null) {
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                        }
                                    })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }

        private fun deleteRef(ref: DatabaseReference) {
            ref.removeValue().addOnSuccessListener {
                Log.d("texts", "deleteRef: DELETED $ref")
            }
        }

        fun deleteFromStorage(location: Any, ref: DatabaseReference) {
            FirebaseStorage.getInstance()
                .getReference(location.toString()).delete().addOnSuccessListener {
                    Log.d("texts", "deleteFromStorage: DELETED $location")
                    deleteRef(ref)
                }
        }
    }
}