package id.linov.beats

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.*
import android.util.Log.e
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import id.linov.beats.adapters.ClientsAdapter
import id.linov.beats.udp.UDPHelper
import id.linov.beatslib.BEATS_STRATEGY
import id.linov.beatslib.SERVICE_ID
import id.linov.beatslib.SERVICE_NAME
import id.linov.beatslib.User
import kotlinx.android.synthetic.main.activity_run_server.*

class RunServerActivity : AppCompatActivity() {

    var started = false
    var progressing = false
    val callback = object : ConnectionLifecycleCallback() {
        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
            e("CLC", "onConnectionResult $p0 - ${p1.status}")
            e("CLC", "onConnectionResult $p0 - ${p1.status.statusCode}")
            if (p1.status.statusCode == 13) {
                val user = Games.users[p0]
                if (user != null) {
                    Games.leaveGroup(p0)
                    Games.users.remove(p0)
                }
                updateList()
            }
        }

        override fun onDisconnected(p0: String) {
            e("CLC", "onDisconnected $p0")
            val user = Games.users[p0]
            if (user != null) {
                Games.leaveGroup(p0)
                Games.users.remove(p0)

            }
            updateList()
        }

        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            e("CLC", "onConnectionInitiated $p0")
            Nearby.getConnectionsClient(this@RunServerActivity).acceptConnection(p0, payloadCallback)
                .addOnSuccessListener {
                    e("SUCCESS", "PAIRED WITH ${p1.endpointName} $p0")
                    Games.users[p0] = User(p1.endpointName)
                    updateList()
                }
                .addOnFailureListener {
                    e("FAILED", "$it")
                }
        }
    }

    private fun updateList() {
//        e("PARENT", "UPDATING RECYCLER")
//        e("USER  -=--", " == ${Games.users} ==")
        adapter.notifyDataSetChanged()
        txtParticipanNumber.text = "${Games.users.size} Participant"

//        Games.users.forEach {
//            it.value.let { e("USER  ====", "$it") }
//        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            e("PAYLOAD", "from $p0 : data --> ${p1}")
            Games.save(p0, p1)
            updateList()
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            e("onPayloadTransferUpdate", "from $p0 : data --> ${p1}")
        }

    }
    val adapter: ClientsAdapter by lazy { ClientsAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run_server)

        btToggleService.setOnClickListener {
            toggle()
        }
        animateButton()

        rvContent.adapter = adapter
        rvContent.layoutManager = LinearLayoutManager(this)
        Games.init(applicationContext) {
            e("PARENT", "setup")
            updateList()
        }
    }

    private fun toggle() {
        if (inputServerName.text.toString().isBlank()) {
            inputServerName.error = "Server name must not be empty"
        }
        if (btToggleService.text == "START") {
            UDPHelper.initReceiver()
            Games.clearAll()
            startAdvertising()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Clear all data?")
                .setMessage("WARNING: Clearing all data will resulting all game progress will be unsaved.")
                .setPositiveButton("Clear") { dialog, _ ->
                    Games.clearAll()
                    updateList()
                    dialog.dismiss()
                }.setNegativeButton("Cancle") {di, _ ->
                    di.dismiss()
                }.show()
        }
    }

    private fun stopAdvertise() {
        Nearby.getConnectionsClient(this).stopAdvertising()
        started = false
        animateButton()
    }

    private fun startAdvertising() {
//        val ao = AdvertisingOptions.Builder().setStrategy(BEATS_STRATEGY).build()
        started = true
        progressing = false
        animateButton()
    }

    private fun animateButton() {
        val bg = if (started) R.color.colorAccent else R.color.colorPrimary
        val text = if (started) "CLEAN" else "START"
        progress.visibility = if (progressing) View.VISIBLE else View.GONE

        btToggleService.setBackgroundColor(ContextCompat.getColor(this, bg))
        btToggleService.text = text
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(root)
            val c1 = ConstraintSet()
            c1.clone(this, R.layout.activity_run_server)
            val c2 = ConstraintSet()
            c2.clone(this, R.layout.activity_run_server_alt)
            val constraint = if (progressing) {
                ConstraintSet().apply {
                    clone(
                        this@RunServerActivity,
                        R.layout.activity_run_server_progress
                    )
                }
            } else if (started) c2 else c1

            constraint.applyTo(root)
        }
    }
}
