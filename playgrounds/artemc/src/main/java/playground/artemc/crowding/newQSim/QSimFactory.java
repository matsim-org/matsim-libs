/* *********************************************************************** *
 * project: org.matsim.*
 * QSimFactory.java
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

package playground.artemc.crowding.newQSim;

/*
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SimStepParallelEventsManagerImpl;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;

import crowdedness.CrowdednessTransitStopHandlerFactory;

*/

/**
 * Constructs an instance of the modular QSim based on the required features as per the Config file.
 * Used by the Controler.
 * You can get an instance of this factory and use it to create a QSim if you are running a "complete"
 * simulation based on a config file. For test cases or specific experiments, it may be better to
 * plug together your QSim instance from your own code.
 * It is not recommended to mix, i.e. to construct a QSim with this code and then add further modules to
 * your own code.
 *
 * A line is changed in order to call the Qsim containing the new L. Sun's dwell time model
 * @author grerat
 */
///*
//public class QSimFactory implements MobsimFactory {
//
//	private final static Logger log = Logger.getLogger(QSimFactory.class);
//
//	@Override
//	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {
//
//		QSimConfigGroup conf = sc.getConfig().qsim();
//		if (conf == null) {
//			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
//		}
//
//		// Get number of parallel Threads
//		int numOfThreads = conf.getNumberOfThreads();
//		QNetsimEngineFactory netsimEngFactory;
//		if (numOfThreads > 1) {
//			*/
///*
//			 * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
//			 * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
//			 * SynchronizedEventsManagerImpl.
//			 *//*
//
//			if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
//				eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
//			}
//			netsimEngFactory = new ParallelQNetsimEngineFactory();
//			log.info("Using parallel QSim with " + numOfThreads + " threads.");
//		} else {
//			netsimEngFactory = new DefaultQSimEngineFactory();
//		}
//		QSim qSim = new QSim(sc, eventsManager);
//		ActivityEngine activityEngine = new ActivityEngine();
//		qSim.addMobsimEngine(activityEngine);
//		qSim.addActivityHandler(activityEngine);
//		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
//		qSim.addMobsimEngine(netsimEngine);
//		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
//		TeleportationEngine teleportationEngine = new TeleportationEngine();
//		qSim.addMobsimEngine(teleportationEngine);
//
//		AgentFactory agentFactory;
//		if (sc.getConfig().scenario().isUseTransit()) {
//			agentFactory = new TransitAgentFactory(qSim);
//			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
//			transitEngine.setUseUmlaeufe(true);
//
//			//Changed on the original class to implement the Sun's Dwell time
//			transitEngine.setTransitStopHandlerFactory(new CrowdednessTransitStopHandlerFactory());
//
//			qSim.addDepartureHandler(transitEngine);
//			qSim.addAgentSource(transitEngine);
//			qSim.addMobsimEngine(transitEngine);
//		} else {
//			agentFactory = new DefaultAgentFactory(qSim);
//		}
//		if (sc.getConfig().network().isTimeVariantNetwork()) {
//			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
//		}
//		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
//		qSim.addAgentSource(agentSource);
//		return qSim;
//	}
//
//}
//*/
