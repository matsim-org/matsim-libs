/* *********************************************************************** *
 * project: org.matsim.*
 * STRC2014Runner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.christoph.strc2014;

import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.christoph.parking.ParkingTypes;
import playground.christoph.parking.WithinDayParkingControlerListener;

import java.util.LinkedHashSet;
import java.util.Set;

public class STRC2014Runner {
	
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("");
		} else {
			Config config = ConfigUtils.loadConfig(args[0], new MultiModalConfigGroup());
			Scenario scenario = ScenarioUtils.loadScenario(config);

			fixFirstActivityStartTime(scenario);
			fixMinLinkTravelTime(scenario);
			
			// if multi-modal simulation is enabled
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
			if (multiModalConfigGroup != null && multiModalConfigGroup.isMultiModalSimulationEnabled()) {
				/*
				 * If the network is not multi-modal but multi-modal simulation is enabled,
				 * convert it to multi-modal.
				 */
				if (!NetworkUtils.isMultimodal(scenario.getNetwork())) {
					String simulatedModes = multiModalConfigGroup.getSimulatedModes();
//					// ensure that multi-modal network includes ride and pt links
//					multiModalConfigGroup.setSimulatedModes(simulatedModes + ",ride,pt");
					new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
					multiModalConfigGroup.setSimulatedModes(simulatedModes);
				}
				
//				// drop routes and convert ride to car legs
//				for (Person person : scenario.getPopulation().getPersons().values()) {
//					for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
//						if (planElement instanceof Leg) {
//							Leg leg = (Leg) planElement;
//							leg.setRoute(null);
//							if (leg.getMode().equals(TransportMode.ride)) leg.setMode(TransportMode.car);
//						}
//					}
//				}
			}
			
			
			double capacityFactor = 1.0;
			
			Set<String> initialParkingTypes = new LinkedHashSet<String>();
			initialParkingTypes.add(ParkingTypes.PRIVATEINSIDEPARKING);
			initialParkingTypes.add(ParkingTypes.PRIVATEOUTSIDEPARKING);
			
			Set<String> allParkingTypes = new LinkedHashSet<String>();
			allParkingTypes.add(ParkingTypes.GARAGEPARKING);
			allParkingTypes.add(ParkingTypes.STREETPARKING);
			allParkingTypes.add(ParkingTypes.PRIVATEINSIDEPARKING);
			allParkingTypes.add(ParkingTypes.PRIVATEOUTSIDEPARKING);
			
			// initialize Controler listeners
			MultiModalControlerListener multiModalControlerListener = null;
//			MultiModalControlerListener multiModalControlerListener = new MultiModalControlerListener();
			WithinDayParkingControlerListener parkingControlerListener = new WithinDayParkingControlerListener(scenario,
                    initialParkingTypes, allParkingTypes, capacityFactor);

			/*
			 * Controler listeners are called in reverse order. Since the parkingControlerListener
			 * depends on the outcomes of the multiModalControlerListener, we add the later last.
			 */
			Controler controler = new Controler(scenario);
			controler.addControlerListener(parkingControlerListener);
//			controler.addControlerListener(multiModalControlerListener);
			
			/*
			 * Analysis stuff
			 */
			controler.addControlerListener(new ActivitiesAnalyzer());
			controler.addControlerListener(new TripsAnalyzer());
			
			controler.setOverwriteFiles(true);
			controler.run();
			
			System.exit(0);			
		}
		
	}
	/*
	 * Fix a problem occurring when agents end their first activity at 00:00:00. They are inserted
	 * into the mobsim and create events even before the simulation initialized event has been
	 * created.
	 */
	private static void fixFirstActivityStartTime(Scenario scenario) {
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			if (firstActivity.getEndTime() == 0.0) {
				firstActivity.setEndTime(1.0);
				firstActivity.setMaximumDuration(1.0);
			}
		}
	}
	
	/*
	 * Fix a problem occurring with very short links. There, agents can enter a link and start an
	 * activity immediately, even before they had any chance to replan. This occurs if the link
	 * travel time is < 1.0s (respectively < a time step).
	 */
	private static void fixMinLinkTravelTime(Scenario scenario) {
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (((LinkImpl) link).getFreespeedTravelTime() < 1.0) {
				link.setLength(link.getFreespeed() + 1.0);
			}
		}
	}
}