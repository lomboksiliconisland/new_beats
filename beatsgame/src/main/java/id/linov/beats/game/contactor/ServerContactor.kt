package id.linov.beats.game.contactor

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.util.Log.e
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import id.linov.beats.game.Game
import id.linov.beats.game.GroupListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import id.linov.beatslib.*
import com.google.gson.reflect.TypeToken
import id.linov.beats.game.GameActivity
import id.linov.beatslib.interfaces.GameListener
import java.lang.Exception


class ServerContactor: GameContactor {
    var connection: ConnectionsClient? = null
    override var groupListener: GroupListener? = null
    var appContext: Context? = null
    override var groupData: GroupData? = null
    override var gameDataListener: GameListener? = null

    override fun init(context: Context) {
        appContext = context
        connection = Nearby.getConnectionsClient(context)
    }

    override fun getMyUID() {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_GET_MYUID, "").toPayload())
    }

    val payloadServerCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            e("PAYLOAD", "from $p0 : data --> ${p1}")
            hanldePayload(p0, p1)
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            e("onPayloadTransferUpdate", "from $p0 : data --> ${p1}")
        }
    }

    private fun hanldePayload(user: String, data: Payload) {
        when (data.type) {
            Payload.Type.BYTES -> {
                data.asBytes()?.let {
                    val str = String(it)
                    e("PAYLOAD", "FROM $user $str")
                    handleCommand(user, str)
                }
            }
        }
    }

    override fun handleCommand(from: String, data: String) {
        val dt = Gson().fromJson(data, DataShare::class.java)
        when (dt?.command) {
            CMD_GET_GROUPS -> handleGroups(from, data)
            CMD_GET_CONFIG -> getConfig(from, data)
            CMD_GET_MYUID -> hanldeUser(from, data)
            CMD_GROUP_GAME -> handleGroupGameData(from, data)
            CMD_NEW_GAME -> handleNewGame(from)
            CMD_GAME_DATA -> handlePersonalGameData(from, data)
            CMD_GROUP_GAME_NEW -> handleOpenGroupGame(data)
            CMD_JOIN_GROUP -> handleGroupJoined(data)
            CMD_CREATE_GROUP -> handleGroupCreated(data)
            CMD_START_TASK -> handleStartTask(data)
            CMD_GROUP_NEW_MEMBER -> handleNewMemberJoined(data)
        }
    }

    private fun handleGroupCreated(str: String) {
        val dttp = object : TypeToken<DataShare<String>>() {}.type
        val groupID = Gson().fromJson<DataShare<String>>(str, dttp)?.data
        Game.groupID = groupID
        Game.groupLeadID = Game.myID
    }

    private fun handleNewMemberJoined(str: String) {
        val dttp = object : TypeToken<DataShare<List<String>>>() {}.type
        val mmbrs = Gson().fromJson<DataShare<List<String>>>(str, dttp)?.data
        mmbrs?.let {
            groupListener?.onMembers(mmbrs)
        }
    }

    private fun handleStartTask(str: String) {
        val dttp = object : TypeToken<DataShare<Int>>() {}.type
        val taskID = Gson().fromJson<DataShare<Int>>(str, dttp)?.data
        gameDataListener?.onOpenTask(taskID)
    }

    private fun handleGroupJoined(str: String) {
        val dttp = object : TypeToken<DataShare<String>>() {}.type
        val groupID = Gson().fromJson<DataShare<String>>(str, dttp)?.data
        Game.groupID = groupID
    }

    private fun handleOpenGroupGame(data: String) {
        e("PAYLOAD", "CMD_GROUP_GAME_NEW= ${data}")
        val dttp = object : TypeToken<DataShare<List<String>>>() {}.type
        val members = Gson().fromJson<DataShare<List<String>>>(data, dttp)?.data

        tryConnectAllMember(members)

        appContext?.let {
            Game.reset(GameType.GROUP)
            Game.groupMembers = members
            it.startActivity(Intent(it, GameActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    private fun tryConnectAllMember(members: List<String>?) {
        members?.forEach {
            val connCallback = object : ConnectionLifecycleCallback() {
                override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
                    e("SUCCESS", "onConnectionResult $p0")
                }

                override fun onDisconnected(p0: String) {
                    e("SUCCESS", "onDisconnected $p0")
                }

                override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                    e("SUCCESS", "accept member $p0")
                    connection?.acceptConnection(p0, payloadServerCallback)
                }
            }
            connection?.requestConnection(Game.userInformation?.name ?: "", it, connCallback)?.addOnFailureListener { e ->
                e("failed", "failed pairing to member  $it $e")
            }
        }
    }

    private fun handlePersonalGameData(user: String, str: String) {
        val dttp = object : TypeToken<DataShare<ActionLog>>() {}.type
        val session = Gson().fromJson<DataShare<ActionLog>>(str, dttp).data
        try {
            gameDataListener?.onGameData(session)
        } catch (e: Exception) {}
    }

    private fun handleNewGame(user: String) {
        e("NEW GAME", "New game created :$user")
        appContext?.let {
            Game.reset(GameType.PERSONAL)
            it.startActivity(Intent(it, GameActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    private fun handleGroupGameData(user: String, str: String) {
        if (groupListener != null || groupData == null) {
            // todo still in group page
            return
        }
        val dttp = object : TypeToken<DataShare<ActionLog>>() {}.type
        val session = Gson().fromJson<DataShare<ActionLog>>(str, dttp).data
        gameDataListener?.onGameData(session)
    }

    private fun hanldeUser(user: String, str: String) {
        val userID = Gson().fromJson<DataShare<String>>(str, DataShare::class.java).data
        Game.userInformation?.userID = userID
    }

    private fun getConfig(user: String, str: String) {
        // todo get configs
    }

    private fun handleGroups(user: String, str: String) {
        val dttp = object : TypeToken<DataShare<List<GroupData>>>() {}.type
        val dt = Gson().fromJson<DataShare<List<GroupData>>>(str, dttp)
        dt?.data?.forEach {
            e("RECEIVED FROM SERVER", "name: ${it.name} # members: ${it.members?.joinToString()}")
        }
        groupListener?.onData(dt)
    }

    override fun connectToServer(onConnect: () -> Unit) {
        val connCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
                e("SUCCESS", "onConnectionResult $p0")
            }

            override fun onDisconnected(p0: String) {
                e("SUCCESS", "onDisconnected $p0")
            }

            override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                e(
                    "INITIALIZED",
                    "connection initilized, $p0 ${p1.endpointName} ${p1.authenticationToken}  ${p1.isIncomingConnection} "
                )
                connection?.acceptConnection(p0, payloadServerCallback)
                onConnect.invoke()
            }
        }
        connection?.requestConnection(
            Game.userInformation?.name ?: "",
            Game.serverID ?: "",
            connCallback
        )?.addOnSuccessListener {
            e("SUCCESS", "Connected to server.....")
        }?.addOnFailureListener {
            e("FAILED", "$it")
            if (it.message?.contains("STATUS_ALREADY_CONNECTED_TO_ENDPOINT") == true) {
                onConnect.invoke()
            } else {
                GlobalScope.async {
                    Thread.sleep(2000)
                    connectToServer(onConnect)
                }
            }
        }
    }

    override fun sendAction(actionLog: ActionLog) {
        var recipient:MutableList<String> = (if (Game.groupID != null) Game.groupMembers ?: listOf() else listOf()).toMutableList()
        recipient.add(Game.serverID ?: "")
        e("Sending to ", "members ${recipient.joinToString()}")

        connection?.sendPayload(recipient, DataShare(CMD_GAME_DATA, actionLog).toPayload())
    }

    override  fun startNewPersonalGame() {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_NEW_GAME, "").toPayload())
    }

    override fun createGroup(name: String) {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_CREATE_GROUP, name).toPayload())
    }

    fun groupListener(listener: GroupListener) {
        groupListener = listener
    }

    fun removeGroupListenet() {
        groupListener = null
    }

    override fun joinGroup(selectedGroup: GroupData) {
        connection?.sendPayload(
            Game.serverID ?: "",
            DataShare(CMD_JOIN_GROUP, selectedGroup.name).toPayload()
        )
    }

    override fun getGroups() {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_GET_GROUPS, "").toPayload())
    }

    override fun addUser() {
        connection?.sendPayload(
            Game.serverID ?: "",
            DataShare(CMD_ADD_USER, Game.userInformation).toPayload()
        )
    }

    override fun startNewGroupGame() {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_GROUP_GAME_NEW, Game.groupID).toPayload())
    }

    override fun startNewTask(taskID: Int) {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_START_TASK, GroupTask(Game.groupID?: "", taskID)).toPayload())
    }

    override fun finished() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun leaveGroup() {
        connection?.sendPayload(Game.serverID ?: "", DataShare(CMD_GROUP_LEAVE, "").toPayload())
    }
}