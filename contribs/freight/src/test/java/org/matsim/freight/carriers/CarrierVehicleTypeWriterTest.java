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
import org.matsim.testcases.MatsimTestUtils;

public class CarrierVehicleTypeWriterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void testTypeWriter(){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(utils.getPackageInputDirectory()+ "vehicleTypes_v2.xml");
		final String outputVehTypeFile = utils.getOutputDirectory()+ "vehicleTypesWritten.xml";
		new CarrierVehicleTypeWriter(types).write(outputVehTypeFile);
		types.getVehicleTypes().clear();
		new CarrierVehicleTypeReader(types).readFile(outputVehTypeFile);
	}
}
