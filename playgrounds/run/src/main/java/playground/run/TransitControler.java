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

package playground.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;

public class TransitControler {

	public static void main(final String[] args) {
//		args[0] = "/Users/nagel/kw/rotterdam/config.xml" ;
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.otfVis().setColoringScheme( OTFVisConfigGroup.COLORING_BVG ) ;
		
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
		public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
			QSim simulation = (QSim) new QSimFactory().createMobsim(sc, eventsManager);

			simulation.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//			simulation.getQSimTransitEngine().setTransitStopHandlerFactory(new SimpleTransitStopHandlerFactory());
//			this.events.addHandler(new LogOutputEventHandler());

			if ( useOTFVis ) {
				OTFVisMobsimFeature otfVisQSimFeature = new OTFVisMobsimFeature(simulation);
				final OTFVisConfigGroup otfVisConfig = simulation.getScenario().getConfig().otfVis();
				otfVisQSimFeature.setVisualizeTeleportedAgents(otfVisConfig.isShowTeleportedAgents());
				simulation.addQueueSimulationListeners(otfVisQSimFeature);
				simulation.getEventsManager().addHandler(otfVisQSimFeature) ;
				otfVisConfig.setDrawTransitFacilities(false) ;
				otfVisConfig.setShowParking(true) ;
			}

//			if(this.useHeadwayControler){
//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
//				this.events.addHandler(new FixedHeadwayControler(simulation));		
//			}
			
			return simulation ;
		}
	}
}
