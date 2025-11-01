package com.example.inspectorappupdate.ui.fragment.topup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.IndexActivity
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.apirequest.dto.payment.PaymentDTO
import com.example.inspectorappupdate.model.payment.PaymentTypeModelData
import com.example.inspectorappupdate.model.wallet.WalletModelData
import com.example.inspectorappupdate.repository.payment.PaymentRepository
import com.example.inspectorappupdate.ui.fragment.student.SearchStudentFragment
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.payment.PaymentViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import com.example.inspectorappupdate.viewmodel.wallet.WalletViewModel
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CardTopUpFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CardTopUpFragment : Fragment(), AdapterView.OnItemSelectedListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // Initialize the card view model
    private val cardViewModel : CardViewModel by activityViewModels()

    // Initialize the wallet view model
    private val walletViewModel: WalletViewModel by activityViewModels()
    // Initialize the payment view model
    private val paymentViewModel: PaymentViewModel by activityViewModels()
    // Initialize the student view model
    private val studentViewModel: StudentViewModel by activityViewModels()
    // Initialize the selected walled id
    var selectedWalletID = 0
    // Initialize the list of wallet types
    lateinit var walletList: List<WalletModelData>
    // Initialize the list of payment types
    lateinit var paymentList: List<PaymentTypeModelData>
    // Initialize the ID of the parent, student id, card number and token
    var parentID = 0
    var studentID = 0
    var cardNumber = ""
    var userToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_card_top_up, container, false)
        val tvMsg = root.findViewById<TextView>(R.id.tv_processing_message)

        // Clear card view model
        lifecycleScope.launch {
            cardViewModel.clearCardDetails()
        }

        // Logout
        root.findViewById<ImageView>(R.id.img_logout).setOnClickListener {
            startActivity(Intent(requireContext(), IndexActivity::class.java))
        }

        // Initialize the database
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
            //root.findViewById<RelativeLayout>(R.id.rl_topup_student_details).visibility = View.VISIBLE
            root.findViewById<TextView>(R.id.tv_search_student_header).text = msgBuilder.toString()
        }

        // Initialize the wallet and payment type spinner
        val spnWalletType = root.findViewById<Spinner>(R.id.spn_transaction_type)
        val spnPaymentType = root.findViewById<Spinner>(R.id.spn_payment_type)

        // Observe the change from the card view model's card details
        cardViewModel.cardLiveData.observe(viewLifecycleOwner, Observer {

            // Initialize the container elements
            val rlStudentDetails = root.findViewById<RelativeLayout>(R.id.rl_topup_student_details)
            val rlPaymentDetails = root.findViewById<RelativeLayout>(R.id.rl_topup_payment_details)
            rlStudentDetails.visibility = View.GONE
            rlPaymentDetails.visibility = View.GONE

            tvMsg.text = getString(R.string.waitwhileprocessing)
            tvMsg.setTextColor("#01043B".toColorInt())
            try{
                if(it != null) {

                    if(it.status == 200 || it.status == 201) {
                        if(it.data != null) {
                            // If the card details is found
                            val name = StringBuilder()
                            name.append(it.data.first_name)
                            name.append(" ")
                            name.append(it.data.last_name)
                            root.findViewById<TextView>(R.id.tv_student_name)?.text = name.toString()
                            tvMsg.text = getString(R.string.card_details)
                            rlStudentDetails.visibility = View.VISIBLE
                            rlPaymentDetails.visibility = View.VISIBLE
                            cardNumber = it.data.card_number

                            lifecycleScope.launch {
                                getStudentDetails(appDatabase.loggedInUserDao().getLastLogin().token, it.data.reg_number)
                            }

                            root.findViewById<TextView>(R.id.tv_card_number)?.text = "Card No: ${it.data.card_number}"
                            root.findViewById<TextView>(R.id.tv_transport_balance)?.text = "Transport: ${it.data.transport_fees_balance}"
                            root.findViewById<TextView>(R.id.tv_schoolfees_balance)?.text = "School fees: ${it.data.school_fees_balance}"
                            root.findViewById<TextView>(R.id.tv_support_balance)?.text = "Support: ${it.data.support_fees_balance}"
                            root.findViewById<TextView>(R.id.tv_insurance_balance)?.text = "Insurance: ${it.data.transport_fees_balance}"

                        }

                        if(it.data == null){
                            tvMsg.text = getString(R.string.unregistered_card)
                            tvMsg.setTextColor("#CC2A02".toColorInt())
                        }
                    }else {
                        if(it.message.contains("resolve")) {
                            tvMsg.text = getString(R.string.internet)
                        }else {
                            tvMsg.text = it.message
                        }
                        tvMsg.setTextColor("#CC2A02".toColorInt())
                    }

                }else {
                    tvMsg.text = getString(R.string.unknown_error)
                }

            }catch (e: Exception){
                Log.i("INSPECTOR-LOG", "${e.message}")
            }
        })

        // Observe the change from card view model
        studentViewModel.studentDetailsLiveData.observe(viewLifecycleOwner, Observer {
            if(it.data != null) {
                if(it.data.student !== null) {
                    parentID = it.data.student.parent_user_id
                    studentID = it.data.student.id
                }
            }
        })

        // Get the list of wallets and payment types
        lifecycleScope.launch {
            val token = appDatabase.loggedInUserDao().getLastLogin().token
            walletViewModel.getWallet(token)
            paymentViewModel.getPaymentType(token)
        }

        // Observe the changes in the wallet response
        walletViewModel.walletLiveData.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                if(it.status == 200 || it.status == 201) {

                    if(it.data != null) {
                        val walletArr = it.data.map { dt -> dt.name }
                        walletList = it.data
                        val ad = ArrayAdapter(requireContext(),
                            android.R.layout.simple_spinner_item, walletArr
                        )

                        ad.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                        spnWalletType?.adapter = ad
                        ad.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                        ad.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                        spnWalletType?.onItemSelectedListener = this as AdapterView.OnItemSelectedListener?
                    }else {
                        tvMsg.text = "Wallet data not found"
                    }

                }else {
                    if (it.message.contains("resolve")) {
                        tvMsg.text = getString(R.string.internet)
                    }else {
                        tvMsg.text = it.message
                    }
                }

            }else {
                tvMsg.text = getString(R.string.unknown_error)
            }

        })

        // Observe the changes in the payment type
        paymentViewModel.paymentLiveData.observe(viewLifecycleOwner, Observer {
            if(it != null) {

                if(it.status == 200 || it.status == 201) {

                    if(it.data != null) {
                        paymentList = it.data
                        val paymentTypeArr = it.data.map { dt -> dt.name }
                        val ad = ArrayAdapter(requireContext(),
                            android.R.layout.simple_spinner_item, paymentTypeArr
                        )

                        ad.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                        spnPaymentType?.adapter = ad
                        ad.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )
                        ad.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                        )

                        spnPaymentType?.onItemSelectedListener = this as AdapterView.OnItemSelectedListener?

                    }else {
                        tvMsg.text = "Payment type data not found"
                    }

                }else {
                    if (it.message.contains("resolve")) {
                        tvMsg.text = getString(R.string.internet)
                    }else {
                        tvMsg.text = it.message
                    }
                }

            }else {
                tvMsg.text = getString(R.string.unknown_error)
            }

        })
        val btnPay = root.findViewById<Button>(R.id.btn_topup)
        // Declaring containers for student and payment details

        // Execute payment process
        btnPay.setOnClickListener {
            btnPay.text = getString(R.string.waitwhileprocessing)
            val amount = root.findViewById<EditText>(R.id.et_amount).text.toString()
            val phoneNUmber = root.findViewById<EditText>(R.id.et_phone_number).text.toString()
            val walletType = spnWalletType.selectedItemPosition.toString()
            val paymentType = spnPaymentType.selectedItemPosition.toString()
            val walletTypeID = walletList[walletType.toInt()].id
            val paymentTypeID = paymentList[paymentType.toInt()].id
            val paymentDTO = PaymentDTO(amount.toInt(), walletTypeID, paymentTypeID, phoneNUmber, studentID, parentID, cardNumber)
            // Make payment
            lifecycleScope.launch {
                val res = PaymentRepository().makePayment(appDatabase.loggedInUserDao().getLastLogin().token, paymentDTO)
                // If the response is not null

                if(res != null) {

                    // If the status is 200 it means there is a response returned
                    if(res.status == 200) {
                        btnPay.setBackgroundColor("#039E37".toColorInt())
                        btnPay.text = res.message
                    }else {
                        if(res.message.contains("resolve")) {
                            btnPay.text = getString(R.string.internet)
                        }else {
                            btnPay.text = res.message
                        }
                        btnPay.setBackgroundColor("#9E1F03".toColorInt())
                    }
                }
            }

        }

        return root
    }

    override fun onItemSelected(
        p0: AdapterView<*>?,
        p1: View?,
        p2: Int,
        p3: Long
    ) {
        Log.i("INSPECTOR-LOG", "${p1?.id} ")
        selectedWalletID = walletList[p2].id
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    companion object {
        lateinit var appDatabase: AppDatabase
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentDetails(bearer: String, regNumber: String){
        studentViewModel.getStudentDetails(bearer, regNumber)
    }
}