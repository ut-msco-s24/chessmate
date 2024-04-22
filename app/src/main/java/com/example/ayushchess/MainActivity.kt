package com.example.ayushchess

import MainViewModel
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.ayushchess.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var authUser : AuthUser

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the toolbar as the ActionBar
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Optionally, remove title from ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        // Navigate up with the navigation controller
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        // Create authentication object.  This will log the user in if needed
        authUser = AuthUser(activityResultRegistry)
        // authUser needs to observe our lifecycle so it can run login activity
        lifecycle.addObserver(authUser)

        authUser.observeUser().observe(this) { user ->
            viewModel.setCurrentUser(user)
        }
    }
}
