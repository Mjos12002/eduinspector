package com.example.inspectorappupdate.ui.fragment.signin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.SearchStudentActivity
import com.example.inspectorappupdate.databinding.FragmentSigninBinding
import com.example.inspectorappupdate.entity.user.LoggedInUserEntity
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.AppVersionViewer
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.signin.SignInViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A simple [Fragment] subclass.
 * Use the [SignInFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignInFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private var _binding: FragmentSigninBinding? = null

    // Initialize the sign in view model
    private val signInViewModel: SignInViewModel by activityViewModels()

    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentSigninBinding.inflate(inflater, container, false)

        // Initialization of the SQLlite
       appDatabase = DbUtility().dbBuilder(requireContext())

        // Get the app version
        val appVersion = AppVersionViewer.process(requireContext())
        binding.tvAppVersion.text = appVersion

        binding.btnsignin.setOnClickListener {

            // Getting the username and password
            try{
                signInViewModel.updateLoginStatusMessage("Processing wait ...")
                val strUsername = binding.etLoginUsername.text.toString()
                val strPassword = binding.etLoginPassword.text.toString()

                lifecycleScope.launch {
                    signInViewModel.userLogin(strUsername, strPassword)
                }
            }catch (e: Exception) {
                binding.tvprocessing.text = getString(R.string.unknown_error)
            }
        }

        // Observe the changes in the login status message
        signInViewModel.loginStatusLiveData.observe(viewLifecycleOwner, Observer {
            binding.tvprocessing.text = it
        })

        // Observe the changes in the credentials status live data
        signInViewModel.credentialStatusLiveData.observe(viewLifecycleOwner, Observer {

            // Run this if there is an error, ie. the username or password is does not conform to the rules
            binding.tvPasswordError.visibility = View.GONE
            binding.tvUsernameError.visibility = View.GONE
            if (it.error) {
                // Hide the message display by default
                binding.tvPasswordError.text = it.passwordMessage
                binding.tvUsernameError.text = it.usernameMessage
                // Show the password error if the field contains the message
                if (it.passwordMessage != "") {
                    binding.tvPasswordError.visibility = View.VISIBLE
                }
                // Show the username error if the field contains the message
                if (it.usernameMessage != "") {
                    binding.tvUsernameError.visibility = View.VISIBLE
                }
            }
        })

        // Observe the changes in the login response
        signInViewModel.loginResponseLiveData.observe(viewLifecycleOwner, Observer{
            try {
                // Checking the response, if the response is ok then add the info to the local database
                Log.i("TAG-INFORMATION", "$it")
                if (it.status.toInt() == 200) {
                    // Getting the today's date
                    val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern(getString(R.string.date_pattern)))
                    signInViewModel.updateLoginStatusMessage("")
                    val userLoggedIn = LoggedInUserEntity(0, it.status, it.error, it.message, it.data.first_name, it.data.last_name, it.data.contact_number, it.data.reg_number, it.data.email, it.data.token, it.data.user_id, it.data.role, today)

                    lifecycleScope.launch {
                        appDatabase.loggedInUserDao().insert(userLoggedIn)
                    }
                    startActivity(Intent(context, SearchStudentActivity::class.java))
                }else {
                    signInViewModel.updateLoginStatusMessage("Error, Contact Admin")
                }
            }catch (e: Exception) {
                Log.i("TAG-INFORMATION", "${e.message}")
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }

        })

        return binding.root
    }


    companion object {
        lateinit var appDatabase: AppDatabase
    }
}