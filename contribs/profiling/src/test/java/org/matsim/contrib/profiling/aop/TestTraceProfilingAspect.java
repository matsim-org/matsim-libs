package org.matsim.contrib.profiling.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TestTraceProfilingAspect extends TraceProfilingAspect {

	@Override
	@Pointcut("!cflow(adviceexecution()) && call(* org.matsim..*(..))")
	public void traceTarget() {}

}
