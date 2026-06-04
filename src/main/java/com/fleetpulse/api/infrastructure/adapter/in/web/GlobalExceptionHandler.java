package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE = "https://api.fleetpulse.com/errors";

    private static final String VALIDATION_FAILED = ERROR_BASE + "/validation-failed";
    private static final String INVALID_CREDENTIALS = ERROR_BASE + "/invalid-credentials";
    private static final String TOKEN_INVALID = ERROR_BASE + "/token-invalid";
    private static final String TOKEN_EXPIRED = ERROR_BASE + "/token-expired";
    private static final String INTERNAL_ERROR = ERROR_BASE + "/internal-error";
    private static final String USER_NOT_FOUND = ERROR_BASE + "/user-not-found";
    private static final String USERNAME_EXISTS = ERROR_BASE + "/username-exists";
    private static final String UNIT_NOT_FOUND = ERROR_BASE + "/unit-not-found";
    private static final String INVALID_COORDINATES = ERROR_BASE + "/invalid-coordinates";
    private static final String FORBIDDEN = ERROR_BASE + "/forbidden";
    private static final String SERVICE_UNAVAILABLE = ERROR_BASE + "/service-unavailable";
    private static final String SOAP_REJECTED = ERROR_BASE + "/soap-rejected";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationFailure(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ){
        log.warn("VALIDATION_FAILED {} - path: {} - client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid")
                ));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setType(URI.create(VALIDATION_FAILED));
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields failed validation. See 'errors' for details");
        problem.setProperty("errors", errors);
        addStandardProperties(problem,request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);

    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            UserNotActiveException.class,
    })
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(
            RuntimeException ex,
            HttpServletRequest request
    ){
        log.warn("AUTH_FAILURE: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr()
        );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create(INVALID_CREDENTIALS));
        problem.setTitle("Invalid credentials");
        problem.setDetail("username or password incorrect");

        addStandardProperties(problem,request);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);

    }

    @ExceptionHandler({
            RefreshTokenNotFoundException.class,
            RefreshTokenRevokedException.class
    })
    public ResponseEntity<ProblemDetail> handleInvalidToken(
            RuntimeException ex,
            HttpServletRequest request
    ){
        log.warn("INVALID_TOKEN: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create(TOKEN_INVALID));
        problem.setTitle("Invalid Token");
        problem.setDetail("The provided token is invalid or has been revoked");
        addStandardProperties(problem,request);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ProblemDetail> handleExpiredToken(
            RuntimeException ex,
            HttpServletRequest request
    ){

        log.warn("EXPIRED_TOKEN: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        problem.setType(URI.create(TOKEN_EXPIRED));
        problem.setTitle("Token expired");
        problem.setDetail("The provided token has expired. Please log in again.");
        addStandardProperties(problem,request);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);

    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {

        log.warn("USER_NOT_FOUND: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setType(URI.create(USER_NOT_FOUND));
        problem.setTitle("User not found");
        problem.setDetail("No user found with the provided identifier");
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);

    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateUsername(
            RuntimeException ex,
            HttpServletRequest request
    ){
        log.info("USERNAME_EXISTS: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problem.setType(URI.create(USERNAME_EXISTS));
        problem.setTitle("Username already exists");
        problem.setDetail("The requested username is already in use");
        addStandardProperties(problem,request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);

    }

    @ExceptionHandler(UnitNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUnitNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ){
        log.warn("UNIT_NOT_FOUND: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setType(URI.create(UNIT_NOT_FOUND));
        problem.setTitle("Unit not Found");
        problem.setDetail("No unit found with the provider identifier");
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);

    }

    @ExceptionHandler(InvalidCoordinateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCoordinate(
            RuntimeException ex,
            HttpServletRequest request
    ){
        log.warn("INVALID_COORDINATE: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setType(URI.create(INVALID_COORDINATES));
        problem.setTitle("Invalid coordinates");
        problem.setDetail("The provided coordinates are outside the valid range or indicate no GPS fix");
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);

    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handledAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ){
        log.warn("ACCESS_DENIED: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);

        problem.setType(URI.create(FORBIDDEN));
        problem.setTitle("Access denied");
        problem.setDetail("You do not have permission to access this resource");
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);

    }

    @ExceptionHandler({
            GpsProviderUnavailableException.class,
            ExternalServiceUnavailableException.class
    })
    public ResponseEntity<ProblemDetail> handleServiceUnavailable(
            RuntimeException ex,
            HttpServletRequest request
    ){

        log.error("SERVICE_UNAVAILABLE: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);

        problem.setType(URI.create(SERVICE_UNAVAILABLE));
        problem.setTitle("Service unavailable");
        problem.setDetail("A required external service is currently unavailable");
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);

    }

    @ExceptionHandler(PulseSendException.class)
    public ResponseEntity<ProblemDetail> handleSoapRejected(
            Exception ex,
            HttpServletRequest request
    ){
        log.error("SOAP_REJECTED: {} - path: {}, client: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);

        problem.setType(URI.create(SOAP_REJECTED));
        problem.setTitle("Pulse dispatch failed");
        problem.setDetail("The GPS pulse was rejected by the external provider");
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ){
        String correlationId = UUID.randomUUID().toString();

        log.error("UNEXPECTED_ERROR: {} - path: {}, client: {}, reference: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                correlationId, ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        problem.setType(URI.create(INTERNAL_ERROR));
        problem.setTitle("Internal server error");
        problem.setDetail("An unexpected error occurred. Reference: " + correlationId);
        addStandardProperties(problem, request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private void addStandardProperties(
            ProblemDetail problem,
            HttpServletRequest request){
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
    }

}
