package id.linov.beats.udp

import android.util.Log.e
import id.linov.beats.Games
import id.linov.beats.Games.getBroadcastAddress
import id.linov.beats.Games.getIPAddress
import id.linov.beats.server.TCPServer
import id.linov.beatslib.CMD_SERVER_IP
import id.linov.beatslib.DataShare
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
        broadcastIP()
//        GlobalScope.async {
//            while (true) {
//                broadcastIP()
//            }
//        }.invokeOnCompletion {
//            e("UDP", "listening UDP:: COMPLETE, relisten..")
//            initReceiver()
//        }
    }

    fun broadcastIP() {
        GlobalScope.async {
            while (true) {
                e("TCP SERVER", "========= BROADCAST IP ============== ${getIPAddress()}")
                socket.broadcast = true
                val bytes = DataShare(CMD_SERVER_IP, getIPAddress()).toPayload().asBytes()
                e("TCP SERVER", "data: ${String(bytes!!)}")
                socket.send(DatagramPacket(bytes, bytes.size , getBroadcastAddress(), UDP_PORT))
                Thread.sleep(5000)
            }
        }.invokeOnCompletion {
            e("ERROR", "${it}")
            broadcastIP()
        }
    }

    private fun listenUdp() {
//        var bytes = ByteArray(2048)
//        var pkg = DatagramPacket(bytes, bytes.size)
//        socket.receive(pkg)
//        val data = String(pkg.data)
//        val cleanData = data.substring(0, data.lastIndexOf("}") +1)
//        e("UDP data", "$cleanData")
//        Games.handleData(cleanData, pkg.address.hostAddress)
    }

    fun sendPayload(bytes: ByteArray?, address: InetAddress = getBroadcastAddress()) {
        e("SEND UDP", "SEND UDP PACKAGE.... ${address.hostName}")
        socket.send(DatagramPacket(bytes, bytes?.size ?: 0, address, UDP_PORT))
    }
}