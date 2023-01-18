/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CarrierPlanReaderV1Test.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 */

package org.matsim.contrib.freight.carrier;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 11:46 AM To
 * change this template use File | Settings | File Templates.
 */
public class CarrierPlanReaderV1Test {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testCarrierPlanReaderDoesSomething() {

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		VehicleType defaultVehicleType = VehicleUtils.getFactory().createVehicleType( Id.create("default", VehicleType.class ) );
		carrierVehicleTypes.getVehicleTypes().put( defaultVehicleType.getId(), defaultVehicleType );

		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers, carrierVehicleTypes );
		carrierPlanReaderV1.readFile(utils.getClassInputDirectory() + "carrierPlansEquils.xml");
		Assert.assertEquals(1, carriers.getCarriers().size());
	}

	@Test
	public void testReaderReadsCorrectly() {

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		VehicleType defaultVehicleType = VehicleUtils.getFactory().createVehicleType( Id.create("default", VehicleType.class ) );
		carrierVehicleTypes.getVehicleTypes().put( defaultVehicleType.getId(), defaultVehicleType );

		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers, carrierVehicleTypes );
		carrierPlanReaderV1.readFile(utils.getClassInputDirectory() + "carrierPlansEquils.xml");
		Assert.assertEquals(1, carriers.getCarriers().size());
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		Assert.assertEquals(1, carrier.getSelectedPlan().getScheduledTours().size());
		Leg leg = (Leg) carrier.getSelectedPlan().getScheduledTours()
				.iterator().next().getTour().getTourElements().get(0);
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals("23", route.getStartLinkId().toString());
		Assert.assertEquals("2", route.getLinkIds().get(0).toString());
		Assert.assertEquals("3", route.getLinkIds().get(1).toString());
		Assert.assertEquals("4", route.getLinkIds().get(2).toString());
		Assert.assertEquals("15", route.getEndLinkId().toString());
	}

	@Test
	public void testReaderReadsScoreAndSelectedPlanCorrectly() {

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		VehicleType defaultVehicleType = VehicleUtils.getFactory().createVehicleType( Id.create("default", VehicleType.class ) );
		carrierVehicleTypes.getVehicleTypes().put( defaultVehicleType.getId(), defaultVehicleType );

		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers, carrierVehicleTypes );
		carrierPlanReaderV1.readFile(utils.getClassInputDirectory() + "carrierPlansEquils.xml");
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		Assert.assertNotNull(carrier.getSelectedPlan());
		Assert.assertEquals(-100.0, carrier.getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2,carrier.getPlans().size());
	}

	@Test
	public void testReaderReadsUnScoredAndUnselectedPlanCorrectly() {

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		VehicleType defaultVehicleType = VehicleUtils.getFactory().createVehicleType( Id.create("default", VehicleType.class ) );
		carrierVehicleTypes.getVehicleTypes().put( defaultVehicleType.getId(), defaultVehicleType );

		Carriers carriers = new Carriers();
		CarrierPlanReaderV1 carrierPlanReaderV1 = new CarrierPlanReaderV1(carriers, carrierVehicleTypes );
		carrierPlanReaderV1.readFile(utils.getClassInputDirectory() + "carrierPlansEquils_unscored_unselected.xml");
		Carrier carrier = carriers.getCarriers().values().iterator().next();
		Assert.assertNull(carrier.getSelectedPlan());
		Assert.assertEquals(2,carrier.getPlans().size());
	}

}
