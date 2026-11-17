package com.fypgrading.evaluationservice.evaluation;

/**
 * NOT_STARTED is never stored: it's the blank form. SUBMITTED is locked for good. SUBMITTING is internal: the draft
 * is frozen while its grade is handed to admin-service (review H2); the API shows it as a draft.
 */
public enum EvaluationStatus {
    NOT_STARTED,
    DRAFT,
    SUBMITTING,
    SUBMITTED;

    /** What the API shows (the contract has no SUBMITTING). */
    public EvaluationStatus visible() {
        return this == SUBMITTING ? DRAFT : this;
    }
}
