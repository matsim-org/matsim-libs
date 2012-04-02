/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.kai.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.agents.TransitAgentFactory;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class TransitControler {

	public static void main(final String[] args) {
		//		args[0] = "/Users/nagel/kw/rotterdam/config.xml" ;
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
//		config.otfVis().setColoringScheme( OTFVisConfigGroup.COLORING_BVG ) ;

		config.getQSimConfigGroup().setVehicleBehavior( QSimConfigGroup.VEHICLE_BEHAVIOR_TELEPORT ) ;

		Controler tc = new Controler(config) ;

		MobsimFactory mobsimFactory = new MyMobsimFactory() ; 
		tc.setMobsimFactory(mobsimFactory) ;

		tc.setOverwriteFiles(true);
		//		tc.setCreateGraphs(false);
		tc.run();
	}

	static class MyMobsimFactory implements MobsimFactory {
		private boolean useOTFVis = true ;

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			EventsManager eventsManager1 = eventsManager;

			QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
			if (conf == null) {
				throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
			}

			// Get number of parallel Threads
			int numOfThreads = conf.getNumberOfThreads();
			QNetsimEngineFactory netsimEngFactory;
			if (numOfThreads > 1) {
				eventsManager1 = new SynchronizedEventsManagerImpl(eventsManager1);
				netsimEngFactory = new ParallelQNetsimEngineFactory();
			} else {
				netsimEngFactory = new DefaultQSimEngineFactory();
			}
			QSim qSim = new QSim(sc, eventsManager1, netsimEngFactory);
			AgentFactory agentFactory= new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setUseUmlaeufe(true);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addMobsimEngine(transitEngine);
			qSim.addAgentSource(transitEngine);
			PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
			qSim.addAgentSource(agentSource);

			//			simulation.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//			transitEngine.setTransitStopHandlerFactory(new SimpleTransitStopHandlerFactory());
			//			this.events.addHandler(new LogOutputEventHandler());

			if ( useOTFVis ) {
				// otfvis configuration.  There is more you can do here than via file!
				final OTFVisConfigGroup otfVisConfig = qSim.getScenario().getConfig().otfVis();
				otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
				//				otfVisConfig.setShowParking(true) ; // this does not really work


				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
				OTFClientLive.run(sc.getConfig(), server);
			}
			//			if(this.useHeadwayControler){
			//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
			//				this.events.addHandler(new FixedHeadwayControler(simulation));		
			//			}

			return qSim ;
		}
	}
}
