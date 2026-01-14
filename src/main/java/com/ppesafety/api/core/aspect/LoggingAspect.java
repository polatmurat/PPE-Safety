package com.ppesafety.api.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceLayer() {
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLayer() {
    }

    @Around("serviceLayer() || controllerLayer()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String username = getCurrentUsername();

        logger.debug("User [{}] executing: {} with args: {}",
                username, methodName, Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 1000) {
                logger.warn("SLOW: {} executed by [{}] in {}ms", methodName, username, executionTime);
            } else {
                logger.debug("{} executed by [{}] in {}ms", methodName, username, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("FAILED: {} executed by [{}] in {}ms with error: {}",
                    methodName, username, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    @Around("@annotation(com.ppesafety.api.core.annotation.Auditable)")
    public Object logAuditableAction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String username = getCurrentUsername();

        logger.info("AUDIT: User [{}] performing action: {} with args: {}",
                username, methodName, Arrays.toString(joinPoint.getArgs()));

        Object result = joinPoint.proceed();

        logger.info("AUDIT: User [{}] completed action: {} successfully", username, methodName);

        return result;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
