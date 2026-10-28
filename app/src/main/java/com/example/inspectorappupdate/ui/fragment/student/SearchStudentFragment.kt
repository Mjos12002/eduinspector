package com.example.inspectorappupdate.ui.fragment.student

import com.example.inspectorappupdate.enums.HttpRequestEnums
import com.squareup.picasso.Picasso
import com.example.inspectorappupdate.utils.signOut
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.inspectorappupdate.R
import com.example.inspectorappupdate.databinding.FragmentSearchStudentBinding
import com.example.inspectorappupdate.entity.academic_term.AcademicTermEntity
import com.example.inspectorappupdate.model.offense_type.OffenseTypeData
import com.example.inspectorappupdate.model.student.StudentModelData
import com.example.inspectorappupdate.repository.academic_term.AcademicTermRepository
import com.example.inspectorappupdate.repository.offense.OffenseRepository
import com.example.inspectorappupdate.repository.offense_type.OffenseTypeRepository
import com.example.inspectorappupdate.repository.signin.LoginRepository
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.utils.Keyboard
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.student.StudentViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Discipline: tap the student's card, choose the fault, confirm. Clears 2 s after the card is taken away. */
class SearchStudentFragment : Fragment() {

    private var _binding : FragmentSearchStudentBinding? = null
    private val binding get() = _binding!!

    private val studentViewModel: StudentViewModel by viewModels()
    private val cardViewModel: CardViewModel by activityViewModels()

    private var academicYearID = 0
    private var academicTermID = 0
    private var instituteID = 0
    private var userToken = ""

    private var faultTypes: List<OffenseTypeData>? = null
    private var faultTypesError: String? = null

    // The student on screen, and whether they came from a card tap (cleared when the card leaves).
    private var student: StudentModelData? = null
    private var fromCard = false
    private var cardHeld = false
    private var fault: OffenseTypeData? = null
    private var saving = false
    private var tappedCard: String? = null
    private var tappedPhoto: String? = null
    private var resetJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchStudentBinding.inflate(inflater, container, false)
        binding.imgLogout.setOnClickListener { signOut() }

        appDatabase = DbUtility().dbBuilder(requireContext())

        lifecycleScope.launch {
            val login = appDatabase.loggedInUserDao().getLastLogin()
            userToken = login.token
            binding.tvSearchStudentHeader.text = "${getString(R.string.welcome)}, ${login.firstName}"
            if (login.role == "inspector") {
                val res = LoginRepository().getDeployedUser(login.token, login.id.toInt())
                if ((res.status == 200 || res.status == 201) && res.data != null) instituteID = res.data.institute_id
            }
            loadTerm()
            loadFaultTypes()
        }

