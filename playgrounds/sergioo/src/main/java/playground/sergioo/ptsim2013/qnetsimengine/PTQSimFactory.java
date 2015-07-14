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

package playground.sergioo.ptsim2013.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import playground.sergioo.ptsim2013.QSim;
import playground.sergioo.ptsim2013.agents.PopulationAgentSource;
import playground.sergioo.ptsim2013.pt.BoardAlightVehicleTransitStopHandlerFactory;
import playground.sergioo.ptsim2013.pt.TransitQSimEngine;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTime;

/**
 * Constructs an instance of the modular QSim based on the required features as per the Config file.
 * Used by the Controler.
 * You can get an instance of this factory and use it to create a QSim if you are running a "complete"
 * simulation based on a config file. For test cases or specific experiments, it may be better to
 * plug together your QSim instance from your own code.
 * It is not recommended to mix, i.e. to construct a QSim with this code and then add further modules to
 * your own code.
 *
 */
public class PTQSimFactory implements MobsimFactory {

	private final static Logger log = Logger.getLogger(PTQSimFactory.class);
	private final StopStopTime stopStopTime;
	
	public PTQSimFactory() {
		stopStopTime = null;
	}
	public PTQSimFactory(StopStopTime stopStopTime) {
		this.stopStopTime = stopStopTime;
	}
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		PTQNetsimEngineFactory netsimEngFactory;
		if (numOfThreads > 1) {
			netsimEngFactory = new PTParallelQNetsimEngineFactory();
			log.info("Using parallel QSim with " + numOfThreads + " threads.");
		} else {
			netsimEngFactory = new PTQSimEngineFactory();
		}
		QSim qSim = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		PTQNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		if(stopStopTime!=null)
			netsimEngine.setStopStopTime(stopStopTime);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory;
		if (sc.getConfig().transit().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setUseUmlaeufe(true);
			transitEngine.setTransitStopHandlerFactory(new BoardAlightVehicleTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
