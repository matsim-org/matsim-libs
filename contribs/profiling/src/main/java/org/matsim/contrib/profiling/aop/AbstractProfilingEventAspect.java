/* ********************************************************************** *
 * project: org.matsim.*
 * AbstractProfilingEventAspect.java
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

package org.matsim.contrib.profiling.aop;

import jdk.jfr.Event;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

// todo annotation style and code style may differ in performance, due to runtime checks
@Aspect
public abstract class AbstractProfilingEventAspect {

	@Pointcut
	public abstract void eventPoints();

	// todo provide more classes to inherit, with: no param, static part, full joinpoint
	public abstract Event createEvent(JoinPoint.StaticPart thisJoinPointStaticPart);

	// todo if the return value is never used (as opposed to the trace aspect), can we use before & after instead to potentially profit from optimizations?
	@Around("eventPoints()")
	public Object around(ProceedingJoinPoint thisJoinPoint) throws Throwable {
		Event jfrEvent = createEvent(thisJoinPoint.getStaticPart());

		jfrEvent.begin();
		var ret = thisJoinPoint.proceed();
		jfrEvent.commit();
		return ret;
	}

}
