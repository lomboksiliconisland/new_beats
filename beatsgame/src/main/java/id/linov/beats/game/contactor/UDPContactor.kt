package id.linov.beats.game.contactor

import android.content.Context
import android.content.Intent
import android.util.Log.e
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.linov.beats.game.Game
import id.linov.beats.game.GameActivity
import id.linov.beats.game.GroupListener
import id.linov.beatslib.*
import id.linov.beatslib.interfaces.GameListener

/**
 * Created by Hayi Nukman at 2019-11-01
 * https://github.com/ha-yi
 */

class UDPContactor: GameContactor {

    override var groupListener: GroupListener? = null
    override var groupData: GroupData? = null
    override var gameDataListener: GameListener? = null

    var connector: TCPGameServerContactor? = null
    var context: Context? = null

    override fun init(context: Context) {
        connector = TCPGameServerContactor(context)
        this.context = context
    }

    override fun addUser() {
        connector?.sendToGameServer(
            "",
            DataShare(CMD_ADD_USER, Game.userInformation).toPayload()
        )
    }

    override fun getMyUID() {
        connector?.sendToGameServer(Game.serverID ?: "", DataShare(CMD_GET_MYUID, "").toPayload())
    }

    override fun startNewPersonalGame() {
        connector?.sendToGameServer(Game.serverID ?: "", DataShare(CMD_NEW_GAME, "").toPayload())
        context?.let {
            Game.reset(GameType.PERSONAL)
            it.startActivity(Intent(it, GameActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    override fun startNewGroupGame() {
        connector?.sendToGameServer(
            Game.serverID ?: "",
            DataShare(CMD_GROUP_GAME_NEW, Game.groupID).toPayload()
        )
    }

    override fun joinGroup(selectedGroup: GroupData) {
        connector?.sendToGameServer(
            Game.serverID ?: "",
            DataShare(CMD_JOIN_GROUP, selectedGroup.name).toPayload()
        )
    }

    override fun createGroup(group: String) {
        connector?.sendToGameServer(
            Game.serverID ?: "",
            DataShare(CMD_CREATE_GROUP, group).toPayload()
        )
    }

    override fun getGroups() {
        connector?.sendToGameServer(
            Game.serverID ?: "",
            DataShare(CMD_GET_GROUPS, "").toPayload()
        )
    }

    override fun leaveGroup() {
        connector?.sendToGameServer(
            Game.serverID ?: "",
            DataShare(CMD_GROUP_LEAVE, "").toPayload()
        )
    }

    override fun sendAction(action: ActionLog) {
        if (action.groupName!= null && action.type == GameType.GROUP) {
            Game.myGroup?.members?.filter { it != Game.userInformation?.userID }?.forEach {
               connector?.sendPayload(it, TCP_GAME_CLI_SRV_PORT, DataShare(CMD_GAME_DATA, action).toPayload())
            }
        }
        // also send to server
        connector?.sendToGameServer("", DataShare(CMD_GAME_DATA, action).toPayload())
    }

    override fun startNewTask(taskID: Int) {
        connector?.sendToGameServer(
            Game.serverID ?: "",
            DataShare(CMD_START_TASK, GroupTask(Game.groupID?: "", taskID)).toPayload()
        )
    }

    override fun finished() {
    }

    override fun connectToServer(onConnect: () -> Unit) {
        // since it UDP, assume connected
        onConnect.invoke()
    }

    override fun handleCommand(from: String, data: String) {
        val dt = Gson().fromJson(data, DataShare::class.java)
        when (dt?.command) {
            CMD_GET_GROUPS -> handleGroups(from, data)
            CMD_GET_CONFIG -> getConfig(from, data)
            CMD_GET_MYUID -> hanldeUser(from, data)
            CMD_GROUP_GAME -> handleGroupGameData(from, data)
            CMD_NEW_GAME -> handleNewGame(from, data)
            CMD_GAME_DATA -> handlePersonalGameData(from, data)
            CMD_GROUP_GAME_NEW -> handleOpenGroupGame(from, data)
            CMD_JOIN_GROUP -> handleGroupJoined(from, data)
            CMD_CREATE_GROUP -> handleGroupCreated(from, data)
            CMD_START_TASK -> handleStartTask(from, data)
            CMD_GROUP_NEW_MEMBER -> handleNewMemberJoined(from, data)
        }
    }

    private fun handleNewMemberJoined(from: String, data: String) {
//        val dttp = object : TypeToken<DataShare<List<String>>>() {}.type
//        val mmbrs = Gson().fromJson<DataShare<List<String>>>(data, dttp)?.data
//        mmbrs?.let {
//            groupListener?.onMembers(mmbrs)
//        }
        // do nothing...
    }

    private fun handleStartTask(from: String, data: String) {
        val tp = object : TypeToken<DataShare<Int>>() {}.type
        Gson().fromJson<DataShare<Int>>(data, tp)?.data?.let { gt ->
//            if (Game.groupID == gt.groupID) {
                gameDataListener?.onOpenTask(gt)
//            }
        }
    }

    private fun handleGroupCreated(from: String, data: String) {
//        val dttp = object : TypeToken<DataShare<String>>() {}.type
//        val groupID = Gson().fromJson<DataShare<String>>(data, dttp)?.data
//        Game.groupID = groupID
//        Game.groupLeadID = Game.myID
        // do nothing
    }

    private fun handleGroupJoined(from: String, data: String) {
//        val dttp = object : TypeToken<DataShare<String>>() {}.type
//        val groupID = Gson().fromJson<DataShare<String>>(data, dttp)?.data
//        Game.groupID = groupID
        // do nothing
    }

    private fun handleOpenGroupGame(from: String, data: String) {
//        e("PAYLOAD", "CMD_GROUP_GAME_NEW= ${data}")

        val dttp = object : TypeToken<DataShare<String>>() {}.type
        val groupID = Gson().fromJson<DataShare<String>>(data, dttp)?.data

        if (Game.groupID == groupID) {
            context?.let {
                Game.reset(GameType.GROUP)
                it.startActivity(Intent(it, GameActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    private fun handlePersonalGameData(from: String, data: String) {
//        e("saveGameData", data)
        val tp = object : TypeToken<DataShare<ActionLog>>() {}.type
        val data = Gson().fromJson<DataShare<ActionLog>>(data, tp)?.data
//        e("saveGameData::type", data?.type.toString())
        if (data?.type == GameType.GROUP && data.groupName == Game.groupID) {
            gameDataListener?.onGameData(data)
        }
    }

    private fun handleNewGame(from: String, data: String) {
//        e("NEW GAME", "New game created :$from")
//        context?.let {
//            Game.reset(GameType.PERSONAL)
//            it.startActivity(Intent(it, GameActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            })
//        }
    }

    private fun handleGroupGameData(from: String, data: String) {
        if (groupListener != null || groupData == null) {
            // todo still in group page
            return
        }
        val dttp = object : TypeToken<DataShare<ActionLog>>() {}.type
        val session = Gson().fromJson<DataShare<ActionLog>>(data, dttp).data
        if (session.groupName == Game.groupID) {
            gameDataListener?.onGameData(session)
        }
    }

    private fun hanldeUser(from: String, data: String) {
//        val userID = Gson().fromJson<DataShare<String>>(data, DataShare::class.java).data
//        Game.userInformation?.userID = userID
    }

    private fun getConfig(from: String, data: String) {

    }

    private fun handleGroups(from: String, str: String) {
        val dttp = object : TypeToken<DataShare<List<GroupData>>>() {}.type
        val dt = Gson().fromJson<DataShare<List<GroupData>>>(str, dttp)
//        dt?.data?.forEach {
//            e("RECEIVED FROM SERVER", "name: ${it.name} # members: ${it.members?.joinToString()}")
//        }
        Game.allGroups = dt?.data
        Game.myGroup = Game.allGroups?.filter { it.members?.contains(Game.userInformation?.userID) == true }?.firstOrNull()

        groupListener?.onData(dt)
    }

}