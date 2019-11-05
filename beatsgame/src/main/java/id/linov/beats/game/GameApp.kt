package id.linov.beats.game

import android.app.Application
import id.linov.beats.game.contactor.GameTileServer

/**
 * Created by Hayi Nukman at 2019-11-05
 * https://github.com/ha-yi
 */

class GameApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Game.initApp(this)
        GameTileServer.runServer()
    }

    override fun onTerminate() {
        GameTileServer.stop()
        super.onTerminate()
    }
}