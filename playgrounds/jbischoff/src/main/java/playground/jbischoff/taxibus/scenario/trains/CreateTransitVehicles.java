/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.trains;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.VehicleWriterV1;


/**
 * @author  jbischoff
 *
 */
public class CreateTransitVehicles {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/braunschweig/bs-network.xml");
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/braunschweig/bs-scheduleNetwork.xml");
		
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(),scenario.getTransitVehicles()).run();;
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/new/transitvehicles.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/new/bs-scheduleNetwork.xml");
	}
	

}