        binding.imvSearch.setOnClickListener { searchTyped() }
        // Posted so it replaces the shared Done handler set after the view is created.
        binding.etSearchStudent.post {
            _binding?.etSearchStudent?.setOnEditorActionListener { _, actionId, event ->
                val enterKey = actionId == EditorInfo.IME_NULL && event?.action == KeyEvent.ACTION_DOWN
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || enterKey) {
                    searchTyped()
                    true
                } else false
            }
        }
        binding.imgClearStudent.setOnClickListener { reset() }
        binding.btnBack.setOnClickListener { chooseFault(null) }
        binding.btnConfirm.setOnClickListener { record() }

        cardViewModel.cardPresent.observe(viewLifecycleOwner) { present ->
            cardHeld = present
            if (present) {
                // A new tap replaces whatever was on screen.
                resetJob?.cancel()
                if (!saving) {
                    fromCard = true
                    showWaiting(getString(R.string.discipline_looking_up))
                }
            } else if (fromCard && !saving) {
                resetSoon()
            }
        }

        cardViewModel.cardLiveData.observe(viewLifecycleOwner) { card ->
            if (cardViewModel.isNoCard(card) || !fromCard || saving) return@observe
            if (card.error) {
                showWaiting(card.message.ifBlank { getString(R.string.discipline_failed) }, error = true)
                return@observe
            }
            // An unknown chip comes back as a success without data.
            if (card.data == null || card.data.reg_number.isNullOrBlank()) {
                showWaiting(getString(R.string.discipline_card_unknown), error = true)
                return@observe
            }
            // A blocked or expired card records nothing; the server refuses it too.
            if (card.data.usable == false || !card.data.card_status.equals("active", ignoreCase = true)) {
                showWaiting(getString(R.string.discipline_card_unusable, card.data.card_status.lowercase()), error = true)
                return@observe
            }
            tappedCard = card.data.card_number
            tappedPhoto = card.data.photo
            binding.tvTapPrompt.text = "${card.data.first_name} ${card.data.last_name}"
            lifecycleScope.launch { studentViewModel.searchStudentByRegNumber(userToken, card.data.reg_number) }
        }

        studentViewModel.studentLiveData.observe(viewLifecycleOwner) {
            if (saving) return@observe
            if (it.error || it.data == null || it.data.student == null) {
                showWaiting(it.message.ifBlank { getString(R.string.discipline_failed) }, error = true)
                return@observe
            }
            showStudent(it.data)
        }

        showWaiting(getString(R.string.permissions_tap_card))
        return binding.root
    }

    override fun onDestroyView() {
        resetJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun searchTyped() {
        val regNumber = binding.etSearchStudent.text.toString().trim()
        if (regNumber.isEmpty()) return
        Keyboard.hide(requireActivity())
        resetJob?.cancel()
        fromCard = false
        tappedCard = null
        tappedPhoto = null
        showWaiting(getString(R.string.discipline_looking_up))
        lifecycleScope.launch { studentViewModel.searchStudentByRegNumber(userToken, regNumber) }
    }

    /** Step 0: no student yet; the prompt, a lookup in progress, or why the card was refused. */
    private fun showWaiting(message: String, error: Boolean = false) {
        student = null
        fault = null
        binding.tvTapPrompt.text = message
        binding.tvTapPrompt.setTextColor(
            if (error) Color.parseColor("#B3261E") else ContextCompat.getColor(requireContext(), R.color.card_details_text_color)
        )
        binding.tvTapPrompt.visibility = View.VISIBLE
        binding.llStudent.visibility = View.GONE
        binding.llFaults.visibility = View.GONE
        binding.llConfirm.visibility = View.GONE
        // The search box is only for finding a student without a card.
        binding.llSearch.visibility = if (fromCard && cardHeld) View.GONE else View.VISIBLE
    }

    /** Step 1 and 2: the student's details, then the list of faults. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showStudent(found: StudentModelData) {
        student = found
        binding.llSearch.visibility = View.GONE
        binding.tvTapPrompt.visibility = View.GONE
        binding.llStudent.visibility = View.VISIBLE
        binding.tvStudentName.text = "${found.first_name} ${found.last_name}"
        binding.tvStudentRegNumber.text = getString(R.string.discipline_reg, found.reg_number)
        showPhoto(found.photo ?: tappedPhoto)
        binding.tvTotalDeductionValue.text = "…"
        binding.tvLastOffense.text = ""
        loadTermFaults(found.student!!.id)
        chooseFault(null)
    }

    private fun chooseFault(chosen: OffenseTypeData?) {
        fault = chosen
        binding.tvResult.visibility = View.GONE
        if (chosen == null) {
            binding.llConfirm.visibility = View.GONE
            binding.llFaults.visibility = View.VISIBLE
            renderFaultTypes()
            return
        }
        // Step 3: what will be recorded.
        binding.llFaults.visibility = View.GONE
        binding.llConfirm.visibility = View.VISIBLE
        binding.tvConfirmFault.text = chosen.type_name
        binding.tvConfirmMarks.text = getString(R.string.discipline_marks, marks(chosen))
        val punishment = chosen.punishment?.takeIf { it.isNotBlank() }
        binding.tvConfirmPunishment.visibility = if (punishment != null) View.VISIBLE else View.GONE
        binding.tvConfirmPunishment.text = punishment?.let { getString(R.string.discipline_punishment, it) }
        binding.btnConfirm.isEnabled = true
        binding.btnConfirm.text = getString(R.string.discipline_confirm)
        binding.btnConfirm.visibility = View.VISIBLE
        binding.btnBack.visibility = View.VISIBLE
        binding.svDiscipline.post { _binding?.svDiscipline?.smoothScrollTo(0, binding.llConfirm.top) }
    }

    private fun renderFaultTypes() {
        val list = binding.llFaultList
        list.removeAllViews()
        val types = faultTypes
        val message = when {
            faultTypesError != null -> faultTypesError
            types == null -> getString(R.string.discipline_loading_faults)
            types.isEmpty() -> getString(R.string.discipline_no_fault_types)
            else -> null
        }
        binding.tvFaultsMessage.text = message
        binding.tvFaultsMessage.visibility = if (message != null) View.VISIBLE else View.GONE
        types?.forEach { type ->
            val row = layoutInflater.inflate(R.layout.item_discipline_fault, list, false)
            row.findViewById<TextView>(R.id.tv_fault_name).text = type.type_name
            row.findViewById<TextView>(R.id.tv_fault_marks).text = getString(R.string.discipline_marks, marks(type))
            val category = type.offense_category?.name?.takeIf { it.isNotBlank() }
            row.findViewById<TextView>(R.id.tv_fault_category).apply {
                text = category
                visibility = if (category != null) View.VISIBLE else View.GONE
            }
            row.setOnClickListener { chooseFault(type) }
            list.addView(row)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun record() {
        val record = student?.student ?: return
        val chosen = fault ?: return
        // An inspector records at their school; an admin at the student's school.
        val school = instituteID.takeIf { it > 0 } ?: record.current_institute_id ?: 0
        if (school <= 0) {
            showResult(getString(R.string.invalid_institute), ok = false)
            return
        }
        saving = true
        binding.btnConfirm.isEnabled = false
        binding.btnConfirm.text = getString(R.string.discipline_recording)
        binding.btnBack.visibility = View.GONE
        lifecycleScope.launch {
            val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val resp = OffenseRepository().createStudentOffense(userToken, record.id, chosen.id, academicYearID, academicTermID, date, school,
                tappedCard.takeIf { fromCard })
            saving = false
            if (_binding == null) return@launch
            if (resp.error) {
                binding.btnConfirm.isEnabled = true
                binding.btnConfirm.text = getString(R.string.discipline_confirm)
                binding.btnBack.visibility = View.VISIBLE
                showResult(resp.message.ifBlank { getString(R.string.discipline_failed) }, ok = false)
            } else {
                binding.btnConfirm.visibility = View.GONE
                val left = resp.data?.current_marks
                showResult(if (left != null) getString(R.string.discipline_recorded_marks, left, resp.data.initial_marks ?: 40)
                    else getString(R.string.discipline_recorded), ok = true)
                loadTermFaults(record.id)
                // A typed search has no card to take away, so it clears on its own.
                if (!fromCard || !cardHeld) resetSoon()
            }
            if (fromCard && !cardHeld && resp.error) resetSoon()
        }
    }

    // The photo link is relative to the API host and signed, so it opens without the sign-in token.
    private fun showPhoto(photo: String?) {
        binding.ivStudentPhoto.setImageDrawable(null)
        val path = photo?.takeIf { it.isNotBlank() } ?: return
        val url = if (path.startsWith("http")) path else HttpRequestEnums.BASEURL.value.trimEnd('/') + "/" + path.trimStart('/')
        Picasso.get().load(url).fit().centerCrop().into(binding.ivStudentPhoto)
    }

    private fun showResult(message: String, ok: Boolean) {
        binding.tvResult.text = message
        binding.tvResult.setTextColor(
            if (ok) ContextCompat.getColor(requireContext(), R.color.main_green_color) else Color.parseColor("#B3261E")
        )
        binding.tvResult.visibility = View.VISIBLE
    }

    private fun resetSoon() {
        resetJob?.cancel()
        resetJob = lifecycleScope.launch {
            delay(RESET_AFTER_MS)
            reset()
        }
    }

    private fun reset() {
        resetJob?.cancel()
        if (_binding == null || saving) return
        fromCard = false
        binding.etSearchStudent.text?.clear()
        showWaiting(getString(R.string.permissions_tap_card))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadTerm() {
        try {
            val academicModel = AcademicTermRepository().getAcademicTerm(userToken)
            val academicEntity = AcademicTermEntity(0, academicModel.data.id, academicModel.data.academic_year_id, academicModel.data.term_name, academicModel.data.start_date, academicModel.data.end_date)
            appDatabase.academicTermDao().insert(academicEntity)
            academicTermID = academicEntity.termID
            academicYearID = academicEntity.yearID
        } catch (e: Exception) {
            Log.i("ACADEMIC-TERM", "${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadFaultTypes() {
        val res = OffenseTypeRepository().getOffenseType(userToken)
        if (res.error) {
            faultTypesError = res.message
        } else {
            faultTypes = res.data.filter { it.id > 0 }.sortedBy { it.type_name.lowercase() }
            faultTypesError = null
        }
        if (_binding != null && student != null && fault == null) renderFaultTypes()
    }

    // This term's deductions for the student on screen; dropped if another student took their place.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTermFaults(studentID: Int) {
        lifecycleScope.launch {
            if (academicTermID == 0) loadTerm()
            val res = OffenseRepository().getStudentOffense(userToken, studentID, academicTermID)
            if (_binding == null || student?.student?.id != studentID) return@launch
            val faults = if (res.error) emptyList() else res.data.filter { it.id > 0 }
            binding.tvTotalDeductionValue.text = "${faults.sumOf { it.marks_deducted }}"
            binding.tvLastOffense.text = faults.lastOrNull()?.let { getString(R.string.discipline_last_fault, it.offense_type.type_name) }
                ?: getString(R.string.discipline_no_faults_yet)
        }
    }

    private fun marks(type: OffenseTypeData): String {
        val value = type.mark_deduction ?: 0.0
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    companion object {
        lateinit var appDatabase: AppDatabase
        private const val RESET_AFTER_MS = 2000L
    }
}
