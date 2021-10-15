/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.analysis;

import org.junit.*;
import org.matsim.testcases.MatsimTestUtils;

public class RunFreightAnalysisIT {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void runAnalysis(){
		final String packageInputDirectory = testUtils.getClassInputDirectory();
		final String outputDirectory = testUtils.getOutputDirectory();
		RunFreightAnalysis freightAnalysis = new RunFreightAnalysis(packageInputDirectory, outputDirectory);
		freightAnalysis.runAnalysis();
	}

	@Test
	public void compareResults() {
		//some generale stats
		checkFile("carrierStats.tsv");
		checkFile("freightVehicleStats.tsv");
		checkFile("freightVehicleTripStats.tsv");
		checkFile("serviceStats.tsv");
		checkFile("shipmentStats.tsv");

		//Carrier specific stats
		checkFile("carrier_carrier1_ServiceStats.tsv");
		checkFile("carrier_carrier1_ShipmentStats.tsv");
		checkFile("carrier_carrier1_VehicleTypeStats.tsv");
		checkFile("carrier_##carrier1_tripStats.tsv");  //Note: The "?" is here, because the carrierId was guessed depending on the vehicleId.
		checkFile("carrier_##carrier1_vehicleStats.tsv"); //Note: The "?" is here, because the carrierId was guessed depending on the vehicleId.
	}

	private void checkFile(String filename) {
		final String inputFilename = testUtils.getInputDirectory() + filename;
		final String outputFilename = testUtils.getOutputDirectory() + filename;
		MatsimTestUtils.compareFilesLineByLine(inputFilename, outputFilename);
	}
}