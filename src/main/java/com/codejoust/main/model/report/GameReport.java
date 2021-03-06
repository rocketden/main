package com.codejoust.main.model.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.ProblemContainer;

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
public class GameReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @EqualsAndHashCode.Include
    private String gameReportId = UUID.randomUUID().toString();

    @OneToMany(fetch = FetchType.EAGER)
    @Setter(AccessLevel.PRIVATE)
    @Fetch(value = FetchMode.SUBSELECT)
    @JoinColumn(name = "problem_containers_table_id")
    private List<ProblemContainer> problemContainers = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH}, fetch = FetchType.EAGER)
    @Setter(AccessLevel.PRIVATE)
    @JoinColumn(name = "users_table_id")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<User> users = new ArrayList<>();

    // The start time of the game
    private Instant createdDateTime;

    // The game duration, in seconds.
    private Long duration;

    // How the game ended.
    private GameEndType gameEndType;
}
