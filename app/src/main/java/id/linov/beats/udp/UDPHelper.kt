package id.linov.beats.udp

import android.util.Log.e
import id.linov.beats.Games
import id.linov.beatslib.UDP_PORT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Created by Hayi Nukman at 2019-10-31
 * https://github.com/ha-yi
 */

object UDPHelper {
    val socket = DatagramSocket(UDP_PORT, InetAddress.getByName("0.0.0.0"))

    init {
        socket.broadcast = true
    }

    fun initReceiver() {
        GlobalScope.async {
            while (true) {
                e("UDP", "listening UDP....")
                listenUdp()
            }
        }.invokeOnCompletion {
            e("UDP", "listening UDP:: COMPLETE, relisten..")
            initReceiver()
        }
    }

    private fun listenUdp() {
        var bytes = ByteArray(2048)
        var pkg = DatagramPacket(bytes, bytes.size)
        socket.receive(pkg)
        val data = String(pkg.data)
        val cleanData = data.substring(0, data.lastIndexOf("}") +1)
        e("UDP data", "$cleanData")
        Games.handleData(cleanData, pkg.address.hostAddress)
    }
}