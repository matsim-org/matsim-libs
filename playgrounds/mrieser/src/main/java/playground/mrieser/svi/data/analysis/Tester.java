/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoriesReader;

/**
 * @author mrieser
 */
public class Tester {

	public static void main(final String[] args) {

		String zoneMappingFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/l41_ZoneNo_TAZ_mapping.csv";
		String networkFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/matsim/network.cleaned.xml";
		String vehTrajectoryFilename = "/Users/cello/Desktop/VehTrajectory.dat";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		
		ZoneIdToIndexMapping zoneMapping = new ZoneIdToIndexMapping();
		new ZoneIdToIndexMappingReader(zoneMapping).readFile(zoneMappingFilename);

		DynamicTravelTimeMatrix matrix = new DynamicTravelTimeMatrix(600, 30*3600.0);
//		new VehicleTrajectoriesReader(new CalculateTravelTimeMatrixFromVehTrajectories(matrix), zoneMapping).readFile(vehTrajectoryFilename);
		
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		new VehicleTrajectoriesReader(new CalculateLinkTravelTimesFromVehTrajectories(ttcalc, scenario.getNetwork()), zoneMapping).readFile(vehTrajectoryFilename);
		matrix.dump();
	}
}
