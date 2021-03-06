package com.codejoust.main.service;

import com.codejoust.main.game_object.Player;
import com.codejoust.main.game_object.PlayerCode;

import org.springframework.stereotype.Service;

/**
 * Class to handle code updates and miscellaneous requests.
 */
@Service
public class LiveGameService {

    // Update a specific player's code.
    public void updateCode(Player player, PlayerCode playerCode) {
        player.setPlayerCode(playerCode);
    }

}
