package com.example.inspectorappupdate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.inspectorappupdate.databinding.ActivityIndexBinding
import com.example.inspectorappupdate.utils.Session
import kotlinx.coroutines.launch

class IndexActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityIndexBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A stored session goes straight to the home screen; sign-in shows only without one.
        lifecycleScope.launch {
            val session = Session.current(this@IndexActivity)
            if (session != null) {
                Session.openHome(this@IndexActivity, session.role)
                return@launch
            }
            binding = ActivityIndexBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val navController = findNavController(R.id.nav_host_fragment_content_index)
            appBarConfiguration = AppBarConfiguration(navController.graph)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_index)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

}