/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationRunner.java
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

package playground.christoph.evacuation.controler;

import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.contrib.multimodal.ControlerDefaultsWithMultiModalModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.withinday.controller.WithinDayControlerListener;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.trafficmonitoring.EvacuationPTTravelTimeFactory;
import playground.meisterk.kti.config.KtiConfigGroup;

import javax.inject.Provider;
import java.util.Map;

public class EvacuationRunner {

	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			Config config = ConfigUtils.loadConfig(args[0], new MultiModalConfigGroup(), new KtiConfigGroup());
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			new EvacuationConfigReader().readFile(args[1]);
			EvacuationConfig.printConfig();
			
			// Prepare scenario - this could be done as pre-processing step
			PrepareEvacuationScenarioListener prepareEvacuationScenario = new PrepareEvacuationScenarioListener();
			prepareEvacuationScenario.prepareScenario(scenario);
			
			final Controler controler = new Controler(scenario);

			/*
			 * Adapt walk- and bike speed according to car speed reduction.
			 * This has to be done before walk and bike travel time objects
			 * have been created.
			 */
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * EvacuationConfig.speedFactor);
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike) * EvacuationConfig.speedFactor);
	        controler.setModules(new AbstractModule() {
                @Override
                public void install() {
                    // create Controler Listeners; add them afterwards in a meaningful order
                    ControlerDefaultsWithMultiModalModule controlerDefaultsWithMultiModalModule = new ControlerDefaultsWithMultiModalModule();
                    controlerDefaultsWithMultiModalModule.addAdditionalTravelTimeFactory(TransportMode.pt, new EvacuationPTTravelTimeFactory(TransportMode.pt, controler.getConfig().plansCalcRoute()));
                    install(controlerDefaultsWithMultiModalModule);
                    WithinDayControlerListener withinDayControlerListener = new WithinDayControlerListener();
                    Provider<Map<String, TravelTime>> multiModalTravelTimes = getProvider(new TypeLiteral<Map<String, TravelTime>>(){});

                    PreconfigureWithinDayControlerListener preconfigureWithinDayControlerListener = new PreconfigureWithinDayControlerListener(
                            withinDayControlerListener, multiModalTravelTimes);
                    EvacuationControlerListener evacuationControlerListener = new EvacuationControlerListener(withinDayControlerListener,
                            multiModalTravelTimes);

                    // Analysis stuff
					addControlerListenerBinding().toInstance(new ActivitiesAnalyzer());
					addControlerListenerBinding().toInstance(new TripsAnalyzer());

					// Evacuation stuff
					addControlerListenerBinding().toInstance(evacuationControlerListener);

					// Within-day Replanning
                    withinDayControlerListener.setModesAnalyzedByTravelTimeCollector(CollectionUtils.stringToSet(TransportMode.car));
					addControlerListenerBinding().toInstance(withinDayControlerListener);

					// pre-configure within-day controler listener with outcomes from the multi-modal controler listener
					addControlerListenerBinding().toInstance(preconfigureWithinDayControlerListener);
				}
            });
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
}
