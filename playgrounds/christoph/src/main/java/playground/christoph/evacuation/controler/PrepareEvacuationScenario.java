/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareEvacuationScenario.java
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

package playground.christoph.evacuation.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;

/**
 * Prepares a scenario to be used in an evacuation simulation.
 * <ul>
 * 	<li>adapt network capacities and speeds</li>
 * 	<li>connect facilities to network</li>
 * 	<li>add exit links to network</li>
 * 	<li>add pickup facilities</li>
 * 	<li>add z coordinates to network</li>
 * </ul>
 */
public class PrepareEvacuationScenario {

	public void prepareScenario(Scenario scenario) {
		
		Config config = scenario.getConfig();
		Network network = scenario.getNetwork();
		ActivityFacilities facilities = ((ScenarioImpl) scenario).getActivityFacilities(); 
		
		/*
		 * Adapt network capacities and speeds.
		 * So far we do not support time dependent capacities/speeds.
		 */
		for (Link link : network.getLinks().values()) {

			if (EvacuationConfig.capacityFactor != 1.0) link.setCapacity(link.getCapacity() * EvacuationConfig.capacityFactor);
			if (EvacuationConfig.speedFactor != 1.0) link.setFreespeed(link.getFreespeed() * EvacuationConfig.speedFactor);
		}
		
		// connect facilities to links
		new WorldConnectLocations(config).connectFacilitiesWithLinks(facilities, (NetworkImpl) network);

		// Add Rescue Links to Network
		new AddExitLinksToNetwork(scenario).createExitLinks();

		// Add secure Facilities to secure Links.
//		new AddSecureFacilitiesToNetwork(this.scenarioData).createSecureFacilities();
		
		// Add pickup facilities to Links.
		for (Link link : scenario.getNetwork().getLinks().values()) {
			/*
			 * Create and add the pickup facility and add activity option ("pickup")
			 */
			String idString = link.getId().toString() + "_pickup";
			ActivityFacility secureFacility = ((ActivityFacilitiesImpl) facilities).createFacility(scenario.createId(idString), link.getCoord());
			((ActivityFacilityImpl)secureFacility).setLinkId(((LinkImpl)link).getId());
			
			ActivityOption activityOption = ((ActivityFacilityImpl)secureFacility).createActivityOption("pickup");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
		}
		
		// Add z-coordinates to the network
		AddZCoordinatesToNetwork zCoordinateAdder = new AddZCoordinatesToNetwork(scenario, EvacuationConfig.dhm25File, EvacuationConfig.srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		zCoordinateAdder.checkSteepness();
	}
}