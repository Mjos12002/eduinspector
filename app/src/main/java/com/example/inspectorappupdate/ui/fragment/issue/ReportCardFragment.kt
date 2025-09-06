package com.example.inspectorappupdate.ui.fragment.issue

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.IndexActivity
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.repository.issue.IssueRepository
import com.example.inspectorappupdate.ui.fragment.student.SearchStudentFragment
import com.example.inspectorappupdate.ui.fragment.topup.CardTopUpFragment.Companion.appDatabase
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ReportCardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ReportCardFragment : Fragment() {
    // TODO: Rename and change types of parameters

    var userToken = ""
    var instituteID = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_report_card, container, false)

        // Logout button
        root.findViewById<ImageView>(R.id.img_logout).setOnClickListener {
            startActivity(Intent(requireContext(), IndexActivity::class.java))
        }

        appDatabase = DbUtility().dbBuilder(requireContext())

        lifecycleScope.launch {
            // Create a string builder to create a welcome message
            val msgBuilder = StringBuilder()
            val firstName = SearchStudentFragment.Companion.appDatabase.loggedInUserDao().getLastLogin().firstName
            val lastName = SearchStudentFragment.Companion.appDatabase.loggedInUserDao().getLastLogin().lastName
            userToken = SearchStudentFragment.Companion.appDatabase.loggedInUserDao().getLastLogin().token
            msgBuilder.append(getString(R.string.welcome))
            msgBuilder.append(", ")
            msgBuilder.append(firstName)
            msgBuilder.append(" ")
            msgBuilder.append(lastName)
            root.findViewById<RelativeLayout>(R.id.rl_student_details).visibility = View.VISIBLE
            root.findViewById<TextView>(R.id.tv_search_student_header).text = msgBuilder.toString()
        }

        val strPriority = root.findViewById<Spinner>(R.id.spn_priority)
        val strTitle = root.findViewById<EditText>(R.id.et_title)
        val strDescription = root.findViewById<EditText>(R.id.et_description)
        val btnReport = root.findViewById<Button>(R.id.btn_report)
        btnReport.setOnClickListener {
            lifecycleScope.launch {
                IssueRepository().addIssue(
                    "",
                    strTitle.text.toString(),
                    strDescription.text.toString(),
                    strPriority.selectedItemPosition,
                    instituteID
                )

            }
        }


        return root
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}