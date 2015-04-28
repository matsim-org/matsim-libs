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
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import javax.inject.Inject;
import javax.inject.Provider;

public class TransitControler {

	public static void main(final String[] args) {
//		args[0] = "/Users/nagel/kw/rotterdam/config.xml" ;
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setColoringScheme( OTFVisConfigGroup.ColoringScheme.bvg ) ;
		
		Controler tc = new Controler(config) ;
		tc.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(MyMobsimFactory.class);
			}
		});

		tc.setOverwriteFiles(true);
//		tc.setCreateGraphs(false);
		tc.run();
	}
	
	static class MyMobsimFactory implements Provider<Mobsim> {
		@Inject Scenario sc;
		@Inject EventsManager eventsManager;
		private boolean useOTFVis = true ;

		@Override
		public Mobsim get() {
			QSim simulation = (QSim) QSimUtils.createDefaultQSim(sc, eventsManager);

//			simulation.getQSimTransitEngine().setTransitStopHandlerFactory(new SimpleTransitStopHandlerFactory());
//			this.events.addHandler(new LogOutputEventHandler());

			if ( useOTFVis ) {
				final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(simulation.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
				otfVisConfig.setDrawTransitFacilities(false) ;
				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, simulation);
				OTFClientLive.run(sc.getConfig(), server);
			}

//			if(this.useHeadwayControler){
//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
//				this.events.addHandler(new FixedHeadwayControler(simulation));		
//			}
			
			return simulation ;
		}
	}
}
