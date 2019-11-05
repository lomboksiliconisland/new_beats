package id.linov.beats.game.contactor

import android.content.Context
import android.util.Log.e
import com.google.android.gms.nearby.connection.Payload
import id.linov.beats.game.Game
import id.linov.beatslib.TCP_PORT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.Exception
import java.net.Socket

/**
 * Created by Hayi Nukman at 2019-11-05
 * https://github.com/ha-yi
 */

class TCPGameServerContactor(context: Context) {
    fun sendToGameServer(s: String, payload: Payload) {
        // on response
        sendPayload(Game.tcpServerIP ?: "192.168.43.1", TCP_PORT, payload)
    }

    fun sendPayload(s: String?, port: Int, payload: Payload) {
        GlobalScope.async {
//            e("SEND PAYLOAD TO SERVER", "RECEIVER IP $s:$port")
            val server = Socket(s, port)
            var error = false
            try {
//                e("SEND PAYLOAD", String(payload.asBytes()?: byteArrayOf()))
                val os = server.getOutputStream()
                val bytes = payload.asBytes() ?: byteArrayOf()
                os.write(bytes)
            } catch (ex: Exception) {
                e("SEND PAYLOAD ERROR", ex.toString())
                error = true
            }
            server.close()
            if (error) sendPayload(s, port, payload)
        }.invokeOnCompletion {
            e("sendToGameServer", "${it}")
        }
    }
}