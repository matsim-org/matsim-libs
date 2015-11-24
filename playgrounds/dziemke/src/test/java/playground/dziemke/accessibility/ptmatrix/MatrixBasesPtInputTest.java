/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.dziemke.accessibility.ptmatrix;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dziemke
 */
public class MatrixBasesPtInputTest {

	@Rule // test
	public MatsimTestUtils testUtils = new MatsimTestUtils(); // test
	
	private static final Logger log = Logger.getLogger(MatrixBasesPtInputTest.class);

	@Test // test
	public final void test1(){ // test
//	public static void main(String[] args) {
		String transitScheduleFile = "../../matsim/examples/pt-tutorial/transitschedule.xml";
		String networkFile = "../../matsim/examples/pt-tutorial/multimodalnetwork.xml";
		String outputRoot = testUtils.getOutputDirectory();
		
		// writing logfiles to file not needed on build server
//		initLogging(outputRoot);
		
		double departureTime = 8. * 60 * 60;

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(transitScheduleFile);
		
		Map<Id<Coord>, Coord> ptMatrixLocationsMap = new HashMap<Id<Coord>, Coord>();
		
		for (TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
			Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
			Coord coord = transitStopFacility.getCoord();
			ptMatrixLocationsMap.put(id, coord);
		}
				
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFile);

		MatrixBasesPtInputUtils.createStopsFile(ptMatrixLocationsMap, outputRoot + "ptStops.csv", ",");
		
		// The locationFacilitiesMap is passed twice: Once for origins and once for destinations.
		// In other uses the two maps may be different -- thus the duplication here.
		new ThreadedMatrixCreator(scenario, ptMatrixLocationsMap, ptMatrixLocationsMap, departureTime, outputRoot, " ", 1);
		// TODO the TreadedMatrixCreator does not work when started from the test
		// when this same class is titled as the main class instead of as test1 it runs without problems
	}
	
	
//	private static void initLogging(String outputBase) {
//		try	{
//			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputBase);
//		} catch (IOException e)	{
//			log.error("Cannot create logfiles: " + e.getMessage());
//			e.printStackTrace();
//		}
//	}
}