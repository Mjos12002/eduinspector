package com.example.inspectorappupdate.ui.fragment.attendance

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Date
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

    lateinit var userToken: String

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

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val dateTimeFormatter =  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentDate = LocalDateTime.now().format(dateFormatter)
        val currentTime = LocalDateTime.now().format(timeFormatter)

        studentName.clear()
        cardStatus.clear()
        cardNumber.clear()
        attendanceDate.clear()
        attendanceTime.clear()

        // Initialize the database module
        appDatabase = DbUtility().dbBuilder(requireContext())
        // Get the user auth token and device id
        lifecycleScope.launch {
            deviceID = appDatabase.loggedInUserDao().getLastLogin().userID
            userToken = appDatabase.loggedInUserDao().getLastLogin().token
        }

        // Initialization of the variables used to manage attendance type (IN / OUT)
        val rlAttendanceIn = root.findViewById<RelativeLayout>(R.id.rl_attendance_in)
        val rlAttendanceOut = root.findViewById<RelativeLayout>(R.id.rl_attendance_out)
        val tvAttendanceIn = root.findViewById<TextView>(R.id.tv_attendance_in)
        val tvAttendanceOut = root.findViewById<TextView>(R.id.tv_attendance_out)
        val btnCreateAttendance = root.findViewById<AppCompatButton>(R.id.btn_confirm_attendance)

        // Permission details
        val tvPermissionReason = root.findViewById<TextView>(R.id.tv_permission_reason)
        val tvPermissionApprovalStatus = root.findViewById<TextView>(R.id.tv_permission_approval_status)
        val tvPermissionStart = root.findViewById<TextView>(R.id.tv_permission_start)
        val tvPermissionEnd = root.findViewById<TextView>(R.id.tv_permission_ends)
        val tvPermissionUsed = root.findViewById<TextView>(R.id.tv_permission_used)
        val rlPermissionContainer = root.findViewById<RelativeLayout>(R.id.rl_permission_container)

        setAttendanceType("In", tvAttendanceIn, tvAttendanceOut, rlAttendanceIn, rlAttendanceOut, rlStudentDetails)

        // Change active / passive based on user choice
        rlAttendanceIn.setOnClickListener {
            setAttendanceType("In", tvAttendanceIn, tvAttendanceOut, rlAttendanceIn, rlAttendanceOut, rlStudentDetails)
        }

        // Change active / passive based on user choice
        rlAttendanceOut.setOnClickListener {
            setAttendanceType("Out", tvAttendanceIn, tvAttendanceOut, rlAttendanceIn, rlAttendanceOut, rlStudentDetails)

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

        // Observe the changes in the student permission response
        studentViewModel.studentPermissionLiveData.observe(viewLifecycleOwner, Observer {
            if(it.data != null) {

                // Filter the last permission
                if (it.data.permissions.isNotEmpty()) {

                    try{

                        val lastPermission = it.data.permissions.sortedWith {a, b -> a.id}

                        val permissionEndDate = LocalDateTime.parse(lastPermission[0].end_time, dateTimeFormatter)
                        val today = LocalDateTime.now()
                        val diff = Period.between(today.toLocalDate(), permissionEndDate.toLocalDate())
                        Log.i("TAG-INFORMATION", "${diff.days}")
                        if( diff.days <= 0) {
                            rlPermissionContainer.visibility = View.VISIBLE
                            tvPermissionApprovalStatus.text = "Approval: ${lastPermission[0].status}"
                            tvPermissionReason.text = "Reason: ${lastPermission[0].reason}"
                            tvPermissionStart.text = "Starts: ${lastPermission[0].start_time}"
                            tvPermissionEnd.text = "Ends: ${lastPermission[0].end_time}"
                            tvPermissionUsed.text = "Expired: ${lastPermission[0].has_exited}"
                        }

                    }catch (e: Exception) {
                        Log.i("TAG-INFORMATION", "${e.message!!}")
                    }
                }
            }
        })

        // Observe to changes in the student view model's attendance IN
        studentViewModel.classAttendanceInLiveData.observe(viewLifecycleOwner, Observer {
            pbProgressBar.visibility = View.GONE
            tvAttendanceResponse.text = it.message
            tvAttendanceResponse.visibility = View.VISIBLE
        })

        // Observe to changes in the student view model's attendance OUT
        studentViewModel.classAttendanceOutLiveData.observe(viewLifecycleOwner, Observer {
            pbProgressBar.visibility = View.GONE
            tvAttendanceResponse.text = it.message
            tvAttendanceResponse.visibility = View.VISIBLE
        })

        // Observe to changes in the card information
        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {
            // Try Or fail
            lifecycleScope.launch {
                studentViewModel.getStudentPermission(userToken, "KSLjh")
            }
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
    fun setAttendanceType(attendance: String, tvIn: TextView, tvOut: TextView, rlIn: RelativeLayout, rlOut: RelativeLayout, rlAttendanceDetails: RelativeLayout) {
        inOrOut = attendance
        rlAttendanceDetails.visibility = View.GONE
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