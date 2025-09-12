package org.matsim.contrib.profiling.aop.trace;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TestTraceProfilingAspect extends TraceProfilingAspect {

	@Pointcut("!cflow(adviceexecution()) && execution(* org.matsim..*(..))")
	public void traceTarget() {}

}
