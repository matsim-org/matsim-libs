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

import java.net.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

public class CarriersAnalysisTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();
	final static URL SCENARIO_URL = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

	@Test
	void runServiceEventTest() {
		// Note: I had to manually change the files for this test to run, as I did not have access to the original input file of the events-file
		// This results in the carrier-plans not being related to the actual events. This is however no problem for testing the core functionality,
		// as those are two disjunct analysis outputs, which do not depend on each other. (aleks Sep'24)
		CarriersAnalysis carriersAnalysis = new CarriersAnalysis(
			IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml" ).toString(),
			testUtils.getInputDirectory() + "in/output_allVehicles.xml",
			IOUtils.extendUrl(SCENARIO_URL, "singleCarrierFiveActivities.xml" ).toString(),
			IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" ).toString(),
			testUtils.getInputDirectory() + "in/serviceBasedEvents.xml",
			testUtils.getOutputDirectory(),
			null);
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersPlans_unPlanned);
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats_unPlanned.tsv",  testUtils.getOutputDirectory() + "Carriers_stats_unPlanned.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats.tsv",  testUtils.getOutputDirectory() + "Carriers_stats.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Load_perVehicle.tsv", testUtils.getOutputDirectory() + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicle.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicleType.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicleType.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perCarrier.tsv", testUtils.getOutputDirectory() + "TimeDistance_perCarrier.tsv");
	}

	@Test
	void runShipmentEventTest() {
		CarriersAnalysis carriersAnalysis = new CarriersAnalysis(
			IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml" ).toString(),
			testUtils.getInputDirectory() + "in/carrierVehicles.xml",
			IOUtils.extendUrl(SCENARIO_URL, "singleCarrierFiveActivities_Shipments.xml" ).toString(),
			IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" ).toString(),
			testUtils.getInputDirectory() + "in/shipmentBasedEvents.xml",
			testUtils.getOutputDirectory(),
			null);
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersPlans_unPlanned);
		carriersAnalysis.runCarrierAnalysis();

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats_unPlanned.tsv",  testUtils.getOutputDirectory() + "Carriers_stats_unPlanned.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats.tsv",  testUtils.getOutputDirectory() + "Carriers_stats.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Load_perVehicle.tsv", testUtils.getOutputDirectory() + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicle.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicleType.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicleType.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perCarrier.tsv", testUtils.getOutputDirectory() + "TimeDistance_perCarrier.tsv");
	}

	@Test
	void runCarriersFileOnlyTest_Shipments() {
		String carrierVehiclesPath = "test/input/org/matsim/freight/carriers/analysis/CarriersAnalysisTest/runShipmentEventTest/in/carrierVehicles.xml";

		CarriersAnalysis carriersAnalysis = new CarriersAnalysis(
			IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml" ).toString(),
			carrierVehiclesPath,
			IOUtils.extendUrl(SCENARIO_URL, "singleCarrierFiveActivities_Shipments.xml" ).toString(),
			IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" ).toString(),
			null,
			testUtils.getOutputDirectory(),
			null);
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersFileOnly);

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats.tsv",  testUtils.getOutputDirectory() + "Carriers_stats.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Load_perVehicle.tsv", testUtils.getOutputDirectory() + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicle.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perVehicleType.tsv", testUtils.getOutputDirectory() + "TimeDistance_perVehicleType.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "TimeDistance_perCarrier.tsv", testUtils.getOutputDirectory() + "TimeDistance_perCarrier.tsv");
	}

	@Test
	void compareCarrierOnlyWithEventsAnalysis() {
		String eventBasedOutputPath = testUtils.getOutputDirectory() + "eventBased/";
		String carrierOnlyOutputPath = testUtils.getOutputDirectory() + "carrierOnly/";

		CarriersAnalysis eventBasedAnalysis = new CarriersAnalysis(
			IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml" ).toString(),
			testUtils.getInputDirectory() + "in/carrierVehicles.xml",
			testUtils.getInputDirectory() + "in/output_carriers.xml.gz",
			IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" ).toString(),
			testUtils.getInputDirectory() + "in/output_events.xml.gz",
			eventBasedOutputPath,
			null);
		eventBasedAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);

		CarriersAnalysis carrierOnlyAnalysis = new CarriersAnalysis(
			IOUtils.extendUrl(SCENARIO_URL, "grid9x9.xml" ).toString(),
			testUtils.getInputDirectory() + "in/carrierVehicles.xml",
			testUtils.getInputDirectory() + "in/output_carriers.xml.gz",
			IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" ).toString(),
			null,
			carrierOnlyOutputPath,
			null);
		carrierOnlyAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersFileOnly);

		MatsimTestUtils.assertEqualFilesLineByLine(eventBasedOutputPath + "Load_perVehicle.tsv", carrierOnlyOutputPath + "Load_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(eventBasedOutputPath + "TimeDistance_perVehicle.tsv", carrierOnlyOutputPath + "TimeDistance_perVehicle.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(eventBasedOutputPath + "TimeDistance_perVehicleType.tsv", carrierOnlyOutputPath + "TimeDistance_perVehicleType.tsv");
		MatsimTestUtils.assertEqualFilesLineByLine(eventBasedOutputPath + "TimeDistance_perCarrier.tsv", carrierOnlyOutputPath + "TimeDistance_perCarrier.tsv");
	}

	@Test
	void runCarriersAnalysisUnPlannedTest_Services() {

		URL carriersFile = IOUtils.extendUrl(SCENARIO_URL, "singleCarrierFiveActivitiesWithoutRoutes.xml" );
		URL vehicleTypesFile = IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" );

		CarrierVehicleTypes vehTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehTypes).readURL(vehicleTypesFile);

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, vehTypes).readURL(carriersFile);

		CarriersAnalysis carriersAnalysis = new CarriersAnalysis(carriers, testUtils.getOutputDirectory());
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersPlans_unPlanned);

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats_unPlanned.tsv",  testUtils.getOutputDirectory() + "Carriers_stats_unPlanned.tsv");
	}

	@Test
	void runCarriersAnalysisUnPlannedTest_Shipments() {

		URL carriersFile = IOUtils.extendUrl(SCENARIO_URL, "singleCarrierFiveActivitiesWithoutRoutes_Shipments.xml" );
		URL vehicleTypesFile = IOUtils.extendUrl(SCENARIO_URL, "vehicleTypes.xml" );

		CarrierVehicleTypes vehTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehTypes).readURL(vehicleTypesFile);

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, vehTypes).readURL(carriersFile);

		CarriersAnalysis carriersAnalysis = new CarriersAnalysis(carriers, testUtils.getOutputDirectory());
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersPlans_unPlanned);

		MatsimTestUtils.assertEqualFilesLineByLine(testUtils.getInputDirectory() + "Carriers_stats_unPlanned.tsv",  testUtils.getOutputDirectory() + "Carriers_stats_unPlanned.tsv");
	}
}
