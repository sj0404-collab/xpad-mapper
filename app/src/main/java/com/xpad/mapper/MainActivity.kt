package com.xpad.mapper

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.xpad.mapper.databinding.ActivityMainBinding
import com.xpad.mapper.ui.DevicesFragment
import com.xpad.mapper.ui.HelpFragment
import com.xpad.mapper.ui.ProfilesFragment
import com.xpad.mapper.ui.TestFragment

/** Общее состояние между табами. */
object PadState {
    var selectedDeviceId: Int = -1
    var selectedDeviceName: String = "—"
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val nav = binding.bottomNav
        nav.inflateMenu(R.menu.menu_main)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_devices -> {
                    changeTab(DevicesFragment()); true
                }
                R.id.nav_test -> {
                    changeTab(TestFragment()); true
                }
                R.id.nav_profiles -> {
                    changeTab(ProfilesFragment()); true
                }
                R.id.nav_help -> {
                    changeTab(HelpFragment()); true
                }
                else -> false
            }
        }
        if (savedInstanceState == null) {
            changeTab(DevicesFragment())
        }
    }

    private fun changeTab(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}