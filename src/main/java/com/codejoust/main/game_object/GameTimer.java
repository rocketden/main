package com.codejoust.main.game_object;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * The Timer class handles the timing for the Game.
 * This class is dynamic, so depending on player actions time could
 * be paused, added, or subtracted.
 */
@Getter
@Setter
public class GameTimer {

    // Varying durations of Game Timer, in seconds.
    public static final long DURATION_10_SECONDS = 10;
    public static final long DURATION_1 = 60;
    public static final long DURATION_10 = 600;
    public static final long DURATION_15 = 900;
    public static final long DURATION_30 = 1800;
    public static final Map<Long, String> TIME_LEFT_DURATION_CONTENT = Map.of(
        DURATION_30, "are thirty minutes",
        DURATION_10, "are ten minutes",
        DURATION_1, "is one minute",
        DURATION_10_SECONDS, "are ten seconds"
    );

    // The time that the game began.
    private Instant startTime = Instant.now();

    // The projected game duration, in seconds.
    private Long duration;

    private Instant endTime;

    private boolean timeUp = false;

    private Timer timer;

    private List<Timer> notificationTimers = new ArrayList<>();

    /**
     * Instantiate the GameTimer class, and schedule the end game task after
     * timer ends.
     * 
     * @param duration The duration, in seconds, until the timer ends.
     */
    public GameTimer(Long duration) {
        this.duration = duration;
        this.endTime = this.startTime.plusSeconds(duration);
        this.timer = new Timer();
    }

}
