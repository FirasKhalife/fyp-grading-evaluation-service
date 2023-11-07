package com.fypgrading.reviewservice.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSubmittedEvent {

    private Integer teamId;

    private Integer reviewerId;
}
