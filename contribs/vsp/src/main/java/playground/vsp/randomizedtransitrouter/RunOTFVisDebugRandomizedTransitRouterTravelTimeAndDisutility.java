/* *********************************************************************** *
 * project: org.matsim.*
 * RunOTFVisDebugRandomizedTransitRouter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.vsp.randomizedtransitrouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizingTransitRouterModule;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Provider;


/**
 * @author dgrether
 *
 */
public class RunOTFVisDebugRandomizedTransitRouterTravelTimeAndDisutility {

	
	public static void main(String[] args) {
		final Config config = ConfigUtils.loadConfig(args[0]) ;

		boolean doVisualization = true;

		config.planCalcScore().setWriteExperiencedPlans(true) ;

		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawTransitFacilities(true) ; // this DOES work
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawTransitFacilityIds(false);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true);
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setScaleQuadTreeRect(true);

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler ctrl = new Controler(scenario) ;

		ctrl.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		ctrl.addOverridingModule(new RandomizingTransitRouterModule());
		
		if (doVisualization){
			ctrl.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new MobsimFactory() {

								@Override
								public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
									final QSim qSim = new QSimBuilder(sc.getConfig()).useDefaults().build(sc, eventsManager);

									final OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
									OTFClientLive.run(sc.getConfig(), server);

									return qSim;
								}
							}.createMobsim(ctrl.getScenario(), ctrl.getEvents());
						}
					});
				}
			});
		}
		ctrl.run() ;
		
	}
}
