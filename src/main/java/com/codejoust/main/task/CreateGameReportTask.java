package com.codejoust.main.task;

import java.util.TimerTask;

import com.codejoust.main.exception.TimerError;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.game_object.Game;
import com.codejoust.main.service.ReportService;

public class CreateGameReportTask extends TimerTask {

    private final Game game;

    private final ReportService reportService;
    
    public CreateGameReportTask(ReportService reportService, Game game) {
        this.reportService = reportService;
        this.game = game;

        // Handle potential errors for run().
        if (reportService == null || game == null) {
            throw new ApiException(TimerError.NULL_SETTING);
        }
    }

	@Override
    public void run() {
        // Create the game report (is the game updated here?).
        reportService.createGameReport(game);
    }
    
}
