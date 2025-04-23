package org.matsim.contrib.profiling.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.matsim.contrib.profiling.instrument.Trace;

@Aspect
public class TestTraceProfilingAspect extends TraceProfilingAspect {

	@Override
	@Pointcut("!cflow(adviceexecution()) && call(* org.matsim..*(..))")
	public void traceTarget(ProceedingJoinPoint thisJoinPoint) {}

}
