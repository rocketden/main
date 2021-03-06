package com.codejoust.main.mapper;

import com.codejoust.main.util.TestFields;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codejoust.main.dto.game.GameTimerDto;
import com.codejoust.main.dto.game.GameTimerMapper;
import com.codejoust.main.game_object.GameTimer;

@SpringBootTest
public class GameTimerMapperTests {

    @Test
    public void toDto() {
        GameTimer gameTimer = new GameTimer(TestFields.DURATION);
        GameTimerDto gameTimerDto = GameTimerMapper.toDto(gameTimer);

        assertEquals(gameTimer.getDuration(), gameTimerDto.getDuration());
        assertEquals(gameTimer.getStartTime(), gameTimerDto.getStartTime());
        assertEquals(gameTimer.getEndTime(), gameTimerDto.getEndTime());
        assertEquals(gameTimer.isTimeUp(), gameTimerDto.isTimeUp());
    }
}
