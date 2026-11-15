package com.fypgrading.evaluationservice.evaluation;

/** The five fixed grading checkpoints, in order. */
public enum Assessment {
    PROPOSAL_PRESENTATION("Proposal presentation"),
    PROGRESS_REPORT("Progress report"),
    ADVISOR_ASSESSMENT("Advisor assessment"),
    FINAL_REPORT("Final report"),
    FINAL_PRESENTATION("Final presentation");

    private final String displayName;

    Assessment(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
