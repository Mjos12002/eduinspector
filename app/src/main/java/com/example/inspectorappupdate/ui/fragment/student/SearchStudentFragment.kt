package com.example.inspectorappupdate.ui.fragment.student

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentSearchStudentBinding
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import kotlinx.coroutines.launch
import java.lang.StringBuilder

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SearchStudentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchStudentFragment : Fragment() {

    // Binding to the fragment UI
    private var _binding : FragmentSearchStudentBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize the fragment
        _binding = FragmentSearchStudentBinding.inflate(inflater, container, false)

        // Initialize the local database utility
        appDatabase = DbUtility().dbBuilder(requireContext())

        // Get from the local database the first name and last name of the user logged in
        lifecycleScope.launch {
            val msgBuilder = StringBuilder()
            val firstName = appDatabase.loggedInUserDao().getLastLogin().firstName
            val lastName = appDatabase.loggedInUserDao().getLastLogin().lastName
            msgBuilder.append("Welcome ")
            msgBuilder.append(firstName)
            msgBuilder.append(" ")
            msgBuilder.append(lastName)

            binding.tvSearchStudentHeader.text = msgBuilder.toString()
        }

        binding.imvSearch.setOnClickListener {

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("Welcome", "Hello World")
    }

    companion object {
        lateinit var appDatabase: AppDatabase

    }
}