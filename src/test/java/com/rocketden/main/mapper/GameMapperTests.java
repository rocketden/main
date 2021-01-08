package com.rocketden.main.mapper;

import com.rocketden.main.dto.game.GameDto;
import com.rocketden.main.dto.game.GameMapper;
import com.rocketden.main.dto.game.PlayerDto;
import com.rocketden.main.dto.game.SubmissionDto;
import com.rocketden.main.dto.room.RoomMapper;
import com.rocketden.main.dto.user.UserMapper;
import com.rocketden.main.game_object.CodeLanguage;
import com.rocketden.main.game_object.Game;
import com.rocketden.main.game_object.Player;
import com.rocketden.main.game_object.PlayerCode;
import com.rocketden.main.game_object.Submission;
import com.rocketden.main.model.Room;
import com.rocketden.main.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class GameMapperTests {

    private static final String ROOM_ID = "012345";
    private static final String USER_ID = "098765";
    private static final String NICKNAME = "test";
    private static final String CODE = "print('hi')";
    private static final CodeLanguage LANGUAGE = CodeLanguage.PYTHON;
    private static final int TEST_CASES = 10;

    @Test
    public void fromRoom() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);
        
        Game game = GameMapper.fromRoom(room);

        assertEquals(room, game.getRoom());
        assertNotNull(game.getPlayers().get(USER_ID));
        assertEquals(user, game.getPlayers().get(USER_ID).getUser());
    }

    @Test
    public void playerFromUser() {
        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);

        Player player = GameMapper.playerFromUser(user);

        assertEquals(user, player.getUser());
        assertNull(player.getPlayerCode());
        assertFalse(player.getSolved());
        assertEquals(0, player.getSubmissions().size());
    }

    @Test
    public void toDto() {
        Room room = new Room();
        room.setRoomId(ROOM_ID);

        User user = new User();
        user.setNickname(NICKNAME);
        user.setUserId(USER_ID);
        room.addUser(user);

        Game game = GameMapper.fromRoom(room);

        PlayerCode playerCode = new PlayerCode();
        playerCode.setCode(CODE);
        playerCode.setLanguage(LANGUAGE);

        Submission submission = new Submission();
        submission.setPlayerCode(playerCode);
        submission.setNumTestCases(TEST_CASES);
        submission.setNumCorrect(TEST_CASES);

        Player player = game.getPlayers().get(USER_ID);
        player.setSolved(true);
        player.setPlayerCode(playerCode);
        player.getSubmissions().add(submission);

        GameDto gameDto = GameMapper.toDto(game);

        assertEquals(RoomMapper.toDto(room), gameDto.getRoom());
        assertEquals(1, gameDto.getPlayers().size());

        PlayerDto playerDto = gameDto.getPlayers().get(0);
        assertEquals(UserMapper.toDto(user), playerDto.getUser());
        assertEquals(player.getSolved(), playerDto.getSolved());
        assertEquals(playerCode.getCode(), playerDto.getCode());
        assertEquals(playerCode.getLanguage(), playerDto.getLanguage());
        assertEquals(1, playerDto.getSubmissions().size());

        SubmissionDto submissionDto = playerDto.getSubmissions().get(0);
        assertEquals(submission.getPlayerCode().getCode(), submissionDto.getCode());
        assertEquals(submission.getPlayerCode().getLanguage(), submissionDto.getLanguage());
        assertEquals(submission.getNumCorrect(), submissionDto.getNumCorrect());
        assertEquals(submission.getNumTestCases(), submissionDto.getNumTestCases());
    }

    @Test
    public void submissionToDto() {
        PlayerCode playerCode = new PlayerCode();
        playerCode.setCode(CODE);
        playerCode.setLanguage(LANGUAGE);

        Submission submission = new Submission();
        submission.setPlayerCode(playerCode);
        submission.setNumTestCases(TEST_CASES);
        submission.setNumCorrect(TEST_CASES);

        SubmissionDto submissionDto = GameMapper.submissionToDto(submission);
        assertEquals(submission.getPlayerCode().getCode(), submissionDto.getCode());
        assertEquals(submission.getPlayerCode().getLanguage(), submissionDto.getLanguage());
        assertEquals(submission.getNumCorrect(), submissionDto.getNumCorrect());
    }
}
