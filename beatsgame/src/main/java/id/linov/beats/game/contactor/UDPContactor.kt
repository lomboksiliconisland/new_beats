package id.linov.beats.game.contactor

import android.content.Context
import android.content.Intent
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

    var connector: UDPConnector? = null
    var context: Context? = null

    override fun init(context: Context) {
        connector = UDPConnector(context)
        this.context = context
    }

    override fun addUser() {
        connector?.sendPayload(
            "",
            DataShare(CMD_ADD_USER, Game.userInformation).toPayload()
        )
    }

    override fun getMyUID() {
    }

    override fun startNewPersonalGame() {
        connector?.sendPayload(Game.serverID ?: "", DataShare(CMD_NEW_GAME, "").toPayload())
        context?.let {
            Game.reset(GameType.PERSONAL)
            it.startActivity(Intent(it, GameActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    override fun startNewGroupGame() {
    }

    override fun joinGroup(selectedGroup: GroupData) {
    }

    override fun createGroup(group: String) {
    }

    override fun getGroups() {
    }

    override fun leaveGroup() {
    }

    override fun sendAction(action: ActionLog) {
        connector?.sendPayload("", DataShare(CMD_GAME_DATA, action).toPayload())
    }

    override fun startNewTask(taskID: Int) {
    }

    override fun finished() {
    }

    override fun connectToServer(onConnect: () -> Unit) {
        // since it UDP, assume connected
        onConnect.invoke()
    }
}