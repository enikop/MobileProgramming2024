package com.example.budgettracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.fragments.HomeFragment
import com.example.budgettracker.fragments.StatsFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        readApiKey()

        // Set up the initial fragment (HomeFragment)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_stats -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, StatsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
    private fun readApiKey() {
        try {
            val inputStream = resources.openRawResource(R.raw.key)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val output = bufferedReader.use { it.readText().trim() }
            API_KEY = output
        } catch (e:Exception) {
            API_KEY = "N/A"
        }
    }
    companion object {
        const val KEY_ITEM_TO_EDIT = "KEY_ITEM_TO_EDIT"
        var API_KEY = ""
    }
}