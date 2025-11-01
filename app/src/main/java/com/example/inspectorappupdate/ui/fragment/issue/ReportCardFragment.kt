package com.example.inspectorappupdate.ui.fragment.issue

import android.content.Intent
import android.database.DatabaseUtils
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.rotationMatrix
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.IndexActivity
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.repository.card.CardRepository
import com.example.inspectorappupdate.repository.issue.IssueRepository
import com.example.inspectorappupdate.ui.fragment.student.SearchStudentFragment
import com.example.inspectorappupdate.ui.fragment.topup.CardTopUpFragment.Companion.appDatabase
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import kotlin.getValue

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
    var cardIndicator = ""

    private val cardViewModel: CardViewModel by activityViewModels()

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
        // Clear the card view model
        // Initialize database utility
        appDatabase = DbUtility().dbBuilder(requireContext())
        // Set logged in user details
        try{
            lifecycleScope.launch {
                cardViewModel.clearCardDetails()
                root.findViewById<TextView>(R.id.tv_search_student_header).text = "Welcome, ${appDatabase.loggedInUserDao().getLastLogin().lastName}"
            }
        }catch (e: Exception){

        }
        // Initialize a card view model

        // Logout button
        root.findViewById<ImageView>(R.id.img_logout).setOnClickListener {
            startActivity(Intent(requireContext(), IndexActivity::class.java))
        }

        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {
            if(!it.error) {
                root.findViewById<RelativeLayout>(R.id.rl_block_action).visibility = View.GONE
                root.findViewById<RelativeLayout>(R.id.rl_student_details).visibility = View.GONE
                if (it.data != null) {
                    // Set the variable of card indicator
                    cardIndicator = it.data.indicator
                    // Build and show the name
                    val nameStrBuilder = StringBuilder()
                    nameStrBuilder.append(it.data.first_name)
                    nameStrBuilder.append(" ")
                    nameStrBuilder.append(it.data.last_name)
                    root.findViewById<TextView>(R.id.tv_student_name).text = nameStrBuilder.toString()

                    // Add other views' text
                    root.findViewById<TextView>(R.id.tv_student_reg_number).text = "Reg No: ${it.data.reg_number}"
                    root.findViewById<TextView>(R.id.tv_card_number).text = "Card No: ${it.data.card_number}"
                    root.findViewById<TextView>(R.id.tv_card_status).text = "Card Status: ${it.data.card_status}"

                    // Show the container views
                    root.findViewById<RelativeLayout>(R.id.rl_block_action).visibility = View.VISIBLE
                    root.findViewById<RelativeLayout>(R.id.rl_student_details).visibility = View.VISIBLE
                }else {
                    Toast.makeText(requireContext(), "Card Information Not Found", Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
            }
        })

        val btnReport = root.findViewById<Button>(R.id.btn_report)
        btnReport.setOnClickListener {
            lifecycleScope.launch {
                try{
                    btnReport.text = getString(R.string.waitwhileprocessing)
                    val token = appDatabase.loggedInUserDao().getLastLogin().token
                    val res = CardRepository().disableCard(cardIndicator, token)
                    Log.i("INSPECTOR-LOG", cardIndicator)
                    if (res.error) {
                        btnReport.text = res.message
                    }else {
                        btnReport.text = res.message
                    }
                }catch (e: Exception){
                        btnReport.text = getString(R.string.error_try_again)
                }
            }
        }

        return root
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}