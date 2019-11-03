package id.linov.beats.game.contactor

import android.content.Context
import id.linov.beats.game.GroupListener
import id.linov.beatslib.ActionLog
import id.linov.beatslib.GroupData
import id.linov.beatslib.User
import id.linov.beatslib.interfaces.GameListener

/**
 * Created by Hayi Nukman at 2019-10-31
 * https://github.com/ha-yi
 */

interface GameContactor {
    var groupListener: GroupListener?
    var groupData: GroupData?
    var gameDataListener: GameListener?

    fun init(context: Context)
    fun addUser()
    fun getMyUID()
    fun startNewPersonalGame()

    fun startNewGroupGame()
    // groups actions
    fun joinGroup(selectedGroup: GroupData)
    fun createGroup(group: String)
    fun getGroups()
    fun leaveGroup()

    // game actions
    fun sendAction(action: ActionLog)
    fun startNewTask(taskID: Int)
    fun finished()

    fun connectToServer(onConnect: () -> Unit)
}