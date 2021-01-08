package com.textsdev.randogram.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.textsdev.randogram.fragments.HomeFragment
import com.textsdev.randogram.fragments.UploadImageFragment


class ViewPagerAdapter(activity: AppCompatActivity, private val itemsCount : Int) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return itemsCount
    }

    override fun createFragment(position: Int): Fragment {
        return if(position == 1) {
            UploadImageFragment()
        }else {
            HomeFragment()
        }
    }
}