package com.example.fintelis

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fintelis.databinding.ActivityDashboardBinding
import com.example.fintelis.viewmodel.TransactionViewModel

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val transactionViewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup the BottomNavigationView with the NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Add a destination changed listener to manually update the selection
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = binding.bottomNavigation.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                if (item.itemId == destination.id) {
                    item.isChecked = true
                    break
                }
            }
        }
    }
}