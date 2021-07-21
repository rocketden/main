package com.codejoust.main.model.problem;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProblemTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @Column(columnDefinition = "TEXT")
    private String input;

    @EqualsAndHashCode.Include
    @Column(columnDefinition = "TEXT")
    private String output;

    @EqualsAndHashCode.Include
    private Boolean hidden = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_table_id")
    private Problem problem;

    @EqualsAndHashCode.Include
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
