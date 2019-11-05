package id.linov.beats.game.contactor

import android.util.Log.e
import id.linov.beats.game.Game
import id.linov.beatslib.TCP_GAME_CLI_SRV_PORT
import id.linov.beatslib.TCP_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.net.ServerSocket

/**
 * Created by Hayi Nukman at 2019-11-05
 * https://github.com/ha-yi
 */

object GameTileServer {
    private var server = ServerSocket(TCP_GAME_CLI_SRV_PORT)
    private var stopped = false

    fun runServer() {
        stopped = false
        listenData()
    }

    private fun listenData() {
        if (stopped) return
        if (server.isClosed) {
            server = ServerSocket(TCP_GAME_CLI_SRV_PORT)
        }
        GlobalScope.async {
            while (!stopped) {
                try {
                    val socket = server.accept()
                    val addr = socket.inetAddress.hostAddress
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val data = input.readLine()
                    val cleanData = data.substring(0, data.lastIndexOf("}") + 1)
                    GlobalScope.launch(Dispatchers.Main) {
                        Game.contactor.handleCommand(addr, cleanData)
                    }
                } catch (ex: Exception) {
                    e("GameTileServer", "STOPPED... $ex")
                }
            }
        }.invokeOnCompletion {
            listenData()
        }
    }

    fun stop() {
        stopped = true
        server.close()
    }
}