package com.rocketden.main.dto.game;

import com.rocketden.main.dto.problem.ProblemDto;
import com.rocketden.main.dto.problem.ProblemMapper;
import com.rocketden.main.dto.room.RoomMapper;
import com.rocketden.main.game_object.Game;
import com.rocketden.main.game_object.Player;
import com.rocketden.main.game_object.Submission;
import com.rocketden.main.model.Room;
import com.rocketden.main.model.User;
import com.rocketden.main.util.Utility;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameMapper {

    private static final ModelMapper mapper = new ModelMapper();

    protected GameMapper() {}

    public static GameDto toDto(Game game) {
        if (game == null) {
            return null;
        }

        GameDto gameDto = new GameDto();
        gameDto.setRoom(RoomMapper.toDto(game.getRoom()));
        gameDto.setGameTimer(GameTimerMapper.toDto(game.getGameTimer()));

        List<ProblemDto> problems = new ArrayList<>();
        game.getProblems().forEach(problem -> problems.add(ProblemMapper.toDto(problem)));
        gameDto.setProblems(problems);

        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);

        List<PlayerDto> players = new ArrayList<>();
        game.getPlayers().values().forEach(player -> players.add(mapper.map(player, PlayerDto.class)));
        gameDto.setPlayers(players);

        return gameDto;
    }

    public static Game fromRoom(Room room) {
        if (room == null) {
            return null;
        }

        Game game = new Game();
        game.setRoom(room);

        // Create players and assign colors in order.
        int index = 0;
        Map<String, Player> players = game.getPlayers();
        for (User user : room.getUsers()) {
            Player player = PlayerMapper.playerFromUser(user);
            player.setColor(Utility.COLOR_LIST.get(index));
            players.put(user.getUserId(), player);
            index = (index + 1) % Utility.COLOR_LIST.size();
        }

        return game;
    }

    public static SubmissionDto submissionToDto(Submission submission) {
        if (submission == null) {
            return null;
        }

        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);

        return mapper.map(submission, SubmissionDto.class);
    }
}
