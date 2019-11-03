package id.linov.beats.game.contactor

import android.content.Context
import com.google.android.gms.nearby.connection.Payload
import java.net.DatagramSocket
import android.net.wifi.WifiManager
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.PayloadCallback
import id.linov.beatslib.UDP_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress


/**
 * Created by Hayi Nukman at 2019-10-31
 * https://github.com/ha-yi
 */

class UDPConnector(val context: Context) {
    val socket = DatagramSocket()

    init {
        socket.broadcast = true
    }

    fun sendPayload(s: String = "", payload: Payload) {
        GlobalScope.async {
            withContext(Dispatchers.IO) {
                val bytes = payload.asBytes()
                socket.send(DatagramPacket(bytes, bytes?.size ?: 0, getBroadcastAddress(), UDP_PORT))
            }
        }
    }

    fun getBroadcastAddress(): InetAddress {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        // handle null somehow

        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3)
            quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    fun acceptConnection(p0: String, payloadServerCallback: PayloadCallback) {

    }

    fun requestConnection(s: String, s1: String, connCallback: ConnectionLifecycleCallback) {

    }

    fun sendPayload(s: MutableList<String>, payload: Payload) {
        val bytes = payload.asBytes()
        socket.send(DatagramPacket(bytes, bytes?.size ?: 0, getBroadcastAddress(), UDP_PORT))
    }
}