/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
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

package org.matsim.freight.carriers.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Path;

public class FreightAnalysisEventBasedTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void runServiceEventTest() throws IOException {
		// Note: I had to manually change the files for this test to run, as I did not have access to the original input file of the events-file
		// This results in the carrier-plans not being related to the actual events. This is however no problem for testing the core functionality,
		// as those are two disjunct analysis outputs, which do not depend on each other. (aleks Sep'24)

		RunFreightAnalysisEventBased analysisEventBased = new RunFreightAnalysisEventBased(
			Path.of(testUtils.getInputDirectory() + "in/grid9x9.xml"),
			Path.of(testUtils.getInputDirectory() + "in/carrierVehicles.xml"),
			Path.of(testUtils.getInputDirectory() + "in/carrierWithServices.xml"),
			Path.of(testUtils.getInputDirectory() + "in/carrierVehicles.xml"),
			Path.of(testUtils.getInputDirectory() + "in/serviceBasedEvents.xml"),
			Path.of(testUtils.getOutputDirectory()),
			null);
		analysisEventBased.runAnalysis();

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carrier_stats.tsv",  testUtils.getOutputDirectory() + "Carrier_stats.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Load_perVehicle.tsv", testUtils.getOutputDirectory() + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicle.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicleType.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicleType.tsv");
	}

	@Test
	void runShipmentEventTest() throws IOException {
		RunFreightAnalysisEventBased analysisEventBased = new RunFreightAnalysisEventBased(
			Path.of(testUtils.getInputDirectory() + "in/grid9x9.xml"),
			Path.of(testUtils.getInputDirectory() + "in/carrierVehicles.xml"),
			Path.of(testUtils.getInputDirectory() + "in/carrierWithShipments.xml"),
			Path.of(testUtils.getInputDirectory() + "in/carrierVehicles.xml"),
			Path.of(testUtils.getInputDirectory() + "in/shipmentBasedEvents.xml"),
			Path.of(testUtils.getOutputDirectory()),
			null);
		analysisEventBased.runAnalysis();

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Load_perVehicle.tsv", testUtils.getOutputDirectory() + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicle.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicleType.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicleType.tsv");
	}
}
