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

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class TransitControler2 {

	private static class OTFVisMobsimListener implements MobsimInitializedListener{
		@Inject Scenario scenario ;
		@Inject EventsManager events ;
		@Override 
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			QSim qsim = (QSim) e.getQueueSimulation() ; 
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim( scenario.getConfig(), scenario, events, qsim);
			OTFClientLive.run(scenario.getConfig(), server);
		}
	}

	private static boolean useTransit = true ;
	private static boolean useOTFVis = true ;

	public static void main(final String[] args) {
		//		args[0] = "/Users/nagel/kw/rotterdam/config.xml" ;
		if ( args.length > 1 ) {
			useOTFVis = Boolean.parseBoolean(args[1]) ;
		}

		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).readFile(args[0]);
		if ( useTransit ) {
			config.transit().setUseTransit(true);
			//		config.otfVis().setColoringScheme( OTFVisConfigGroup.COLORING_BVG ) ;
		}

		config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.teleport ) ;

		//		config.otfVis().setShowTeleportedAgents(true) ;


		final Controler controler = new Controler(config) ;
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles ) ;
		controler.setDirtyShutdown(true);

		//		Logger.getLogger("main").warn("warning: using randomized pt router!!!!") ;
		//		tc.addOverridingModule(new RandomizedTransitRouterModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addMobsimListenerBinding().to( OTFVisMobsimListener.class ) ;
				
//				bindMobsim().toProvider(new Provider<Mobsim>() {
//					@Inject EventsManager events ;
//					@Inject Scenario scenario ;
//					@Override
//					public Mobsim get() {
//						QSim qSim = QSimUtils.createDefaultQSim(scenario, events);
//
//						if ( TransitControler2.useOTFVis ) {
//							// otfvis configuration.  There is more you can do here than via file!
//							final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
//							otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
//							otfVisConfig.setAgentSize((float) 120.);
//							//				otfVisConfig.setShowParking(true) ; // this does not really work
//							//				otfVisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.bvg) ;
//							//				otfVisConfig.setShowTeleportedAgents(true) ;
//
//							OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
//							OTFClientLive.run(scenario.getConfig(), server);
//						}
//						//			if(this.useHeadwayControler){
//						//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
//						//				this.events.addHandler(new FixedHeadwayControler(simulation));		
//						//			}
//
//						return qSim ;
//					}
//				});
			}
		});
		controler.addOverridingModule(new OTFVisFileWriterModule());
		//		tc.setCreateGraphs(false);

		controler.run();
	}

}
