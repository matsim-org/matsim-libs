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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CarrierVehicleTypeLoaderTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private CarrierVehicleTypes types;
	private Carriers carriers;

	@Before
	public void setUp() throws Exception{
		types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(utils.getClassInputDirectory() + "vehicleTypes.xml");
		carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, types ).readFile(utils.getClassInputDirectory() + "carrierPlansEquils.xml" );
	}

	@Test
	public void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarriersUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("lightVehicle"));

		VehicleType vehicleTypeLoaded = v.getType();
		Assert.assertNotNull(vehicleTypeLoaded);

		Assert.assertEquals("light", vehicleTypeLoaded.getId().toString());
		Assert.assertEquals(15, vehicleTypeLoaded.getCapacity().getOther(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(20, vehicleTypeLoaded.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.35, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("gasoline", vehicleTypeLoaded.getEngineInformation().getFuelType().toString());
		Assert.assertEquals(0.02, VehicleUtils.getFuelConsumption(vehicleTypeLoaded), MatsimTestUtils.EPSILON);
	}

	@Test
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);
		Carrier testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
		CarrierVehicle v = CarriersUtils.getCarrierVehicle(testCarrier,Id.createVehicleId("mediumVehicle"));

		VehicleType vehicleTypeLoaded = v.getType();
		Assert.assertNotNull(vehicleTypeLoaded);

		Assert.assertEquals("medium", vehicleTypeLoaded.getId().toString());
		Assert.assertEquals(30, vehicleTypeLoaded.getCapacity().getOther(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(50, vehicleTypeLoaded.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.4, vehicleTypeLoaded.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, vehicleTypeLoaded.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("gasoline", vehicleTypeLoaded.getEngineInformation().getFuelType().toString());
		Assert.assertEquals(0.02, VehicleUtils.getFuelConsumption(vehicleTypeLoaded), MatsimTestUtils.EPSILON);

	}

}
