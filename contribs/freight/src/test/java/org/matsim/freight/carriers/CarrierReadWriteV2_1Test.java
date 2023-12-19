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

package org.matsim.freight.carriers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

public class CarrierReadWriteV2_1Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void readWriteTest() throws FileNotFoundException, IOException {

		Carriers carriers = new Carriers(Collections.emptyList());
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

		String inputFilename = utils.getClassInputDirectory() + "carriers.xml";
		String outputFilename = utils.getOutputDirectory() + "outputCarriers.xml";

		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(carrierVehicleTypes);
		vehicleTypeReader.readFile(utils.getClassInputDirectory() + "vehicles.xml");

		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(carriers, carrierVehicleTypes);
		carrierReader.readFile(inputFilename);

		new CarrierPlanXmlWriterV2_1(carriers).write(outputFilename);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename);
	}


	@Test
	void readWriteReadTest() throws FileNotFoundException, IOException {

		Carriers carriers = new Carriers(Collections.emptyList());
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();

		String inputFilename = utils.getClassInputDirectory() + "/carriers.xml";
		String outputFilename = utils.getOutputDirectory() + "/outputCarriers.xml";
		String outputFilename2 = utils.getOutputDirectory() + "/outputCarriers2.xml";

		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(carrierVehicleTypes);
		vehicleTypeReader.readFile(utils.getClassInputDirectory() + "vehicles.xml");

		CarrierPlanXmlReader reader1 = new CarrierPlanXmlReader(carriers, carrierVehicleTypes);
		reader1.readFile(utils.getClassInputDirectory() + "carriers.xml");

		new CarrierPlanXmlWriterV2_1(carriers).write(outputFilename);

		carriers.getCarriers().clear();

		CarrierPlanXmlReader reader2 = new CarrierPlanXmlReader(carriers, carrierVehicleTypes);
		reader2.readFile(outputFilename);

		new CarrierPlanXmlWriterV2_1(carriers).write(outputFilename2);

		MatsimTestUtils.assertEqualFilesLineByLine(inputFilename, outputFilename2);
	}
}
