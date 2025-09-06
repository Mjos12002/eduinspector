package com.example.inspectorappupdate.ui.fragment.pin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
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

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_change_pin, container, false)

        root.findViewById<Button>(R.id.img_logout).setOnClickListener {
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


        // Initialize the view elements
        val etOldPIN = root.findViewById<EditText>(R.id.et_old_pin)
        val etrNewPIN = root.findViewById<EditText>(R.id.et_new_pin)
        val btnSave = root.findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            lifecycleScope.launch {
                CardRepository().changePIN("", etOldPIN.text.toString(), etrNewPIN.text.toString(), cardNumber)
            }
        }

        // Observe the change in card view model
        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {
            cardNumber = it.data.card_number
        })

        return root
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }
}