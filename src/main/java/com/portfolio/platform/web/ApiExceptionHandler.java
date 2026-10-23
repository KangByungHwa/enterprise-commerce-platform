package com.portfolio.platform.web;
import com.portfolio.platform.inventory.InsufficientStockException;
import com.portfolio.platform.order.OrderNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail notFound(OrderNotFoundException e){return problem(HttpStatus.NOT_FOUND,e.getMessage());}
    @ExceptionHandler({InsufficientStockException.class,IllegalArgumentException.class}) @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail conflict(RuntimeException e){return problem(HttpStatus.CONFLICT,e.getMessage());}
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail invalid(MethodArgumentNotValidException e){return problem(HttpStatus.BAD_REQUEST,"Request validation failed");}
    private ProblemDetail problem(HttpStatus status,String detail){ProblemDetail p=ProblemDetail.forStatusAndDetail(status,detail);p.setProperty("timestamp",Instant.now());return p;}
}

