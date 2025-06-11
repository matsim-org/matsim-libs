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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.nio.file.Path;

public class CarrierVehicleTypeLoaderTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private Carriers carriers;

	@BeforeEach
	public void setUp() {
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(Path.of(utils.getClassInputDirectory()).getParent().resolve("vehicleTypes_v2.xml").toString());
		carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, types).readFile(utils.getClassInputDirectory() + "carrierPlansEquils.xml" );
	}

	@Test
	void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
//		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarriersUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("lightVehicle"));

		assert v != null;
		VehicleType vehicleTypeLoaded = v.getType();
		Assertions.assertNotNull(vehicleTypeLoaded);

		Assertions.assertEquals("light", vehicleTypeLoaded.getId().toString());
		Assertions.assertEquals(15, vehicleTypeLoaded.getCapacity().getOther(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(20, vehicleTypeLoaded.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0.35, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals("gasoline", VehicleUtils.getHbefaTechnology(vehicleTypeLoaded.getEngineInformation()));
		Assertions.assertEquals(0.02, VehicleUtils.getFuelConsumptionLitersPerMeter(vehicleTypeLoaded.getEngineInformation()), MatsimTestUtils.EPSILON);
	}

	@Test
	void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
//		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarriersUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("mediumVehicle"));

		assert v != null;
		VehicleType vehicleTypeLoaded = v.getType();
		Assertions.assertNotNull(vehicleTypeLoaded);

		Assertions.assertEquals("medium", vehicleTypeLoaded.getId().toString());
		Assertions.assertEquals(30, vehicleTypeLoaded.getCapacity().getOther(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(50, vehicleTypeLoaded.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0.4, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);

		Assertions.assertEquals("gasoline", VehicleUtils.getHbefaTechnology(vehicleTypeLoaded.getEngineInformation()));
		Assertions.assertEquals(0.02, VehicleUtils.getFuelConsumptionLitersPerMeter(vehicleTypeLoaded.getEngineInformation()), MatsimTestUtils.EPSILON);

	}

}
