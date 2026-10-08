package com.example.inspectorappupdate

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.inspectorappupdate.databinding.ActivitySearchStudentBinding
import com.example.inspectorappupdate.utils.AppDatabase
import com.example.inspectorappupdate.utils.AppRole
import com.example.inspectorappupdate.utils.DbUtility
import com.example.inspectorappupdate.utils.RetrofitInstance
import com.example.inspectorappupdate.utils.Session
import com.example.inspectorappupdate.viewmodel.card.CardViewModel
import com.example.inspectorappupdate.viewmodel.gate.StudentGateViewModel
import com.example.inspectorappupdate.viewmodel.visitor.VisitorGateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import kotlin.collections.contains
import java.util.concurrent.atomic.AtomicBoolean

class SearchStudentActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySearchStudentBinding

    val cardViewModel: CardViewModel by viewModels()
    val visitorGateViewModel: VisitorGateViewModel by viewModels()
    val studentGateViewModel: StudentGateViewModel by viewModels()

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The signed-in role decides the bottom bar and the first screen.
        val role = intent.getStringExtra(Session.EXTRA_ROLE)
            ?: runBlocking { Session.current(this@SearchStudentActivity)?.role }
        val visibleTiles = AppRole.tiles(role)
        listOf(
            binding.rlDisplineDeduction, binding.rlStudentAttendance, binding.rlVisitorGate,
            binding.rlReportLost, binding.rlCardTopup, binding.rlEditPin,
            binding.rlCardTools, binding.rlPermissions
        ).forEach { tile ->
            tile?.visibility = if (tile?.id in visibleTiles) View.VISIBLE else View.GONE
        }

        visitorGateViewModel.setAllowedModes(AppRole.visitorModes(role))

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_search_student) as NavHostFragment
        val navController = navHost.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph2)
        graph.setStartDestination(AppRole.startDestination(role))
        navController.setGraph(graph, null)
        appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.rlCardTools.setOnClickListener {
            navController.navigate(R.id.CardToolsFragment)
        }

        binding.rlPermissions.setOnClickListener {
            navController.navigate(R.id.PermissionsFragment)
        }

        binding.rlEditPin.setOnClickListener {
            navController.navigate(R.id.ChangePinFragment)
        }

        binding.rlCardTopup.setOnClickListener {
            navController.navigate(R.id.CardTopUpFragment)
        }

        binding.rlReportLost.setOnClickListener {
            navController.navigate(R.id.ReportCardFragment)
        }

        binding.rlStudentAttendance.setOnClickListener {
            navController.navigate(R.id.StudentGateFragment)
        }

        binding.rlVisitorGate.setOnClickListener {
            navController.navigate(R.id.VisitorGateFragment)
        }

        binding.rlDisplineDeduction.setOnClickListener {
            navController.navigate(R.id.SearchStudentFragment)
        }

        // Adapter of the NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if(nfcAdapter == null) {
            Toast.makeText(baseContext, "No NFC found", Toast.LENGTH_LONG).show()
            return
        }

        // Initialize the room database
        appDatabase = DbUtility().dbBuilder(baseContext)

    }

    override fun onStart() {
        super.onStart()
        // A rejected token (account blocked, token expired) ends the session once.
        val handled = AtomicBoolean(false)
        RetrofitInstance.onUnauthorized = {
            if (handled.compareAndSet(false, true)) {
                runOnUiThread { Session.signOut(this, getString(R.string.session_ended)) }
            }
        }
    }

    override fun onStop() {
        RetrofitInstance.onUnauthorized = null
        super.onStop()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_search_student)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalStdlibApi::class)
    private fun resolveIntent(intent: Intent) {
        val validActions = listOf(
            NfcAdapter.ACTION_TAG_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED
        )

        if (intent.action in validActions) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            // On the visitor gate screen any tag type is accepted; the chip UID identifies the card.
            val navController = findNavController(R.id.nav_host_fragment_content_search_student)
            // The student gate records the tap itself; the chip UID identifies the card.
            if (tag != null && navController.currentDestination?.id == R.id.StudentGateFragment) {
                val uid = tag.id.toHexString()
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                lifecycleScope.launch {
                    val token = try {
                        appDatabase.loggedInUserDao().getLastLogin().token
                    } catch (e: Exception) {
                        Toast.makeText(baseContext, "Sign in again", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    studentGateViewModel.onCardTapped(token, uid, deviceId)
                }
                watchCardPresence(tag) { studentGateViewModel.onCardRemoved() }
                return
            }
            if (tag != null && navController.currentDestination?.id == R.id.VisitorGateFragment) {
                val uid = tag.id.toHexString()
                val chipType = tag.techList.firstOrNull()?.substringAfterLast('.')
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                lifecycleScope.launch {
                    val token = try {
                        appDatabase.loggedInUserDao().getLastLogin().token
                    } catch (e: Exception) {
                        Toast.makeText(baseContext, "Sign in again", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    visitorGateViewModel.onCardTapped(token, uid, chipType, deviceId)
                }
                watchCardPresence(tag) { visitorGateViewModel.onCardRemoved() }
                return
            }

            try {
                val tagFromIntent = tag
                val techList = tagFromIntent?.techList?.toList()
                techList?.forEach { it ->
                    // If the card is of class ultralight, connect and get the card number
                    if(it == MifareUltralight::class.java.name) {
                        val ultralight = MifareUltralight.get(tagFromIntent)
                        ultralight.connect()
                        val tagID = ultralight.tag.id.toHexString()
                        lifecycleScope.launch {
                            cardViewModel.getCardDetails(appDatabase.loggedInUserDao().getLastLogin().token, tagID)
                        }
                    }
                }

            }catch (e: Exception){
                Toast.makeText(baseContext, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private var presenceJob: Job? = null

    // Tells a gate screen when the tapped card is taken away, so its result stays up until then.
    // Reconnecting fails once the card is gone.
    private fun watchCardPresence(tag: Tag, onRemoved: () -> Unit) {
        presenceJob?.cancel()
        val chip = NfcA.get(tag)
        if (chip == null) {
            onRemoved()
            return
        }
        visitorGateViewModel.onCardPresent()
        presenceJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    delay(PRESENCE_CHECK_MS)
                    try {
                        chip.close()
                        chip.connect()
                    } catch (e: Exception) {
                        break
                    }
                }
                if (isActive) withContext(Dispatchers.Main) { onRemoved() }
            } finally {
                try { chip.close() } catch (_: Exception) { }
            }
        }
    }

    private fun openNfcSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_NFC)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter?.isEnabled == false) {
            openNfcSettings()
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        presenceJob?.cancel()
        visitorGateViewModel.onCardRemoved()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolveIntent(intent)
    }

    companion object {
        lateinit var appDatabase: AppDatabase
        private const val PRESENCE_CHECK_MS = 400L
    }
}