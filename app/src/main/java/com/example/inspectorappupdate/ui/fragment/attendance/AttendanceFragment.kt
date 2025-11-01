package com.example.inspectorappupdate.ui.fragment.attendance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.ui.fragment.student.SearchStudentFragment
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AttendanceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AttendanceFragment : Fragment() {
    // TODO: Rename and change types of parameters

    var deviceID = ""
    var userDetails = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Initialize the database module
        appDatabase = DbUtility().dbBuilder(requireContext())
        // Get the details of the users and device id
        lifecycleScope.launch {
            deviceID = appDatabase.loggedInUserDao().getLastLogin().userID

        }

        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_attendance, container, false)
        return root
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}