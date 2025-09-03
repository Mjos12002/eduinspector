package com.example.inspectorappupdate.ui.fragment.student

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentSearchStudentBinding
import com.example.inspectorappupdate.entity.academic_term.AcademicTermEntity
import com.example.inspectorappupdate.model.offense_type.OffenseTypeData
import com.example.inspectorappupdate.repository.academic_term.AcademicTermRepository
import com.example.inspectorappupdate.repository.offense.OffenseRepository
import com.example.inspectorappupdate.repository.offense_type.OffenseTypeRepository
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.offense.OffenseViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SearchStudentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchStudentFragment : Fragment(), AdapterView.OnItemSelectedListener {

    // Binding to the fragment UI
    private var _binding : FragmentSearchStudentBinding? = null
    private val binding get() = _binding!!

    // Initialize the view model for the student search
    private val studentViewModel: StudentViewModel by activityViewModels()

    // Initialize the view model for the card details
    private val cardViewModel: CardViewModel by activityViewModels()

    // Initialise the view model for the student offense
    private val offenseViewModel: OffenseViewModel by activityViewModels()

    // Variable holding the student id
    var studentID = 0
    var academicYearID = 0
    var academicTermID = 0

    var userToken = ""

    // Offense Typese List
    lateinit var offenseTypeList : List<OffenseTypeData>

    var offenseTypeID = 0

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
            userToken = appDatabase.loggedInUserDao().getLastLogin().token
            msgBuilder.append(getString(R.string.welcome))
            msgBuilder.append(", ")
            msgBuilder.append(firstName)
            msgBuilder.append(" ")
            msgBuilder.append(lastName)
            binding.rlStudentDetails.visibility = View.VISIBLE
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
            binding.rlDeductionsParent.visibility = View.VISIBLE
            getCurrentAcademicTerm()

        })

        // Observe the changes in the offenses live data
        offenseViewModel.offenseLiveData.observe(viewLifecycleOwner, Observer{
            try{
                if(it.data.isNotEmpty()) {
                    val lastOffense = it.data.last()
                    binding.tvLastOffense.text = "Last offense: "  + lastOffense.offense_type.type_name
                    val counter = it.data.sumOf { it.marks_deducted }
                    binding.tvTotalDeductionValue.text = "$counter"
                }else {
                    binding.tvTotalDeductionValue.text = "0"
                    binding.tvLastOffense.text = ""
                }

            }catch (e: Exception) {

            }

        })

        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {
            Log.i("INSPECTOR-LOG", "OBSERVING $it")
        })

        // Add event to add deduction
        binding.imgAddDeduction.setOnClickListener {
            showViewToDeductMarks(requireContext())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadOffense(): Array<String>{

        lifecycleScope.launch {
            val token = appDatabase.loggedInUserDao().getLastLogin().token
            val offenseType = OffenseTypeRepository().getOffenseType(token)

        }
        return arrayOf("")

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showViewToDeductMarks(context: Context) {

        val dialog = BottomSheetDialog(context)

        val view = layoutInflater.inflate(R.layout.discipline_deduction, null)
        val spin = view.findViewById<Spinner>(R.id.spn_offense)
        loadOffense()

        lifecycleScope.launch {
            val token = appDatabase.loggedInUserDao().getLastLogin().token
            val offenseType = OffenseTypeRepository().getOffenseType(token)
            val arr = offenseType.data.map { it.type_name }
            // Set the offense type
            offenseTypeList = offenseType.data

            val ad = ArrayAdapter(context,
                android.R.layout.simple_spinner_item, arr
            )

            ad.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )

            spin.adapter = ad

            // set simple layout resource file
            // for each item of spinner
            ad.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
            ad.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spin.onItemSelectedListener = this as AdapterView.OnItemSelectedListener?

        val x = view.findViewById<AppCompatButton>(R.id.btn_add_offense)
        x.setOnClickListener {
            lifecycleScope.launch {
                val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val resp = OffenseRepository().createStudentOffense(userToken, studentID, offenseTypeID, academicYearID, academicTermID, date, 1)
                dialog.dismiss()
            }

        }
        dialog.setCancelable(true)
        // set content view to our view.
        dialog.setContentView(view)
        // call a show method to display a dialog
        dialog.show()

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
                academicTermID = academicEntity.termID
                academicYearID = academicEntity.yearID
                offenseViewModel.getStudentOffense(token, studentID, academicModel.data.id)

            }catch (e: Exception){
                Log.i("ACADEMIC-TERM", "${e.message}")
            }

        }

    }

    override fun onItemSelected(
        p0: AdapterView<*>?,
        p1: View?,
        p2: Int,
        p3: Long
    ) {
        offenseTypeID = offenseTypeList[p2].id
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}