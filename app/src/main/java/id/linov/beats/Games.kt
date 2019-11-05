package id.linov.beats

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log.e
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.linov.beats.udp.UDPHelper
import id.linov.beatslib.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.net.InetAddress
import android.text.format.Formatter
import android.util.Base64
import id.linov.beats.server.TCPServer
import id.linov.beatslib.GameSession
import id.linov.beatslib.GroupData


@SuppressLint("StaticFieldLeak")
object Games {
    var serverUUID: String? = null
    var ctx: Context? = null
    var con: ConnectionInfo? = null
    private var updateListener: (() -> Unit)? = null


    // group name to list of members
    val groups: MutableMap<String, GroupData> = mutableMapOf()
    val personalData: MutableMap<String, MutableMap<Int, List<Action>>> = mutableMapOf()
    val users: MutableMap<String, User> = mutableMapOf()

    val gameSessions: MutableMap<String, GameSession> = mutableMapOf()
    val groupSessions: MutableMap<String, GameSession> = mutableMapOf()

    fun init(context: Context, updateListener: () -> Unit) {
        ctx = context
        this.updateListener = updateListener
        val base64 = Base64.encode(System.currentTimeMillis().toString().toByteArray(), Base64.DEFAULT)
        serverUUID = String(base64)
    }

    fun getIPAddress(): String {
        val wifi = ctx?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifi.connectionInfo.ipAddress

        return Formatter.formatIpAddress(ip)
    }

