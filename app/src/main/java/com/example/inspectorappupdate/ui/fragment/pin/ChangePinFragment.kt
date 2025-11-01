package com.example.inspectorappupdate.ui.fragment.pin

import android.content.Intent
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
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.IndexActivity
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.repository.card.CardRepository
import com.example.inspectorappupdate.ui.fragment.issue.ReportCardFragment.Companion.appDatabase
import com.example.inspectorappupdate.ui.fragment.student.SearchStudentFragment
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChangePinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChangePinFragment : Fragment() {
    // TODO: Rename and change types of parameters
    var userToken = ""

    // Initialize the card view model
    private val cardViewModel: CardViewModel by activityViewModels()
    var cardNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            cardViewModel.clearCardDetails()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_change_pin, container, false)
        // Initialize the container elements
        val rlStudentDetails = root.findViewById<RelativeLayout>(R.id.rl_student_details)
        val rlPinDetails = root.findViewById<RelativeLayout>(R.id.rl_pin_details)

        root.findViewById<ImageView>(R.id.img_logout).setOnClickListener {
            startActivity(Intent(requireContext(), IndexActivity::class.java))
        }

        appDatabase = DbUtility().dbBuilder(requireContext())

        lifecycleScope.launch {
            // Clear card view model
            cardViewModel.clearCardDetails()
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
            root.findViewById<TextView>(R.id.tv_search_student_header).text = msgBuilder.toString()
        }


        // Initialize the view elements
        val etOldPIN = root.findViewById<EditText>(R.id.et_old_pin)
        val etrNewPIN = root.findViewById<EditText>(R.id.et_new_pin)
        val btnSave = root.findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            lifecycleScope.launch {
                btnSave.text = getString(R.string.waitwhileprocessing)
                val resp = CardRepository().changePIN(appDatabase.loggedInUserDao().getLastLogin().token, etOldPIN.text.toString(), etrNewPIN.text.toString(), cardNumber)
                if(resp.message.contains("resolve")) {
                    btnSave.text = getString(R.string.internet)
                }else {
                    btnSave.text = resp.message
                }

            }
        }

        // Observe the change in card view model
        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {
            try {
                val msgHolder = root.findViewById<TextView>(R.id.tv_processing_message)

                msgHolder.text = getString(R.string.waitwhileprocessing)
                msgHolder.setTextColor("#000630".toColorInt())
                Log.i("INSPECTOR-LOG", " top $it")
                // If the response is returned
                if(it != null){
                    if(it.status == 200 || it.status == 201) {
                        // If there response contains data object
                        Log.i("INSPECTOR-LOG", "$it")
                        if(it.data != null) {
                            // Initialize the variables
                            cardNumber = it.data.card_number
                            val name = StringBuilder()
                            val cardNumber = StringBuilder()
                            val status = StringBuilder()

                            name.append(it.data.first_name)
                            name.append(" ")
                            name.append(it.data.last_name)

                            cardNumber.append("Card No: ")
                            cardNumber.append(it.data.card_number)

                            status.append("Status: ")
                            status.append(it.data.card_status)

                            root.findViewById<TextView>(R.id.tv_student_name).text = name.toString()
                            root.findViewById<TextView>(R.id.tv_student_reg_number).text = cardNumber.toString()
                            root.findViewById<TextView>(R.id.tv_card_status).text = status.toString()

                            rlStudentDetails.visibility = View.VISIBLE
                            rlPinDetails.visibility = View.VISIBLE
                            msgHolder.text = getString(R.string.card_details)

                        }else {
                            msgHolder.text = getString(R.string.unregistered_card)
                            msgHolder.setTextColor("#BA2202".toColorInt())
                        }
                    }else {
                        msgHolder.setTextColor("#BA2202".toColorInt())
                        if(it.message.contains("resolve")) {
                            msgHolder.text = getString(R.string.internet)
                        }else {
                            msgHolder.text = it.message
                        }
                    }

                }else {
                    msgHolder.text = getString(R.string.unknown_error)
                }

            }catch (e: Exception){

            }
        })

        return root
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}