/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayParkingRunner.java
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

package playground.christoph.parking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.NetworkUtils;

import playground.christoph.parking.core.facilities.OtherFacilityCreator;
import playground.christoph.parking.core.facilities.ParkingFacilityCreator;

public class WithinDayParkingRunner {
	
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("");
		} else {
			Config config = ConfigUtils.loadConfig(args[0], new MultiModalConfigGroup());
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
			// if multi-modal simulation is enabled
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
			if (multiModalConfigGroup != null && multiModalConfigGroup.isMultiModalSimulationEnabled()) {
				/*
				 * If the network is not multi-modal but multi-modal simulation is enabled,
				 * convert it to multi-modal.
				 */
				if (!NetworkUtils.isMultimodal(scenario.getNetwork())) {
					String simulatedModes = multiModalConfigGroup.getSimulatedModes();
					// ensure that multi-modal network includes ride and pt links
					multiModalConfigGroup.setSimulatedModes(simulatedModes + ",ride,pt");
					new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
					multiModalConfigGroup.setSimulatedModes(simulatedModes);
				}
				
				// drop routes and convert ride to car legs
				for (Person person : scenario.getPopulation().getPersons().values()) {
					for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
						if (planElement instanceof Leg) {
							Leg leg = (Leg) planElement;
							leg.setRoute(null);
							if (leg.getMode().equals(TransportMode.ride)) leg.setMode(TransportMode.car);
						}
					}
				}
			}

			OtherFacilityCreator.createParkings(scenario);
			
			int i = 0;
			int mod = 3;
			for (Link link : scenario.getNetwork().getLinks().values()) {
				if (i++ % mod == 0) ParkingFacilityCreator.createParkings(scenario, link, ParkingTypes.PARKING);
//				if (i++ % mod == 0) ParkingFacilityCreator.createParkings(scenario, link, ParkingTypes.PARKING, 1000.0);
			}
			
			Controler controler = new Controler(scenario);
			
			// initialize Controler listeners
			MultiModalControlerListener multiModalControlerListener = new MultiModalControlerListener();
			WithinDayParkingControlerListener parkingControlerListener = new WithinDayParkingControlerListener(controler,
					multiModalControlerListener);

			/*
			 * Controler listeners are called in reverse order. Since the parkingControlerListener
			 * depends on the outcomes of the multiModalControlerListener, we add the later last.
			 */
			controler.addControlerListener(parkingControlerListener);
			controler.addControlerListener(multiModalControlerListener);
			
			controler.setOverwriteFiles(true);
			controler.run();
			
			System.exit(0);			
		}		
	}
}