package id.linov.beats.server

import android.util.Log.e
import id.linov.beats.Games
import id.linov.beatslib.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket

/**
 * Created by Hayi Nukman at 2019-11-05
 * https://github.com/ha-yi
 */

object TCPServer {
    val server = ServerSocket(TCP_PORT)

    fun startServer() {
        startTCPServer()
    }

    private fun startTCPServer() {
        e("TCP SERVER", "=======START TCP SERVER =======")
        GlobalScope.async {
            while (true) {
                e("TCP SERVER", "======= AWAIT CLIENT =======")
                val socket = server.accept()
                val addr = socket.inetAddress.hostAddress
                val port = socket.port
                val output = PrintWriter(socket.getOutputStream())
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val data = input.readLine()
                e("TCP SERVER", "received from $addr port: $port --> $data")
                val cleanData = data.substring(0, data.lastIndexOf("}") + 1)
                e("TCP SERVER", "$cleanData")
                Games.handleData(cleanData, addr)
            }
        }.invokeOnCompletion {
            e("TCPServer::ERROR", it.toString())
            startTCPServer()
        }
    }

    fun sendToClient(addr: String, dataShare: DataShare<*>) {
        GlobalScope.async {
            e("SEND PAYLOAD TO CLIENT", "CLIENT IP ${addr}")
            val server = Socket(addr, TCP_GAME_CLI_SRV_PORT)
            var error = false
            try {
                e("SEND PAYLOAD TO CLIENT", "$dataShare")
                val os = server.getOutputStream()
                val bytes = dataShare.toPayload().asBytes() ?: byteArrayOf()
                os.write(bytes)
            } catch (ex: Exception) {
                e("SEND PAYLOAD ERROR", ex.toString())
                error = true
            }
            server.close()
            if (error) sendToClient(addr, dataShare)
        }.invokeOnCompletion {
            e("sendToClient", "${it}")
        }
    }
}