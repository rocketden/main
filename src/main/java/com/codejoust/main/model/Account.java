package com.codejoust.main.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import com.codejoust.main.dto.account.AccountRole;
import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.model.problem.ProblemTag;
import com.codejoust.main.model.report.GameReport;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    // Firebase unique identifier (UID) - links this account with the Firebase info
    @EqualsAndHashCode.Include
    private String uid;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER)
    @Setter(AccessLevel.PRIVATE)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Problem> problems = new ArrayList<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER)
    @Setter(AccessLevel.PRIVATE)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProblemTag> problemTags = new ArrayList<>();

    // List of tags associated with this problem
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Setter(AccessLevel.PRIVATE)
    @JoinColumn(name = "game_report_id")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<GameReport> gameReports = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private AccountRole role = AccountRole.TEACHER;

    public void addGameReport(GameReport gameReport) {
        gameReports.add(0, gameReport);
    }
}
