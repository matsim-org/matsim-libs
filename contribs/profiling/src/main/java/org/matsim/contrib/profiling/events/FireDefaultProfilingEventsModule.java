/* ********************************************************************** *
 * project: org.matsim.*
 * FireDefaultProfilingEventsModule.java
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

package org.matsim.contrib.profiling.events;

import org.matsim.core.controler.AbstractModule;

/**
 * Hook into MATSim Listeners to create JFR profiling events at different phases within MATSim.
 * <p>Events to be recorded:
 * <ul>
 *     <li>{@link MatsimStartupListenersJfrEvent}</li>
 *     <li>{@link IterationJfrEvent}</li>
 *     <li>{@link IterationStartsListenersJfrEvent}</li>
 *     <li>{@link ReplanningListenersJfrEvent}</li>
 *     <li>{@link MobsimJfrEvent}</li>
 *     <li>{@link ScoringListenersJfrEvent}</li>
 *     <li>{@link IterationEndsListenersJfrEvent}</li>
 *     <li>{@link MatsimShutdownListenersJfrEvent}</li>
 * </ul>
 */
public class FireDefaultProfilingEventsModule extends AbstractModule {

	@Override
	public void install() {
		var iterationTimer = new IterationJfrTimer();
		addControlerListenerBinding().toInstance(iterationTimer.startListener);
		addControlerListenerBinding().toInstance(iterationTimer);
		var operationsTimer = new OperationsJfrTimer();
		addControlerListenerBinding().toInstance(operationsTimer.startListener);
		addControlerListenerBinding().toInstance(operationsTimer);
		addMobsimListenerBinding().to(MobsimJfrTimer.class);
	}

}
