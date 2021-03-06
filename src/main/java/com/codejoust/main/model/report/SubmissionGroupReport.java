package com.codejoust.main.model.report;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Getter
@Setter
public class SubmissionGroupReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String gameReportId;

    @OneToMany(fetch = FetchType.EAGER)
    @Setter(AccessLevel.PRIVATE)
    @Fetch(value = FetchMode.SUBSELECT)
    @JoinColumn(name = "submission_reports_table_id")
    private List<SubmissionReport> submissionReports = new ArrayList<>();
}
