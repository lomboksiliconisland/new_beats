package id.linov.beats.game.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import id.linov.beats.game.R
import kotlinx.android.synthetic.main.fragment_game.*
import id.linov.beats.game.Game
import id.linov.beatslib.GameType.PERSONAL
import id.linov.beats.game.GameActivity
import id.linov.beats.game.GroupActivity
import id.linov.beatslib.GameType.GROUP

/**
 * Created by Hayi Nukman at 2019-10-20
 * https://github.com/ha-yi
 */

class HomeGameFrag: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llGroup.setOnClickListener {
            startGroupTest()
        }

        llPersonal.setOnClickListener {
            startPersonalTest()
        }
    }

    private fun startPersonalTest() {
        Game.reset(PERSONAL)
        startActivity(Intent(context, GameActivity::class.java))
    }

    private fun startGroupTest() {
        Game.reset(GROUP)
        startActivity(Intent(context, GroupActivity::class.java))
    }
}