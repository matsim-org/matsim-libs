/* ********************************************************************** *
 * project: org.matsim.*
 * JFREventCreator.java
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
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

/**
 * Simple AOP method interceptor to record JFR {@link Event events} for each invocation of the intercepted method and its duration.
 */
public class JFREventCreator implements MethodInterceptor {
	private static final Logger log = LogManager.getLogger(JFREventCreator.class);

	private final Function<MethodInvocation, ? extends Event> eventFunction;

	public JFREventCreator(Function<MethodInvocation, ? extends Event> eventFunction) {
		this.eventFunction = eventFunction;
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Event event = eventFunction.apply(invocation);

		log.info("AOP profiling: {}", event);

		event.begin();
		Object object = invocation.proceed();
		event.commit();
		return object;
	}
}
