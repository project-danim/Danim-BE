package com.project.danim_be.common.Anotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Aspect
public class LogAspect {

	Logger logger = LoggerFactory.getLogger(LogAspect.class);

	@Around("@annotation(LogExecutionTime)")
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable{
		StopWatch stopWatch = new StopWatch();
		String methodName = joinPoint.getSignature().getName();

		stopWatch.start(methodName);

		Object proceed = joinPoint.proceed();

		stopWatch.stop();
		long timeTaken = stopWatch.getTotalTimeMillis();
		double timeInSeconds = (double) timeTaken / 1000;
		logger.info("Execution time of " + methodName + ": " + timeInSeconds + " seconds.");

		return proceed;
	}
}

