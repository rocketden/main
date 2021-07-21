package com.codejoust.main.task;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codejoust.main.dto.game.GameDto;
import com.codejoust.main.dto.game.GameMapper;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.game_object.GameTimer;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.service.ReportService;
import com.codejoust.main.service.SocketService;
import com.codejoust.main.util.TestFields;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateGameReportTaskTests {

    @Mock
    private ReportService reportService;
    
    @Mock
    private SocketService socketService;

    @Test
    public void createGameReportTaskSocketMessageNullGame() {
        MockitoAnnotations.initMocks(this);
        assertThrows(ApiException.class, () -> new CreateGameReportTask(reportService, null));
    }

    @Test
    public void createGameReportTaskSocketMessageNullSocketService() {
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        user.setSessionId(TestFields.SESSION_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.MEDIUM);
        room.setHost(user);
        room.addUser(user);

        Game game = GameMapper.fromRoom(room);
        GameTimer gameTimer = new GameTimer(10L);
        game.setGameTimer(gameTimer);

        MockitoAnnotations.initMocks(this);

        assertThrows(ApiException.class, () -> new CreateGameReportTask(null, game));
    }

    @Test
    public void createGameReportTaskSocketMessage() {
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        user.setUserId(TestFields.USER_ID);
        user.setSessionId(TestFields.SESSION_ID);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setDifficulty(ProblemDifficulty.MEDIUM);
        room.setHost(user);
        room.addUser(user);

        Game game = GameMapper.fromRoom(room);
        GameTimer gameTimer = new GameTimer(1L);
        game.setGameTimer(gameTimer);

        // Make the Game DTO update that will occur on timer end.
        GameDto gameDto = GameMapper.toDto(game);
        gameDto.getGameTimer().setTimeUp(true);

        MockitoAnnotations.initMocks(this);

        CreateGameReportTask createGameReportTask = new CreateGameReportTask(reportService, game);
        gameTimer.getTimer().schedule(createGameReportTask,  1000L);

        verify(reportService, timeout(1200)).createGameReport(eq(game));
    }
}
