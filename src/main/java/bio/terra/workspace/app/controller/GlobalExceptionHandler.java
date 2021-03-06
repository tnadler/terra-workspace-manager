package bio.terra.workspace.app.controller;

import bio.terra.workspace.common.exception.ErrorReportException;
import bio.terra.workspace.generated.model.ErrorReport;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

// This module provides a top-level exception handler for controllers.
// All exceptions that rise through the controllers are caught in this handler.
// It converts the exceptions into standard ErrorReport responses.

@RestControllerAdvice
public class GlobalExceptionHandler {
  private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // -- Error Report - one of our exceptions --
  @ExceptionHandler(ErrorReportException.class)
  public ResponseEntity<ErrorReport> errorReportHandler(ErrorReportException ex) {
    return buildErrorReport(ex, ex.getStatusCode(), ex.getCauses());
  }

  // -- validation exceptions - we don't control the exception raised
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    NoHandlerFoundException.class
  })
  public ResponseEntity<ErrorReport> validationExceptionHandler(Exception ex) {
    logger.error("Global exception handler: catch stack", ex);
    // For security reasons, we generally don't want to include the user's invalid (and potentially
    // malicious) input in the error response, which also means we don't include the full exception.
    // Instead, we return a generic error message about input validation.
    String validationErrorMessage =
        "Request could not be parsed or was invalid: "
            + ex.getClass().getSimpleName()
            + ". Ensure that all types are correct and that enums have valid values.";
    ErrorReport errorReport =
        new ErrorReport()
            .message(validationErrorMessage)
            .statusCode(HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(errorReport, HttpStatus.BAD_REQUEST);
  }

  // -- catchall - log so we can understand what we have missed in the handlers above
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorReport> catchallHandler(Exception ex) {
    logger.error("Exception caught by catchall hander", ex);
    return buildErrorReport(ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
  }

  private ResponseEntity<ErrorReport> buildErrorReport(
      Throwable ex, HttpStatus statusCode, List<String> causes) {
    StringBuilder combinedCauseString = new StringBuilder();
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      combinedCauseString.append("cause: " + cause.toString() + ", ");
    }
    logger.error("Global exception handler: " + combinedCauseString.toString(), ex);

    ErrorReport errorReport =
        new ErrorReport().message(ex.getMessage()).statusCode(statusCode.value()).causes(causes);
    return new ResponseEntity<>(errorReport, statusCode);
  }
}
