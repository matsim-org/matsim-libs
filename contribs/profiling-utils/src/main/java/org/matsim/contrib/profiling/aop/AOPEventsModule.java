/* ********************************************************************** *
 * project: org.matsim.*
 * AOPEventsModule.java
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

import com.google.inject.matcher.Matchers;
import org.matsim.contrib.profiling.events.JFRMatsimEvent;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ReplanningListener;

import java.lang.reflect.Modifier;

/**
 * AOP via Guice
 */
public class AOPEventsModule extends AbstractModule {

	public void install() {
		binder().bindInterceptor(Matchers.subclassesOf(ReplanningListener.class).and(Matchers.not((c) -> Modifier.isFinal(c.getModifiers()))),
			MethodNameMatcher.forName("notifyReplanning"),
			new JFREventCreator((invocation) -> JFRMatsimEvent.create("scoring AOP: " + invocation.getMethod().getDeclaringClass().getName())));
	}

}
