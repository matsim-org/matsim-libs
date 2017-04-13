/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dziemke.accessibility.input;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.VehicleWriterV1;

public class ScheduleBasedPTFilesCreator {

	public static void main(String[] args) {		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/schedule.xml");
		
		//cut network here, if necessary
		System.out.println(scenario.getTransitSchedule().getTransitLines().size());
		new CreatePseudoNetwork(scenario.getTransitSchedule(),scenario.getNetwork(),"pt").createNetwork();; 
		new NetworkWriter(scenario.getNetwork()).write("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/network.xml");
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(),scenario.getTransitVehicles()).run();
		
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/schedule2.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/vehicles.xml");	
	}
}