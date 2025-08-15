package com.example.inspectorappupdate.ui.fragment.student

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentSearchStudentBinding
import com.example.inspectorappupdate.entity.academic_term.AcademicTermEntity
import com.example.inspectorappupdate.repository.academic_term.AcademicTermRepository
import com.example.inspectorappupdate.repository.offense.OffenseRepository
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.offense.OffenseViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import kotlinx.coroutines.launch
import java.lang.Exception

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

    // Initialize the view model for the student search
    private val studentViewModel: StudentViewModel by activityViewModels()

    // Initialise the view model for the student offense
    private val offenseViewModel: OffenseViewModel by activityViewModels()

    // Variable holding the student id
    var studentID = 0

    @RequiresApi(Build.VERSION_CODES.O)
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
            // Create a string builder to create a welcome message
            val msgBuilder = StringBuilder()
            val firstName = appDatabase.loggedInUserDao().getLastLogin().firstName
            val lastName = appDatabase.loggedInUserDao().getLastLogin().lastName
            msgBuilder.append(getString(R.string.welcome))
            msgBuilder.append(", ")
            msgBuilder.append(firstName)
            msgBuilder.append(" ")
            msgBuilder.append(lastName)

            binding.tvSearchStudentHeader.text = msgBuilder.toString()
        }

        // Execute the search action to get the student details
        binding.imvSearch.setOnClickListener {
            val regNumber = binding.etSearchStudent.text.toString()
            lifecycleScope.launch {
                val token = appDatabase.loggedInUserDao().getLastLogin().token
                studentViewModel.searchStudentByRegNumber(token, regNumber)
            }
        }

        // Observe the changes in the live data object
        studentViewModel.studentLiveData.observe(viewLifecycleOwner, Observer{
            // Create the student name object to display
            val studentName = StringBuilder()
            studentName.append(it.data.first_name)
            studentName.append(" ")
            studentName.append(it.data.last_name)
            binding.tvStudentName.text = studentName.toString()
            binding.tvStudentRegNumber.text = it.data.reg_number

            // Set the student id value
            studentID = it.data.id

            getCurrentAcademicTerm()

        })

        // Observe the changes in the offenses live data
        offenseViewModel.offenseLiveData.observe(viewLifecycleOwner, Observer{
            val lastOffense = it.data.last()
            binding.tvLastOffense.text = "Last offense: " + lastOffense.offense_type.type_name
            binding.tvOffenseTerm.text = lastOffense.term.term_name
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("Welcome", "Hello World")
    }

    companion object {
        lateinit var appDatabase: AppDatabase

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentAcademicTerm(){
        lifecycleScope.launch {
            try{
                val token = appDatabase.loggedInUserDao().getLastLogin().token
                val academicModel = AcademicTermRepository().getAcademicTerm(token)
                val academicEntity = AcademicTermEntity(0, academicModel.data.id, academicModel.data.academic_year_id, academicModel.data.term_name, academicModel.data.start_date, academicModel.data.end_date)
                appDatabase.academicTermDao().insert(academicEntity)

                // Get the student discipline
//                val offenseModel = OffenseRepository().getStudentOffense(token, academicModel.data.id, academicModel.data.academic_year_id)
//                Log.i("ACADEMIC-TERM", "$academicModel")
//                Log.i("ACADEMIC-TERM", "$offenseModel")
                Log.i("ACADEMIC-TERM", "$studentID -- ${academicModel.data.id}")
                offenseViewModel.getStudentOffense(token, studentID, academicModel.data.id)

            }catch (e: Exception){
                Log.i("ACADEMIC-TERM", "${e.message}")
            }

        }

    }
}