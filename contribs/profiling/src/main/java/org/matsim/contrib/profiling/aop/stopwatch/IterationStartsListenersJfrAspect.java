package org.matsim.contrib.profiling.aop.stopwatch;

import jdk.jfr.Event;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.matsim.contrib.profiling.aop.AbstractProfilingEventAspect;

@Aspect
public class IterationStartsListenersJfrAspect extends AbstractProfilingEventAspect {

	@Pointcut("execution(* org.matsim.core.controler.ControlerListenerManagerImpl.fireControlerIterationStartsEvent(..))")
	public void eventPoints() {}

	public Event createEvent(JoinPoint.StaticPart thisJoinPointStaticPart) {
		return new AopStopwatchIterationStartsJfrEvent();
	}
}
