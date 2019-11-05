package id.linov.beats.game.contactor

import android.content.Context
import com.google.android.gms.nearby.connection.Payload
import java.net.DatagramSocket
import android.net.wifi.WifiManager
import android.util.Log
import android.util.Log.e
import android.widget.Toast
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.gson.Gson
import id.linov.beats.game.Game
import id.linov.beatslib.CMD_SERVER_IP
import id.linov.beatslib.DataShare
import id.linov.beatslib.UDP_PORT
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.InetAddress


/**
 * Created by Hayi Nukman at 2019-10-31
 * https://github.com/ha-yi
 */

object UDPBroadcastListener {
    private val broadcastSocket = DatagramSocket(UDP_PORT, InetAddress.getByName("0.0.0.0"))

    init {
//        socket.broadcast = true
        broadcastSocket.broadcast = true
        startListen()
    }
//
//    fun sendToGameServer(s: String = "", payload: Payload) {
//        GlobalScope.async {
//            withContext(Dispatchers.IO) {
//                val bytes = payload.asBytes()
//                socket.send(DatagramPacket(bytes, bytes?.size ?: 0, getBroadcastAddress(), UDP_PORT))
//            }
//        }
//    }
//
//    fun getBroadcastAddress(): InetAddress {
//        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val dhcp = wifi.dhcpInfo
//        // handle null somehow
//
//        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
//        val quads = ByteArray(4)
//        for (k in 0..3)
//            quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
//        return InetAddress.getByAddress(quads)
//    }
//
//    fun acceptConnection(p0: String, payloadServerCallback: PayloadCallback) {
//
//    }
//
//    fun requestConnection(s: String, s1: String, connCallback: ConnectionLifecycleCallback) {
//
//    }
//
//    fun sendToGameServer(s: MutableList<String>, payload: Payload) {
//        val bytes = payload.asBytes()
//        socket.send(DatagramPacket(bytes, bytes?.size ?: 0, getBroadcastAddress(), UDP_PORT))
//    }

    fun startListen() {
        e("LISTEN UDP", "LISTEN UDP BROADCAST")
        GlobalScope.async {
            while (true) {
                listenUDPPackage()
            }
        }.invokeOnCompletion {
            e("ERROR", "${it}")
            startListen()
        }
    }

    private fun listenUDPPackage() {
        var bytes = ByteArray(2048)
        var pkg = DatagramPacket(bytes, bytes.size)
        broadcastSocket.receive(pkg)
        val data = String(pkg.data)
        val cleanData = data.substring(0, data.lastIndexOf("}") +1)
        e("UDP data from other", "${pkg.address.hostAddress}: $cleanData")
        val dt = Gson().fromJson(cleanData, DataShare::class.java)

        if (dt.command == CMD_SERVER_IP) {
            if (Game.tcpServerIP.isNullOrBlank()) {
                Game.tcpServerIP = pkg.address.hostAddress
                GlobalScope.async {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            Game.application?.applicationContext,
                            "Connected to server: ${Game.tcpServerIP}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            if (Game.tcpServerIP == pkg.address.hostAddress) {
                Game.lastServerUptime = System.currentTimeMillis()
            }
        }
    }
}