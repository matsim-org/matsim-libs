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

import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class CarrierPlanXmlReaderV2WithDtdTest  {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private Carrier testCarrier;

	@Before
	public void setUp() throws Exception{

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		String classInputDirectory = utils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(classInputDirectory + "carrierPlansEquilsWithDtd.xml" );
		testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
	}

	@Test @Ignore
	public void test_whenReadingServices_nuOfServicesIsCorrect(){
		Assert.assertEquals(3,testCarrier.getServices().size());
	}

	@Test
	public void test_whenReadingCarrier_itReadsTypeIdsCorrectly(){

		CarrierVehicle light = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("lightVehicle"));
		Gbl.assertNotNull(light);
		Assert.assertEquals("light",light.getVehicleTypeId().toString());

		CarrierVehicle medium = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("mediumVehicle"));
		Gbl.assertNotNull(medium);
		Assert.assertEquals("medium",medium.getVehicleTypeId().toString());

		CarrierVehicle heavy = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("heavyVehicle"));
		Gbl.assertNotNull(heavy);
		Assert.assertEquals("heavy",heavy.getVehicleTypeId().toString());
	}

	@Test @Ignore
	public void test_whenReadingCarrier_itReadsVehiclesCorrectly(){
		Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = testCarrier.getCarrierCapabilities().getCarrierVehicles();
		Assert.assertEquals(3,carrierVehicles.size());
		Assert.assertTrue(exactlyTheseVehiclesAreInVehicleCollection(Arrays.asList(Id.create("lightVehicle", Vehicle.class),
				Id.create("mediumVehicle", Vehicle.class),Id.create("heavyVehicle", Vehicle.class)),carrierVehicles.values()));
	}

	@Test @Ignore
	public void test_whenReadingCarrier_itReadsFleetSizeCorrectly(){
		Assert.assertEquals(FleetSize.INFINITE, testCarrier.getCarrierCapabilities().getFleetSize());
	}

	@Test @Ignore
	public void test_whenReadingCarrier_itReadsShipmentsCorrectly(){
		Assert.assertEquals(2, testCarrier.getShipments().size());
	}

	@Test @Ignore
	public void test_whenReadingCarrier_itReadsPlansCorrectly(){
		Assert.assertEquals(3, testCarrier.getPlans().size());
	}

	@Test @Ignore
	public void test_whenReadingCarrier_itSelectsPlansCorrectly(){
		Assert.assertNotNull(testCarrier.getSelectedPlan());
	}

	@Test
	public void test_whenReadingCarrierWithFiniteFleet_itSetsFleetSizeCorrectly(){

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		String classInputDirectory = utils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(classInputDirectory + "carrierPlansEquilsFiniteFleetWithDtd.xml" );
		Assert.assertEquals(FleetSize.FINITE, carriers.getCarriers().get(Id.create("testCarrier", Carrier.class)).getCarrierCapabilities().getFleetSize());
	}

	@Test @Ignore
	public void test_whenReadingPlans_nuOfToursIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		Assert.assertEquals(1, plans.get(0).getScheduledTours().size());
		Assert.assertEquals(1, plans.get(1).getScheduledTours().size());
		Assert.assertEquals(1, plans.get(2).getScheduledTours().size());
	}

	@Test @Ignore
	public void test_whenReadingToursOfPlan1_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan1 = plans.get(0);
		ScheduledTour tour1 = plan1.getScheduledTours().iterator().next();
		Assert.assertEquals(5,tour1.getTour().getTourElements().size());
	}

	@Test
	public void test_whenReadingToursOfPlan2_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan2 = plans.get(1);
		ScheduledTour tour1 = plan2.getScheduledTours().iterator().next();
		Assert.assertEquals(9,tour1.getTour().getTourElements().size());
	}

	@Test @Ignore
	public void test_whenReadingToursOfPlan3_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan3 = plans.get(2);
		ScheduledTour tour1 = plan3.getScheduledTours().iterator().next();
		Assert.assertEquals(9,tour1.getTour().getTourElements().size());
	}


	private boolean exactlyTheseVehiclesAreInVehicleCollection(List<Id<Vehicle>> asList, Collection<CarrierVehicle> carrierVehicles) {
		List<CarrierVehicle> vehicles = new ArrayList<>(carrierVehicles);
		for(CarrierVehicle type : carrierVehicles) if(asList.contains(type.getId() )) vehicles.remove(type );
		return vehicles.isEmpty();
	}

}
