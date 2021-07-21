package com.codejoust.main.dao;

import com.codejoust.main.model.report.GameReport;

import org.springframework.data.repository.CrudRepository;

public interface GameReportRepository extends CrudRepository<GameReport, Integer> {

    GameReport findGameReportByGameReportId(String gameReportId);
}
