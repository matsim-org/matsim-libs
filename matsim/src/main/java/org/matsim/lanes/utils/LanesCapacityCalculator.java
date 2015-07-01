/* *********************************************************************** *
 * project: org.matsim.*
 * LanesCapacityCalculator
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
package org.matsim.lanes.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.MatsimLaneDefinitionsWriter;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;


/**
 * @author dgrether
 *
 */
public class LanesCapacityCalculator {

	/**
	 * Calculate capacity by formular from Neumann2008DA:
	 * 
	 * Flow of a Lane is given by the flow of the link divided by the number of lanes represented by the link.
	 * 
	 * A Lane may represent one or more lanes in reality. This is given by the attribute numberOfRepresentedLanes of
	 * the Lane definition. The flow of a lane is scaled by this number.
	 */
	public void calculateAndSetCapacity(Lane lane, boolean isLaneAtLinkEnd, Link link, Network network){
		if (isLaneAtLinkEnd){
			double noLanesLink = link.getNumberOfLanes();
			double linkFlowCapPerSecondPerLane = link.getCapacity() / network.getCapacityPeriod()
					/ noLanesLink;
			double laneFlowCapPerHour = lane.getNumberOfRepresentedLanes()
					* linkFlowCapPerSecondPerLane * 3600.0;
			lane.setCapacityVehiclesPerHour(laneFlowCapPerHour);
		}
		else {
			double capacity = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
			lane.setCapacityVehiclesPerHour(capacity);
		}
	}
	
	
	
	public static void calculateMissingCapacitiesForLanes20(String networkInputFilename, String lanes20InputFilename, String lanes20OutputFilename){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkInputFilename);
		config.scenario().setUseLanes(true);
		config.network().setLaneDefinitionsFile(lanes20InputFilename);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		LanesCapacityCalculator calc = new LanesCapacityCalculator();
		LaneDefinitions20 lanes = (LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME);
		for (LanesToLinkAssignment20 l2l : lanes.getLanesToLinkAssignments().values()){
			Link link = network.getLinks().get(l2l.getLinkId());
			for (Lane lane : l2l.getLanes().values()){
				if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){
					calc.calculateAndSetCapacity(lane, true, link, network);
				}
				else {
					calc.calculateAndSetCapacity(lane, false, link, network);
				}
			}
		}
		MatsimLaneDefinitionsWriter writer = new MatsimLaneDefinitionsWriter();
		writer.writeFile20(lanes20OutputFilename, lanes);
	}
	
	
}
