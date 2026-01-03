package com.example.inspectorappupdate.ui.fragment.attendance

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.adapter.SchoolClassAdapter
import com.example.inspectorappupdate.adapter.SchoolPresentOptionAdapter
import com.example.inspectorappupdate.dto.student.StudentAttendanceDTO
import com.example.inspectorappupdate.enums.AttendanceCategoryEnum
import com.example.inspectorappupdate.model.attendance.AttendanceOptionModel
import com.example.inspectorappupdate.model.schoolclass.SchoolClassModel
import com.example.inspectorappupdate.model.studentpermission.Permission
import com.example.inspectorappupdate.model.studentpermission.StudentPermissionResponse
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.getValue
import kotlin.reflect.typeOf

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AttendanceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AttendanceFragment : Fragment(), AdapterView.OnItemSelectedListener {
    // TODO: Rename and change types of parameters

    // Variable for the device ID
    private var deviceID = ""
    // Variable ro the action taken (In or Out)
    private var inOrOut = ""
    private var strStudentRegNumber = ""
    // Variable for the card number
    private var strCardNumber = ""
    // Variable for the student name (Showing on the UI)
    private lateinit var studentName: StringBuilder
    // Variable for the card number (Showing on the UI)
    private lateinit var cardNumber: StringBuilder
    // Variable for the card status (Showing on the UI)
    private lateinit var cardStatus: StringBuilder
    // Variable for the attendance date (Showing on the UI)
    private lateinit var attendanceDate: StringBuilder
    // Variable for the attendance time (Showing on the UI)
    private lateinit var attendanceTime: StringBuilder
    // Variable for the auth token
    private lateinit var userToken: String
    // Variable to check if the user has permission
    private var studentHasPermission = false
    // Variable to hold the card view model
    private val cardViewModel: CardViewModel by activityViewModels()
    // Variable to hold the student view model
    private val studentViewModel: StudentViewModel by activityViewModels()
    // Variable for the permission ID
    private var permissionID = 0
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

        studentName.clear()
        cardStatus.clear()
        cardNumber.clear()
        attendanceDate.clear()
        attendanceTime.clear()

        // Initialize the database module
        appDatabase = DbUtility().dbBuilder(requireContext())
        // Initialize the view to show the user logged in
        val tvShowLoggedInUser = root.findViewById<TextView>(R.id.tv_search_student_header)
        // Get the user auth token and device id
        lifecycleScope.launch {
            deviceID = appDatabase.loggedInUserDao().getLastLogin().userID
            userToken = appDatabase.loggedInUserDao().getLastLogin().token
            val loggedInUser: StringBuilder = StringBuilder()
            loggedInUser.append("Welcome, ")
            loggedInUser.append(appDatabase.loggedInUserDao().getLastLogin().lastName)
            loggedInUser.append(" ")
            loggedInUser.append(appDatabase.loggedInUserDao().getLastLogin().firstName)
            tvShowLoggedInUser.text = loggedInUser.toString()
        }

        // Initialize the school classname and attendance option model
        val schoolClassNameModel = listOf<SchoolClassModel>(
            SchoolClassModel("Choose class name"),
            SchoolClassModel("S1 A"),
            SchoolClassModel("S1 B"),
            SchoolClassModel("S1 C"),
            SchoolClassModel("S2 A"),
            SchoolClassModel("S2 B"),
            SchoolClassModel("S2 C"),
            SchoolClassModel("S3 A"),
            SchoolClassModel("S3 B"),
        )

        val attendanceModel = listOf<AttendanceOptionModel>(
            AttendanceOptionModel(AttendanceCategoryEnum.ABSENT.value),
            AttendanceOptionModel(AttendanceCategoryEnum.PRESENT.value)
        )

        // Initialize school class adapter and school presence option adapter
        val schoolClassAdapter = SchoolClassAdapter(requireContext(), schoolClassNameModel)
        val schoolPresenceOptionAdapter = SchoolPresentOptionAdapter(requireContext(), attendanceModel)

        // Initialization of the variables used to manage attendance type (IN / OUT)
        val rlAttendanceIn = root.findViewById<RelativeLayout>(R.id.rl_attendance_in)
        val rlAttendanceOut = root.findViewById<RelativeLayout>(R.id.rl_attendance_out)
        val tvAttendanceIn = root.findViewById<TextView>(R.id.tv_attendance_in)
        val tvAttendanceOut = root.findViewById<TextView>(R.id.tv_attendance_out)
        val btnCreateAttendance = root.findViewById<AppCompatButton>(R.id.btn_confirm_attendance)
        val tvGateChoice = root.findViewById<TextView>(R.id.tv_gate_choice)
        val tvClassRoomChoice = root.findViewById<TextView>(R.id.tv_classroom_choice)
        val rlGateAttendance = root.findViewById<RelativeLayout>(R.id.rl_attendance_gate)
        val rlClassAttendance = root.findViewById<RelativeLayout>(R.id.rl_attendance_classroom)
        val spAttendanceStatus = root.findViewById<Spinner>(R.id.sp_status)
        val spClassName = root.findViewById<Spinner>(R.id.sp_classname)
        val tvClassAttendanceDate = root.findViewById<TextView>(R.id.tv_classroom_attendance_date)
        val btnCreateSchoolAttendance = root.findViewById<AppCompatButton>(R.id.btn_confirm_classroom_attendance)
        val pbSchoolAttendance = root.findViewById<ProgressBar>(R.id.pb_classroom_attendance)
        val etAttendanceNote = root.findViewById<EditText>(R.id.et_attendance_note)

        // Permission details
        val tvPermissionReason = root.findViewById<TextView>(R.id.tv_permission_reason)
        val tvPermissionApprovalStatus = root.findViewById<TextView>(R.id.tv_permission_approval_status)
        val tvPermissionStart = root.findViewById<TextView>(R.id.tv_permission_start)
        val tvPermissionEnd = root.findViewById<TextView>(R.id.tv_permission_ends)
        val tvPermissionUsed = root.findViewById<TextView>(R.id.tv_permission_used)
        val rlPermissionContainer = root.findViewById<RelativeLayout>(R.id.rl_permission_container)

        // Set the adapter of the spinners
        spAttendanceStatus.adapter = schoolPresenceOptionAdapter
        spClassName.adapter = schoolClassAdapter

        spAttendanceStatus.onItemSelectedListener = this
        spClassName.onItemSelectedListener = this

        // Click event for the class room attendance
        btnCreateSchoolAttendance.setOnClickListener {
            try{
                pbSchoolAttendance.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val txtAttendanceNote = etAttendanceNote.text.toString()
                    studentViewModel.createStudentClassRoomAttendance(userToken, StudentAttendanceDTO(strStudentRegNumber, "", "", "", 2, txtAttendanceNote))

                }

            }catch (e: Exception) {

            }
        }

        // Click event on the gate choice, change background (active/inactive) and show / hide class attendance or gate attendance
        tvGateChoice.setOnClickListener {
            it.background = ContextCompat.getDrawable(requireContext(), R.drawable.active_gate_or_classroom)
            tvClassRoomChoice.background = ContextCompat.getDrawable(requireContext(), R.drawable.inactive_gate_or_classroom)
            rlGateAttendance.visibility = View.VISIBLE
            rlClassAttendance.visibility = View.GONE
        }

        // Click event on the classroom choice, change background (active/inactive) and show / hide class attendance or gate attendance
        tvClassRoomChoice.setOnClickListener {
            tvGateChoice.background = ContextCompat.getDrawable(requireContext(), R.drawable.inactive_gate_or_classroom)
            it.background = ContextCompat.getDrawable(requireContext(), R.drawable.active_gate_or_classroom)
            rlClassAttendance.visibility = View.VISIBLE
            rlGateAttendance.visibility = View.GONE
            tvClassAttendanceDate.text = "${LocalDateTime.now().format(dateFormatter).toString()} ${LocalDateTime.now().format(timeFormatter).toString()}"
        }

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
            Log.i("TAG-INFORMATION", "$it")
            if(it.data != null) {

                // Check if the permission is not empty (List of last 5 permission)
                if (it.data.permissions.isNotEmpty()) {

                    try{
                        // If there is a valid permission (permission's end date is in past or today)
                        if (inOrOut == "Out") {
                            // Checking the last student permission
                            val lastStudentPermission = checkLastStudentPermission(it)
                            val permissionEndDateDiff = checkDateDifference(lastStudentPermission!!, dateTimeFormatter, "end")
                            val permissionStartDateDiff = checkDateDifference(lastStudentPermission, dateTimeFormatter, "start")

                            if (permissionEndDateDiff > 0 && lastStudentPermission.has_exited == "false") {
                                permissionID = lastStudentPermission.id
                                studentHasPermission = true
                                rlPermissionContainer.visibility = View.VISIBLE
                                tvPermissionApprovalStatus.text = "Approval: ${lastStudentPermission.status}"
                                tvPermissionReason.text = "Reason: ${lastStudentPermission.reason}"
                                tvPermissionStart.text = "Starts: ${lastStudentPermission.start_time}"
                                tvPermissionEnd.text = "Ends: ${lastStudentPermission.end_time}"
                                tvPermissionUsed.text = "Expired: ${lastStudentPermission.has_exited}"
                            }

                        }else if(inOrOut == "In") {
                            val lastStudentPermission = checkLastStudentPermission(it)
                            val permissionEndDateDiff = checkDateDifference(lastStudentPermission!!, dateTimeFormatter, "end")
                            val permissionStartDateDiff = checkDateDifference(lastStudentPermission, dateTimeFormatter, "start")

                            Log.i("TAG-INFORMATION", "$permissionEndDateDiff")
                            Log.i("TAG-INFORMATION", "$permissionStartDateDiff")
                            Log.i("TAG-INFORMATION", "$lastStudentPermission")

                            if (permissionEndDateDiff <= 0 && lastStudentPermission.has_exited == "true") {
                                permissionID = lastStudentPermission.id
                                studentHasPermission = true
                                rlPermissionContainer.visibility = View.VISIBLE
                                tvPermissionApprovalStatus.text = "Approval: ${lastStudentPermission.status}"
                                tvPermissionReason.text = "Reason: ${lastStudentPermission.reason}"
                                tvPermissionStart.text = "Starts: ${lastStudentPermission.start_time}"
                                tvPermissionEnd.text = "Ends: ${lastStudentPermission.end_time}"
                                tvPermissionUsed.text = "Expired: ${lastStudentPermission.has_exited}"
                            }
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
                userToken = appDatabase.loggedInUserDao().getLastLogin().token
                studentViewModel.getStudentPermission(userToken, it.data.reg_number)
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
                    strStudentRegNumber = it.data.reg_number

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
                    attendanceTime.append(LocalDateTime.now().format(timeFormatter).toString())

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
            rlIn.background = ContextCompat.getDrawable(requireContext(), R.drawable.active_attendance)
            tvOut.setTextColor(resources.getColor(R.color.black))
            rlOut.background = ContextCompat.getDrawable(requireContext(), R.drawable.inactive_attendance)
        }else {
            tvOut.setTextColor(resources.getColor(R.color.white))
            rlOut.background = ContextCompat.getDrawable(requireContext(), R.drawable.active_attendance)
            tvIn.setTextColor(resources.getColor(R.color.black))
            rlIn.background = ContextCompat.getDrawable(requireContext(), R.drawable.inactive_attendance)
        }
    }

    // checkLastStudentPermission is used to check the last permission of the student
    @RequiresApi(Build.VERSION_CODES.O)
    fun checkLastStudentPermission(studentPermission: StudentPermissionResponse): Permission? {
        Log.i("TAG-INFORMATION", "$studentPermission")
        val sortedPermission = studentPermission.data?.permissions?.sortedWith {a, b -> a.id}
        return sortedPermission?.get(0)
    }

    // checkPermissionExpiryTime is used to check the expiry of the permission
    @RequiresApi(Build.VERSION_CODES.O)
    fun checkDateDifference(permission: Permission, dateTimeFormatter: DateTimeFormatter, type: String): Int {

        var strPermissionDate = permission.end_time
        if (type == "start") {
            strPermissionDate = permission.start_time
        }
        val strNow = LocalDateTime.now().format(dateTimeFormatter)
        val permissionEndDate = LocalDateTime.parse(strPermissionDate, dateTimeFormatter)
        val now = LocalDateTime.parse(strNow, dateTimeFormatter)
        val duration = Duration.between(now, permissionEndDate).toHours().toInt()
        return duration

    }

    override fun onItemSelected(
        p0: AdapterView<*>?,
        p1: View?,
        p2: Int,
        p3: Long
    ) {
        var x = p0?.getItemAtPosition(p2)

        Log.i("TAG-INFORMATION", "${x?.javaClass}")
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}