/* *********************************************************************** *
 * project: org.matsim.*
 * InwilRunner.java
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

package playground.christoph.dissertation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.withinday.controller.WithinDayControlerListener;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.controler.EvacuationControlerListener;
import playground.christoph.evacuation.controler.PreconfigureMultiModalControlerListener;
import playground.christoph.evacuation.controler.PreconfigureWithinDayControlerListener;
import playground.christoph.evacuation.controler.PrepareEvacuationScenarioListener;

public class InwilRunner {

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
			Config config = ConfigUtils.loadConfig(args[0], new MultiModalConfigGroup());
			
			/*
			 * Workaround:
			 * Using transit schedule and vehicles cannot be enabled in the config. Otherwise
			 * TransitRoutes are created for pt legs when reading the population file. Therefore,
			 * the population file is removed from the config file, before reading the scenario.
			 * Afterwards, the population is read separately.
			 */
//			String populationFile = config.plans().getInputFile();
//			config.plans().setInputFile(null);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			config.scenario().setUseTransit(true);
			config.scenario().setUseVehicles(true);
			new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());
			new VehicleReaderV1(((ScenarioImpl) scenario).getVehicles()).readFile(config.transit().getVehiclesFile());
			config.scenario().setUseTransit(false);
			config.scenario().setUseVehicles(false);
			
//			new MatsimPopulationReader(scenario).parse(populationFile);
			
//			boolean useTransit = config.scenario().isUseTransit();
//			boolean useVehicles = config.scenario().isUseVehicles();
//			
//			// workaround to load transit schedule and vehicles even if they are not simulated physically
//			boolean enableUseTransit = (useTransit == false);
//			boolean enableUseVehicles = (useVehicles == false);
//			
//			if (enableUseTransit) config.scenario().setUseTransit(true);
//			if (enableUseVehicles) config.scenario().setUseTransit(true);						
//			Scenario scenario = ScenarioUtils.loadScenario(config);
//			if (enableUseTransit) config.scenario().setUseTransit(false);
//			if (enableUseVehicles) config.scenario().setUseTransit(false);
			
			new EvacuationConfigReader().readFile(args[1]);
			EvacuationConfig.deterministicRNGOffset = Long.parseLong(args[2]);
			EvacuationConfig.printConfig();

			/*
			 * This is a workaround so far...
			 * Update allowed modes in the network - this is necessary since it is now multi-modal.
			 * The routers for pt and ride will use only links that support pt respectively ride mode. 
			 */
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
			if (!NetworkUtils.isMultimodal(scenario.getNetwork())) {
				new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
			}
			for (Link link : scenario.getNetwork().getLinks().values()) {
				if (link.getAllowedModes().contains(TransportMode.car)) {
					if (!link.getAllowedModes().contains(TransportMode.ride)) {
						Set<String> allowedModes = new HashSet<String>(link.getAllowedModes());
						allowedModes.add(TransportMode.ride);
						link.setAllowedModes(allowedModes);
					}
					if (!link.getAllowedModes().contains(TransportMode.pt)) {
						Set<String> allowedModes = new HashSet<String>(link.getAllowedModes());
						allowedModes.add(TransportMode.pt);
						link.setAllowedModes(allowedModes);
					}
				}
			}
			
			/*
			 * If an agent's plan ends at 0:00:00, its state is changed from activity to leg
			 * immediately when the agent is created, even before the simulation is started.
			 * This confuses some parts of the code. Therefore, we shift such activities by
			 * one second.
			 */
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan plan = person.getSelectedPlan();
				Activity activity = (Activity) plan.getPlanElements().get(0);
				if (activity.getEndTime() == 0.0 || (activity.getStartTime() == 0.0 && activity.getMaximumDuration() == 0.0)) {
					activity.setEndTime(1.0);
					activity.setMaximumDuration(1.0);
				}
			}
			
			// Prepare scenario - this could be done as pre-processing step
			PrepareEvacuationScenarioListener prepareEvacuationScenario = new PrepareEvacuationScenarioListener();
			prepareEvacuationScenario.prepareScenario(scenario);
						
			final Controler controler = new Controler(scenario);

			// Use a Scoring Function, that only scores the travel times!
			controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
			
			/*
			 * Adapt walk- and bike speed according to car speed reduction.
			 * This has to be done before walk and bike travel time objects
			 * have been created.
			 */
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * EvacuationConfig.speedFactor);
			config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike) * EvacuationConfig.speedFactor);
		
			// create Controler Listeners; add them afterwards in a meaningful order
			MultiModalControlerListener multiModalControlerListener = new MultiModalControlerListener();
			WithinDayControlerListener withinDayControlerListener = new WithinDayControlerListener();
			PreconfigureMultiModalControlerListener preconfigureMultiModalControlerListener = new PreconfigureMultiModalControlerListener(
					multiModalControlerListener);
			PreconfigureWithinDayControlerListener preconfigureWithinDayControlerListener = new PreconfigureWithinDayControlerListener(
					withinDayControlerListener, multiModalControlerListener);
			EvacuationControlerListener evacuationControlerListener = new EvacuationControlerListener(withinDayControlerListener,
					multiModalControlerListener);
			
			// Analysis stuff
			controler.addControlerListener(new ActivitiesAnalyzer());
			controler.addControlerListener(new TripsAnalyzer());
			
			// Evacuation stuff
			controler.addControlerListener(evacuationControlerListener);
			
			// Within-day Replanning
			withinDayControlerListener.setModesAnalyzedByTravelTimeCollector(CollectionUtils.stringToSet(TransportMode.car));
			controler.addControlerListener(withinDayControlerListener);
			
			// pre-configure within-day controler listener with outcomes from the multi-modal controler listener
			controler.addControlerListener(preconfigureWithinDayControlerListener);			
			
			// Configuration
			controler.addControlerListener(multiModalControlerListener);
			
			// pre-configure multi-modal controler listener
			controler.addControlerListener(preconfigureMultiModalControlerListener);			
			
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
	
//	private void loadTransit() throws UncheckedIOException {
//		new TransitScheduleReader(this.scenario).readFile(this.config.transit().getTransitScheduleFile());
//		if ((this.config.transit() != null) && (this.config.transit().getTransitLinesAttributesFile() != null)) {
//			String transitLinesAttributesFileName = this.config.transit().getTransitLinesAttributesFile();
//			log.info("loading transit lines attributes from " + transitLinesAttributesFileName);
//			new ObjectAttributesXmlReader(this.scenario.getTransitSchedule().getTransitLinesAttributes()).parse(transitLinesAttributesFileName);
//		}
//		if ((this.config.transit() != null) && (this.config.transit().getTransitStopsAttributesFile() != null)) {
//			String transitStopsAttributesFileName = this.config.transit().getTransitStopsAttributesFile();
//			log.info("loading transit stop facilities attributes from " + transitStopsAttributesFileName);
//			new ObjectAttributesXmlReader(this.scenario.getTransitSchedule().getTransitStopsAttributes()).parse(transitStopsAttributesFileName);
//		}
//	}
//
//	private void loadVehicles() throws UncheckedIOException {
//		new VehicleReaderV1(this.scenario.getVehicles()).readFile(this.config.transit().getVehiclesFile());
//	}
}
