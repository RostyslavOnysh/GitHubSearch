package com.opt.github_search_repo.exception;

import com.opt.github_search_repo.controllers.GithubController;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@ControllerAdvice(assignableTypes = GithubController.class)
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponse> handleFeignNotFoundGithubUserException() {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "This user does not exist");
        log.warn("This user does not exist");
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptableException() {
        log.warn("Exception format");
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_ACCEPTABLE.value(), "Not acceptable format");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
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
}
