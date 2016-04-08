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


package playground.polettif.multiModalMap.workbench;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.polettif.boescpa.converters.osm.OSM2MixedIVT;
import playground.polettif.multiModalMap.hafas.hafasCreator.PTScheduleCreatorHAFAS;

/**
 * based on boescpa.converters.scheduleCreator.PTScheduleCreatorDefaultV2
 */
public class RunHAFAS2UnmappedMTS {

	public static void run(String osmFile, String  hafasFolder, String vehicleFile_Mixed, String vehicleFile_OnlyPT, Coord cutCenter, int cutRadius, Coord cutNW, Coord cutSE, String outbase) {

		String[] inputArgs = new String[12];

		inputArgs[0] = osmFile;
		inputArgs[1] = hafasFolder;
		inputArgs[2] = vehicleFile_Mixed;
		inputArgs[3] = vehicleFile_OnlyPT;

		inputArgs[4] = Double.toString(cutCenter.getX());
		inputArgs[5] = Double.toString(cutCenter.getY());
		inputArgs[6] = Integer.toString(cutRadius);
		inputArgs[7] = Double.toString(cutNW.getX());
		inputArgs[8] = Double.toString(cutNW.getY());
		inputArgs[9] = Double.toString(cutSE.getX());
		inputArgs[10] = Double.toString(cutSE.getY());

		inputArgs[11] = outbase;

		OSM2MixedIVT.main(inputArgs);
	}

	public static void main(String[] args) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		TransitSchedule schedule = scenario.getTransitSchedule();
		Vehicles vehicles = scenario.getVehicles();

		new PTScheduleCreatorHAFAS(schedule, vehicles, null).createSchedule("C:/Users/polettif/Desktop/data/hafas/");

		new TransitScheduleWriter(schedule).writeFile("C:/Users/polettif/Desktop/output/hafas2mts/schedule.xml");
		new VehicleWriterV1(vehicles).writeFile("C:/Users/polettif/Desktop/output/hafas2mts/vehicles.xml");
	}

}