    fun getBroadcastAddress(): InetAddress {
        val wifi = ctx?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        // handle null somehow

        val broadcast = dhcp.ipAddress and dhcp.netmask or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3)
            quads[k] = (broadcast shr k * 8 and 0xFF).toByte()
        return InetAddress.getByAddress(quads)
    }

    fun handleData(str: String, user: String) {
        e("PAYLOAD", "FROM $user $str")
        val dt = Gson().fromJson<DataShare<Any>>(str, DataShare::class.java)

        when (dt?.command) {
            CMD_GAME_DATA -> saveGameData(user, str)
            CMD_CREATE_GROUP -> createGroup(user, str)
            CMD_GET_GROUPS -> getGroups(user)
            CMD_JOIN_GROUP -> joinGroup(user, str)
            CMD_GET_MYUID -> handleGetUID(user)
            CMD_ADD_USER -> addUser(user, str)
            CMD_NEW_GAME -> handleNewGame(user)
            CMD_GROUP_GAME_NEW -> handleCreateGroupGame(user, str)
            CMD_START_TASK -> broadcastTaskID(user, str)
            CMD_GROUP_LEAVE -> leaveGroup(user)
        }
    }

    fun save(user: String, p: Payload) {
        if (p.type == Payload.Type.BYTES) {
            p.asBytes()?.let {
                val str = String(it)
                handleData(str, user)
            }
        }
    }

    fun leaveGroup(user: String) {
        val grp = users[user]?.groupID
        grp?.let {
            groups[grp]?.members?.remove(user)
            if (groups[grp]?.members.isNullOrEmpty()) {
                groups.remove(grp)
            } else if (groups[grp]?.leadID == user) {
                groups[grp]?.leadID = groups[grp]?.members?.firstOrNull() ?: ""
            }
//            send(users.map { it.key }, groups.values.toList(), CMD_GET_GROUPS)
            users[user]?.groupID = null
            getGroups(user)
        }
    }

    private fun broadcastTaskID(user: String, str: String) {
        val tp = object : TypeToken<DataShare<GroupTask>>() {}.type
        val grpTask = Gson().fromJson<DataShare<GroupTask>>(str, tp)?.data
        grpTask?.let { task ->
            groups[task.groupID]?.members?.toList()?.forEach {
                send(it, task.taskID , CMD_START_TASK)
            }
        }
    }

    private fun handleCreateGroupGame(user: String, str: String) {
        e("CMD_GROUP_GAME_NEW", "Start game $user")
        val tp = object : TypeToken<DataShare<String>>() {}.type
        val grpID = Gson().fromJson<DataShare<String>>(str, tp).data
        groups[grpID]?.members?.toList()?.forEach {
            send(it, grpID, CMD_GROUP_GAME_NEW)
        }

//        ctx?.let {
//            val dts = groups[grpID]?.members?.toList() ?: listOf()
//            e("CMD_GROUP_GAME_NEW", "start game on all:  ${dts.joinToString()}")
//            Nearby.getConnectionsClient(it).sendPayload(dts, DataShare(CMD_GROUP_GAME_NEW, dts).toPayload())
//
//
////            send(user, grpID, CMD_GET_GROUPS)
//        }
    }

    private fun handleNewGame(user: String) {
        // force replace game session.
        gameSessions[user] =
            GameSession(user, GameType.PERSONAL, startTime = System.currentTimeMillis())
//        ctx?.let {
//            Nearby.getConnectionsClient(it).sendPayload(user, DataShare(CMD_NEW_GAME, user).toPayload())
//        }
        send(user, user, CMD_NEW_GAME)
    }

    private fun addUser(user: String, str: String) {
        val tp = object : TypeToken<DataShare<User>>() {}.type
        val data = Gson().fromJson<DataShare<User>>(str, tp)
        e("ADD USER", " $user -- $str --")
        e("ADD USER", "$data")
        if (data?.data != null) {
            users[user] = data.data.apply {
                userID = user
            }
            e("USERS", "$users")
            GlobalScope.launch(Dispatchers.Main) {
                updateListener?.invoke()
            }
        }
    }

    private fun handleGetUID(user: String) {
//        ctx?.let {
//            Nearby.getConnectionsClient(it)
//                .sendPayload(user, DataShare(CMD_GET_MYUID, user).toPayload())
//        }
    }

    private fun <T> send(user: String, data: T, cmd: Int) {
        e("SEND DATA", "SEND UDP PACKAGE.... $user $data")
        TCPServer.sendToClient(user, DataShare(cmd, data))
    }

    private fun joinGroup(user: String, str: String) {
        val tp = object : TypeToken<DataShare<String>>() {}.type
        val data = Gson().fromJson<DataShare<String>>(str, tp)?.data
        data?.let {
            groups[it]?.members?.add(user)
            users[user]?.groupID = data
            // send response groups.
            getGroups(user)
            groups[it]?.let { g ->
                send(g.leadID, g.members, CMD_GROUP_NEW_MEMBER)
            }
        }
        getGroups(user)
        GlobalScope.launch(Dispatchers.Main) {
            updateListener?.invoke()
        }
    }

    private fun getGroups(user: String) {
        // notify all clients
        users.forEach {
            send(it.key, groups.values.toList(), CMD_GET_GROUPS)
        }
    }

    private fun createGroup(user: String, str: String) {
        e("SERVER", "create group: $str owner $user")
        val tp = object : TypeToken<DataShare<String>>() {}.type
        val data = Gson().fromJson<DataShare<String>>(str, tp)?.data
        data?.let {
            groups.put(
                it, GroupData(
                    it,
                    user,
                    mutableSetOf(user),
                    users[user]?.name
                )
            )
            e("SERVER", "new group created ")
            users[user]?.groupID = data
            users[user]?.isGroupOwner = true

            getGroups(user)
        }
        GlobalScope.launch(Dispatchers.Main) {
            updateListener?.invoke()
        }
    }

    private fun saveGameData(user: String, str: String) {
        e("saveGameData", str)
        val tp = object : TypeToken<DataShare<ActionLog>>() {}.type
        val data = Gson().fromJson<DataShare<ActionLog>>(str, tp)?.data
        e("saveGameData::type", data?.type.toString())
        if (data != null) {
            when (data.type) {
                GameType.PERSONAL -> savePersonalGameData(user, data)
                GameType.GROUP -> saveGroupGameData(user, data)
            }
        }
    }

    private fun saveGroupGameData(user: String, data: ActionLog) {
        data.groupName?.let { groupID ->
            if (groupSessions[groupID] == null) {
                groupSessions[groupID] = GameSession(
                    user,
                    GameType.GROUP,
                    startTime = System.currentTimeMillis(),
                    groupName = groupID
                )
            }
            groupSessions[groupID]?.saveActionLog(data)
//            groups[groupID]?.members?.toList()?.forEach {
//                send(it, data, CMD_GROUP_GAME)
//            }

            Servers.addBlock(BlockPojo().apply {
                assessmentId = "G-${serverUUID}-${groupID}"
                taskId = data.taskID.toString()
                color = getColorName(data.action.tile.color)
                timestamp = data.action.tile.timestamp.toString()
                pos_x = data.action.x.toString()
                pos_y = data.action.y.toString()
                username = users[user]?.name ?: user
            })
        }
    }

    private fun savePersonalGameData(user: String, data: ActionLog) {
        if (gameSessions[user] == null) {
            handleNewGame(user)
        }
        gameSessions[user]?.saveActionLog(data)
        Servers.addBlock(BlockPojo().apply {
            assessmentId = "P-${serverUUID}-${user}_${users[user]?.name}"
            taskId = data.taskID.toString()
            color = getColorName(data.action.tile.color)
            timestamp = data.action.tile.timestamp.toString()
            pos_x = data.action.x.toString()
            pos_y = data.action.y.toString()
            username = users[user]?.name ?: user
        })
    }

    fun clearAll() {
        groups.clear()
        personalData.clear()
        users.clear()
        gameSessions.clear()
        groupSessions.clear()
    }
}