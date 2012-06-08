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

package org.matsim.withinday.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;

/*
 * Depending on the number of threads in the config file a
 * ParallelQSimEngine or a DefaultQSimEngine is used.
 */
public class WithinDayQSimFactory implements MobsimFactory {

	private static final Logger log = Logger.getLogger(WithinDayQSimFactory.class);

	private final ReplanningManager replanningManager;
	
	public WithinDayQSimFactory(ReplanningManager replanningManager) {
		this.replanningManager = replanningManager;
	}
	
    private static QSim createWithinDayQSim(final Scenario scenario, final EventsManager events) {
        return createWithinDayQSim(scenario, events, new DefaultQSimEngineFactory());
    }

    private static QSim createWithinDayQSim(final Scenario scenario, final EventsManager events, QNetsimEngineFactory factory) {
        QSim qSim1 = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = factory.createQSimEngine(qSim1, MatsimRandom.getRandom());
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
        ExperimentalBasicWithindayAgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        return qSim;
    }

    @Override
	public QSim createMobsim(Scenario sc, EventsManager eventsManager) {
		// Get number of parallel Threads
		QSimConfigGroup conf = (QSimConfigGroup) sc.getConfig().getModule(QSimConfigGroup.GROUP_NAME);
		  
		int numOfThreads = 1;
		if (conf != null) numOfThreads = conf.getNumberOfThreads();

		QSim sim;
		if (numOfThreads > 1) {
			/*
			 * A SimStepParallelEventsManagerImpl can handle concurrent events,
			 * therefore we do not need a SynchronizedEventsManagerImpl wrapper.
			 */
        	if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
        		eventsManager = new SynchronizedEventsManagerImpl(eventsManager);        		
        	}
			sim = createWithinDayQSim(sc, eventsManager, new ParallelQNetsimEngineFactory());
			  			  
			// Get number of parallel Threads
			log.info("Using parallel QSim with " + numOfThreads + " parallel Threads.");
		}
		else {
			sim = createWithinDayQSim(sc, eventsManager);
		}
		
		sim.addMobsimEngine(replanningManager);
		
		return sim;
	}
}
