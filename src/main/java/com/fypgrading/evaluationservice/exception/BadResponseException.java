package com.fypgrading.evaluationservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BadResponseException extends RuntimeException {

    private HttpStatusCode httpStatusCode;

    private ExceptionResponse response;

}
