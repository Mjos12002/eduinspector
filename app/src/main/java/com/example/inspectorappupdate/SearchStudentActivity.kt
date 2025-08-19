package com.example.inspectorappupdate

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.inspectorappupdate.databinding.ActivitySearchStudentBinding

class SearchStudentActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySearchStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_content_search_student)
        appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.rlEditPin.setOnClickListener {
            navController.navigate(R.id.ChangePinFragment)
        }

        binding.rlCardTopup.setOnClickListener {
            navController.navigate(R.id.CardTopUpFragment)
        }

        binding.rlReportLost.setOnClickListener {
            navController.navigate(R.id.ReportCardFragment)
        }

        binding.rlDisplineDeduction.setOnClickListener {
            navController.navigate(R.id.SearchStudentFragment)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_search_student)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}