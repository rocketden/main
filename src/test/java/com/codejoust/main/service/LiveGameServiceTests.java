package com.codejoust.main.service;

import com.codejoust.main.util.TestFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codejoust.main.dto.game.GameMapper;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.Player;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;

@ExtendWith(MockitoExtension.class)
public class LiveGameServiceTests {

    @Spy
    @InjectMocks
    private LiveGameService liveGameService;

    @Test
    public void updateCodeSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        room.addUser(user);

        Game game = GameMapper.fromRoom(room);
        Player player = game.getPlayers().get(TestFields.USER_ID);
        liveGameService.updateCode(player, TestFields.PLAYER_CODE_1);

        assertEquals(TestFields.PLAYER_CODE_1, player.getPlayerCode());
    }
}
