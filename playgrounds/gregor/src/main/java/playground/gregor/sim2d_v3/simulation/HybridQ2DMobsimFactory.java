/* *********************************************************************** *
 * project: org.matsim.*
 * HybridQ2DMobsimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.simulation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

public class HybridQ2DMobsimFactory implements MobsimFactory {

	private final static Logger log = Logger.getLogger(HybridQ2DMobsimFactory.class);
	
	Sim2DEngine sim2DEngine = null;

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

		if (!sc.getConfig().controler().getMobsim().equals("hybridQ2D")) {
			throw new RuntimeException("This factory does not make sense for " + sc.getConfig().controler().getMobsim()  );
		}
		
		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();

//		QNetsimEngineFactory netsimEngFactory;
//		if (numOfThreads > 1) {
//			eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
//			netsimEngFactory = new ParallelQNetsimEngineFactory();
//			log.info("Using parallel QSim with " + numOfThreads + " threads.");
//		} else {
//			netsimEngFactory = new DefaultQSimEngineFactory();
//		}

//		QSim qSim = QSim.createQSimWithDefaultEngines(sc, eventsManager, netsimEngFactory);

		QSim qSim = new QSim(sc, eventsManager);

		Sim2DActivityEngine activityEngine = new Sim2DActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		
		Sim2DEngine e = new Sim2DEngine(qSim);
		this.sim2DEngine = e;
		qSim.addMobsimEngine(e);
		Sim2DDepartureHandler d = new Sim2DDepartureHandler(e);
		qSim.addDepartureHandler(d);
		
//		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim); // no longer needed
//		QNetsimEngine netsimEngine = new QNetsimEngine( qSim, new KaiHybridNetworkFactory( e ) ) ; // use this instead of null version
		QNetsimEngine netsimEngine = new QNetsimEngine( qSim, null ) ;
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		return qSim;
	}

	public Sim2DEngine getSim2DEngine() {
		return this.sim2DEngine;
	}
}
