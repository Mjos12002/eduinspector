package com.example.inspectorappupdate.ui.fragment.attendance

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.Visibility
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.repository.student.StudentRepository
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.getValue

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
    var inOrOut = ""
    var strCardNumber = ""
    lateinit var studentName: StringBuilder
    lateinit var cardNumber: StringBuilder
    lateinit var cardStatus: StringBuilder
    lateinit var attendanceDate: StringBuilder
    lateinit var attendanceTime: StringBuilder

    private val cardViewModel: CardViewModel by activityViewModels()
    private val studentViewModel: StudentViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_attendance, container, false)

        // Initialize the container view of student information & attendance
        val rlStudentDetails = root.findViewById<RelativeLayout>(R.id.rl_attendance_details)

        // Initialize the progress bar
        val pbProgressBar = root.findViewById<ProgressBar>(R.id.pb_processing)

        // Initialize the view to show the api result
        val tvAttendanceResponse = root.findViewById<TextView>(R.id.tv_attendance_response)

        rlStudentDetails.visibility = View.GONE

        //Initialize and clear the string builder
        studentName = StringBuilder()
        cardStatus = StringBuilder()
        cardNumber = StringBuilder()
        attendanceDate = StringBuilder()
        attendanceTime = StringBuilder()

        studentName.clear()
        cardStatus.clear()
        cardNumber.clear()
        attendanceDate.clear()
        attendanceTime.clear()

        // Initialize the database module
        appDatabase = DbUtility().dbBuilder(requireContext())
        // Get the details of the users and device id
        lifecycleScope.launch {
            deviceID = appDatabase.loggedInUserDao().getLastLogin().userID
        }

        // Initialization of the variables used to manage attendance type (IN / OUT)
        val rlAttendanceIn = root.findViewById<RelativeLayout>(R.id.rl_attendance_in)
        val rlAttendanceOut = root.findViewById<RelativeLayout>(R.id.rl_attendance_out)
        val tvAttendanceIn = root.findViewById<TextView>(R.id.tv_attendance_in)
        val tvAttendanceOut = root.findViewById<TextView>(R.id.tv_attendance_out)
        val btnCreateAttendance = root.findViewById<AppCompatButton>(R.id.btn_confirm_attendance)

        setAttendanceType("In", tvAttendanceIn, tvAttendanceOut, rlAttendanceIn, rlAttendanceOut)

        // Change active / passive based on user choice
        rlAttendanceIn.setOnClickListener {
            setAttendanceType("In", tvAttendanceIn, tvAttendanceOut, rlAttendanceIn, rlAttendanceOut)
        }

        // Change active / passive based on user choice
        rlAttendanceOut.setOnClickListener {
            setAttendanceType("Out", tvAttendanceIn, tvAttendanceOut, rlAttendanceIn, rlAttendanceOut)

        }

        // Click event to send the attendance data
        btnCreateAttendance.setOnClickListener {
            try{
                pbProgressBar.visibility = View.VISIBLE
                tvAttendanceResponse.text = ""
                if(inOrOut == "In") {
                    lifecycleScope.launch {
                        studentViewModel.createSchoolAttendanceIn(appDatabase.loggedInUserDao().getLastLogin().token, strCardNumber, 17)
                    }
                }else {
                    lifecycleScope.launch {
                        studentViewModel.createSchoolAttendanceOut(appDatabase.loggedInUserDao().getLastLogin().token, strCardNumber, 17)
                    }
                }

            }catch (e: Exception) {
                pbProgressBar.visibility = View.GONE
                tvAttendanceResponse.text = getString(R.string.unknown_error)
                tvAttendanceResponse.visibility = View.VISIBLE
            }

        }

        // Listen to changes in the student view model's attendance IN
        studentViewModel.classAttendanceInLiveData.observe(viewLifecycleOwner, Observer {
            pbProgressBar.visibility = View.GONE
            tvAttendanceResponse.text = it.message
            tvAttendanceResponse.visibility = View.VISIBLE
        })

        // Listen to changes in the student view model's attendance OUT
        studentViewModel.classAttendanceOutLiveData.observe(viewLifecycleOwner, Observer {
            pbProgressBar.visibility = View.GONE
            tvAttendanceResponse.text = it.message
            tvAttendanceResponse.visibility = View.VISIBLE
        })

        // Listen to changes in the card information
        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {
            // Try and fail
            try{
                tvAttendanceResponse.visibility = View.GONE
                tvAttendanceResponse.text = ""
                studentName.clear()
                cardStatus.clear()
                cardNumber.clear()
                attendanceDate.clear()
                attendanceTime.clear()
                if (it.data != null) {
                    // Get the time and date formatted
                    strCardNumber = it.data.card_number
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val currentDate = LocalDateTime.now().format(dateFormatter)
                    val currentTime = LocalDateTime.now().format(timeFormatter)

                    studentName.append(it.data.first_name)
                    studentName.append(" ")
                    studentName.append(it.data.last_name)

                    // append the card status details
                    cardStatus.append("Card status: ")
                    cardStatus.append(it.data.card_status)

                    // append the card number details
                    cardNumber.append("Card no: ")
                    cardNumber.append(it.data.card_number)

                    // append the attendance date
                    attendanceDate.append("Date: ")
                    attendanceDate.append(currentDate.toString())

                    // append the attendance time
                    attendanceTime.append("Time: ")
                    attendanceTime.append(currentTime.toString())

                    root.findViewById<TextView>(R.id.tv_student_name).text = studentName.toString()
                    root.findViewById<TextView>(R.id.tv_card_number).text = cardNumber.toString()
                    root.findViewById<TextView>(R.id.tv_card_status).text = cardStatus.toString()

                    // Display the time and date
                    root.findViewById<TextView>(R.id.tv_attendance_time).text = attendanceTime.toString()
                    root.findViewById<TextView>(R.id.tv_attendance_date).text = attendanceDate.toString()
                    rlStudentDetails.visibility = View.VISIBLE

                }else {
                    rlStudentDetails.visibility = View.GONE
                    tvAttendanceResponse.visibility = View.VISIBLE
                    tvAttendanceResponse.text = getString(R.string.unregistered)
                }

            }catch (e: Exception) {
                tvAttendanceResponse.visibility = View.VISIBLE
                tvAttendanceResponse.text = getString(R.string.unregistered)
            }

        })

        return root
    }

    //setAttendanceType is used to change the attendance type
    fun setAttendanceType(attendance: String, tvIn: TextView, tvOut: TextView, rlIn: RelativeLayout, rlOut: RelativeLayout) {
        inOrOut = attendance
        if(attendance == "In") {
            tvIn.setTextColor(resources.getColor(R.color.white))
            rlIn.setBackgroundDrawable(resources.getDrawable(R.drawable.active_attendance))
            tvOut.setTextColor(resources.getColor(R.color.black))
            rlOut.setBackgroundDrawable(resources.getDrawable(R.drawable.inactive_attendance))
        }else {
            tvOut.setTextColor(resources.getColor(R.color.white))
            rlOut.setBackgroundDrawable(resources.getDrawable(R.drawable.active_attendance))
            tvIn.setTextColor(resources.getColor(R.color.black))
            rlIn.setBackgroundDrawable(resources.getDrawable(R.drawable.inactive_attendance))
        }
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}