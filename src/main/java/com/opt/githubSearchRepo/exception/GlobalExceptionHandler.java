package com.opt.githubSearchRepo.exception;

import com.opt.githubSearchRepo.controllers.GithubController;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice(assignableTypes = GithubController.class)
@Slf4j
public class GlobalExceptionHandler {

    public ResponseEntity<ErrorResponse> handleFeignNotFoundGithubUserException() {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "This user does not exist");
        log.warn("This user does not exist");
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FeignException.Forbidden.class)
    public ResponseEntity<ErrorResponse> handleFeignRateLimitExceededException(FeignException.Forbidden exception) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Rate limit exceeded");
        log.warn("Rate limit exceeded: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        ErrorResponse exceptionResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred!");
        log.error("Unexpected error: {}", exception.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(WebClientResponseException.Forbidden.class)
    public ResponseEntity<ErrorResponse> handleWebClientForbidden(WebClientResponseException.Forbidden exception) {
        ErrorResponse exceptionResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(),
                "GitHub API rate limit exceeded!");
        log.warn("GitHub API rate limit exceeded: {}", exception.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(WebClientResponseException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getRawStatusCode());
        ErrorResponse exceptionResponse = new ErrorResponse(status.value(),
                "An error occurred while accessing the GitHub API: " + exception.getMessage());
        log.error("WebClient error: {}", exception.getMessage());
        return new ResponseEntity<>(exceptionResponse, status);
    }

    @ExceptionHandler(WebClientResponseException.NotFound.class)
    public ResponseEntity<ErrorResponse> handleWebClientNotFound(WebClientResponseException.NotFound exception) {
        ErrorResponse exceptionResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                "GitHub user or resource not found!");
        log.warn("WebClient user or resource not found: {}", exception.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotAcceptableStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(NotAcceptableStatusException ex) {
        ErrorResponse errorResponse = new ErrorResponse(406, "Not acceptable format");
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponse errorResponse = new ErrorResponse(406, "Not acceptable format");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}
