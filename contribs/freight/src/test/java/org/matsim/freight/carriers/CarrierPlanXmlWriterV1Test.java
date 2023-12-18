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
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 */
public class CarrierPlanXmlWriterV1Test {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testCarrierPlanWriterWrites() {

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( testUtils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		VehicleType defaultVehicleType = VehicleUtils.getFactory().createVehicleType( Id.create("default", VehicleType.class ) );
		carrierVehicleTypes.getVehicleTypes().put( defaultVehicleType.getId(), defaultVehicleType );

		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers, carrierVehicleTypes );
		carrierPlanReaderV1.readFile(testUtils.getClassInputDirectory() + "carrierPlansEquils.xml");
		CarrierPlanXmlWriterV1 planWriter = new CarrierPlanXmlWriterV1(carriers.getCarriers().values());
		planWriter.write(testUtils.getOutputDirectory() + "carrierPlansEquilsWritten.xml");
	}


}
