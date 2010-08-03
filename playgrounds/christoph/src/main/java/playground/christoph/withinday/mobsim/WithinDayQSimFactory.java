/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeWithinDayQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.withinday.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.ptproject.qsim.netsimengine.ParallelQSimEngineFactory;

/*
 * Depending on the number of threads in the config file a
 * ParallelQSimEngine or a DefaultQSimEngine is used.
 */
public class WithinDayQSimFactory implements MobsimFactory {
	private static final Logger log = Logger.getLogger(WithinDayQSimFactory.class);
	  
	@Override
	public WithinDayQSim createMobsim(Scenario sc, EventsManager eventsManager) {
		// Get number of parallel Threads
		QSimConfigGroup conf = (QSimConfigGroup) sc.getConfig().getModule(QSimConfigGroup.GROUP_NAME);
		  
		int numOfThreads = 1;
		if (conf != null) numOfThreads = conf.getNumberOfThreads();

		if (numOfThreads > 1) {
			SynchronizedEventsManagerImpl em = new SynchronizedEventsManagerImpl(eventsManager);
			WithinDayQSim sim = new WithinDayQSim(sc, em, new ParallelQSimEngineFactory());
			  			  
			// Get number of parallel Threads
			log.info("Using parallel QSim with " + numOfThreads + " parallel Threads.");
			  
			return sim;
		}
		else {
			return new WithinDayQSim(sc, eventsManager); 
		}
	}
}
