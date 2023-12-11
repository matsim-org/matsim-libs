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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CarrierPlanXmlWriterV2_1Test {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	private Carrier testCarrier;

	@BeforeEach
	public void setUp() throws Exception{

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( this.testUtils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(this.testUtils.getClassInputDirectory() + "carrierPlansEquils.xml" );
		new CarrierPlanXmlWriterV2_1(carriers).write(this.testUtils.getClassInputDirectory() + "carrierPlansEquilsWritten.xml");
		carriers.getCarriers().clear();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(this.testUtils.getClassInputDirectory() + "carrierPlansEquilsWritten.xml" );
		testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
	}

	@Test
	void test_whenReadingServices_nuOfServicesIsCorrect(){
		assertEquals(3,testCarrier.getServices().size());
	}

	@Test
	void test_whenReadingCarrier_itReadsTypeIdsCorrectly(){

		CarrierVehicle light = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("lightVehicle"));
		assertEquals("light",light.getVehicleTypeId().toString());

		CarrierVehicle medium = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("mediumVehicle"));
		assertEquals("medium",medium.getVehicleTypeId().toString());

		CarrierVehicle heavy = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("heavyVehicle"));
		assertEquals("heavy",heavy.getVehicleTypeId().toString());
	}

	@Test
	void test_whenReadingCarrier_itReadsVehiclesCorrectly(){
		Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = testCarrier.getCarrierCapabilities().getCarrierVehicles();
		assertEquals(3,carrierVehicles.size());
		assertTrue(exactlyTheseVehiclesAreInVehicleCollection(Arrays.asList(Id.create("lightVehicle", Vehicle.class),
				Id.create("mediumVehicle", Vehicle.class),Id.create("heavyVehicle", Vehicle.class)),carrierVehicles.values()));
	}

	@Test
	void test_whenReadingCarrier_itReadsFleetSizeCorrectly(){
		assertEquals(FleetSize.INFINITE, testCarrier.getCarrierCapabilities().getFleetSize());
	}

	@Test
	void test_whenReadingCarrier_itReadsShipmentsCorrectly(){
		assertEquals(2, testCarrier.getShipments().size());
	}

	@Test
	void test_whenReadingCarrier_itReadsPlansCorrectly(){
		assertEquals(3, testCarrier.getPlans().size());
	}

	@Test
	void test_whenReadingCarrier_itSelectsPlansCorrectly(){
		assertNotNull(testCarrier.getSelectedPlan());
	}

	@Test
	void test_whenReadingPlans_nuOfToursIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		assertEquals(1, plans.get(0).getScheduledTours().size());
		assertEquals(1, plans.get(1).getScheduledTours().size());
		assertEquals(1, plans.get(2).getScheduledTours().size());
	}

	@Test
	void test_whenReadingToursOfPlan1_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan1 = plans.get(0);
		ScheduledTour tour1 = plan1.getScheduledTours().iterator().next();
		assertEquals(5,tour1.getTour().getTourElements().size());
	}

	@Test
	void test_whenReadingToursOfPlan2_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan2 = plans.get(1);
		ScheduledTour tour1 = plan2.getScheduledTours().iterator().next();
		assertEquals(9,tour1.getTour().getTourElements().size());
	}

	@Test
	void test_whenReadingToursOfPlan3_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan3 = plans.get(2);
		ScheduledTour tour1 = plan3.getScheduledTours().iterator().next();
		assertEquals(9,tour1.getTour().getTourElements().size());
	}

	@Test
	void test_whenReadingToursOfPlan1_SpritScoreIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan1 = plans.get(0);
		plan1.getAttributes().getAttribute("jspritScore");
		assertEquals(Double.NaN, CarriersUtils.getJspritScore(plan1), testUtils.EPSILON);
	}

	@Test
	void test_whenReadingToursOfPlan2_jSpritScoreIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan2 = plans.get(1);
		plan2.getAttributes().getAttribute("jspritScore");
		assertEquals(80.0, CarriersUtils.getJspritScore(plan2), testUtils.EPSILON);
	}

	@Test
	void test_whenReadingToursOfPlan3_jSpritIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<CarrierPlan>(testCarrier.getPlans());
		CarrierPlan plan3 = plans.get(2);
		plan3.getAttributes().getAttribute("jspritScore");
		assertEquals(105.0, CarriersUtils.getJspritScore(plan3), testUtils.EPSILON);
	}


	private boolean exactlyTheseVehiclesAreInVehicleCollection(List<Id<Vehicle>> asList, Collection<CarrierVehicle> carrierVehicles) {
		List<CarrierVehicle> vehicles = new ArrayList<CarrierVehicle>(carrierVehicles);
		for(CarrierVehicle type : carrierVehicles) if(asList.contains(type.getId() )) vehicles.remove(type );
		return vehicles.isEmpty();
	}

	@Test
	void test_CarrierHasAttributes(){
		assertEquals((TransportMode.drt), CarriersUtils.getCarrierMode(testCarrier));
		assertEquals(50, CarriersUtils.getJspritIterations(testCarrier));
	}

	@Test
	void test_ServicesAndShipmentsHaveAttributes(){
		Object serviceCustomerAtt = testCarrier.getServices().get(Id.create("serv1",CarrierService.class)).getAttributes().getAttribute("customer");
		assertNotNull(serviceCustomerAtt);
		assertEquals("someRandomCustomer", (String) serviceCustomerAtt);
		Object shipmentCustomerAtt = testCarrier.getShipments().get(Id.create("s1",CarrierShipment.class)).getAttributes().getAttribute("customer");
		assertNotNull(shipmentCustomerAtt);
		assertEquals("someRandomCustomer", (String) shipmentCustomerAtt);
	}

	@Test
	void test_ReadWriteFilesAreEqual(){
		MatsimTestUtils.assertEqualFilesLineByLine(this.testUtils.getClassInputDirectory() + "carrierPlansEquils.xml", this.testUtils.getClassInputDirectory() + "carrierPlansEquilsWritten.xml");
	}
}
