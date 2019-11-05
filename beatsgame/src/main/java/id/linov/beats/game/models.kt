package id.linov.beats.game

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.google.android.gms.nearby.connection.Payload
import com.google.gson.Gson
import id.linov.beats.game.contactor.GameContactor
import id.linov.beats.game.contactor.ServerContactor
import id.linov.beats.game.contactor.UDPContactor
import id.linov.beatslib.*

/**
 * Created by Hayi Nukman at 2019-10-19
 * https://github.com/ha-yi
 */

object Game {
    var tcpServerIP: String? = null
    var lastServerUptime: Long? = null
    val contactor: GameContactor = UDPContactor()
    var serverID: String? = null
    var userInformation: User? = null
    var gameType: GameType = GameType.PERSONAL
    var taskActions: MutableMap<Int, MutableList<Action>> = mutableMapOf()
    var actions: MutableList<Action> = mutableListOf()
    var taskID: Int = -1
    var selectedOpt: Colors = Colors.W
    var groupID: String? = ""
    var groupLeadID: String? = ""
    val myID: String? get() = userInformation?.userID
    var groupMembers: List<String>? = listOf()
    var application: Application? = null

    var myGroup: GroupData? = null

    var allGroups: List<GroupData>? = listOf()

    fun initApp(application: Application) {
        this.application = application
        getIPAddress()
    }

    fun getIPAddress(): String {
        val wifi = application?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifi.connectionInfo.ipAddress

        return Formatter.formatIpAddress(ip)
    }

    fun getColor(color: Colors) : Int {
        return when (color) {
            Colors.R -> R.color.col_r
            Colors.B -> R.color.col_b
            Colors.Y -> R.color.col_y
            Colors.W -> R.color.col_grey_1000w
            else -> R.color.col_grey_1000w
        }
    }

    fun getSelectedOptColor(): Int {
        return getColor(selectedOpt)
    }

    fun currentActionPayload(): Payload {
        return DataShare(CMD_GAME_DATA, GameData(userInformation, gameType, taskID, actions)).toPayload()
    }

    fun reset(type: GameType) {
        gameType = type
        taskActions = mutableMapOf()
        actions = mutableListOf()
        selectedOpt = Colors.W
        groupMembers = listOf()
    }

    fun isGroupLead(): Boolean {
        return gameType == GameType.PERSONAL || (gameType == GameType.GROUP && myID == groupLeadID)
    }
}

