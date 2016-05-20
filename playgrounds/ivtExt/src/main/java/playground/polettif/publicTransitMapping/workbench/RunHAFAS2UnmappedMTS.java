/* *********************************************************************** *
 * project: org.matsim.*
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


package playground.polettif.publicTransitMapping.workbench;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.polettif.publicTransitMapping.hafas.hafasCreator.PTScheduleCreatorHAFAS;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

/**
 * based on boescpa.converters.scheduleCreator.PTScheduleCreatorDefaultV2
 */
public class RunHAFAS2UnmappedMTS {

	public static void main(String[] args) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		TransitSchedule schedule = scenario.getTransitSchedule();
		Vehicles vehicles = scenario.getVehicles();

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");

		new PTScheduleCreatorHAFAS(schedule, vehicles, transformation).createSchedule(args[0]);

//		ScheduleTools.writeTransitSchedule(schedule, args[1]+"schedule.xml");
		new VehicleWriterV1(vehicles).writeFile(args[1]+"vehicles.xml");
	}

}