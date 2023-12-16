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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CarrierPlanXmlReaderV2Test {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private Carrier testCarrier;

	@BeforeEach
	public void setUp() throws Exception{
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		String classInputDirectory = utils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(classInputDirectory + "carrierPlansEquils.xml" );
		testCarrier = carriers.getCarriers().get(Id.create("testCarrier", Carrier.class));
	}

	@Test
	void test_whenReadingServices_nuOfServicesIsCorrect(){
		Assertions.assertEquals(3,testCarrier.getServices().size());
	}

	@Test
	void test_whenReadingCarrier_itReadsTypeIdsCorrectly(){

		CarrierVehicle light = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("lightVehicle"));
		Gbl.assertNotNull(light);
		Assertions.assertEquals("light",light.getVehicleTypeId().toString());

		CarrierVehicle medium = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("mediumVehicle"));
		Gbl.assertNotNull(medium);
		Assertions.assertEquals("medium",medium.getVehicleTypeId().toString());

		CarrierVehicle heavy = CarriersUtils.getCarrierVehicle(testCarrier, Id.createVehicleId("heavyVehicle"));
		Gbl.assertNotNull(heavy);
		Assertions.assertEquals("heavy",heavy.getVehicleTypeId().toString());
	}

	@Test
	void test_whenReadingCarrier_itReadsVehiclesCorrectly(){
		Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = testCarrier.getCarrierCapabilities().getCarrierVehicles();
		Assertions.assertEquals(3,carrierVehicles.size());
		Assertions.assertTrue(exactlyTheseVehiclesAreInVehicleCollection(Arrays.asList(Id.create("lightVehicle", Vehicle.class),
				Id.create("mediumVehicle", Vehicle.class),Id.create("heavyVehicle", Vehicle.class)),carrierVehicles.values()));
	}

	@Test
	void test_whenReadingCarrier_itReadsFleetSizeCorrectly(){
		Assertions.assertEquals(FleetSize.INFINITE, testCarrier.getCarrierCapabilities().getFleetSize());
	}

	@Test
	void test_whenReadingCarrier_itReadsShipmentsCorrectly(){
		Assertions.assertEquals(2, testCarrier.getShipments().size());
	}

	@Test
	void test_whenReadingCarrier_itReadsPlansCorrectly(){
		Assertions.assertEquals(3, testCarrier.getPlans().size());
	}

	@Test
	void test_whenReadingCarrier_itSelectsPlansCorrectly(){
		Assertions.assertNotNull(testCarrier.getSelectedPlan());
	}

	@Test
	void test_whenReadingCarrierWithFiniteFleet_itSetsFleetSizeCorrectly(){

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = new Carriers();
		String classInputDirectory = utils.getClassInputDirectory();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readFile(classInputDirectory + "carrierPlansEquilsFiniteFleet.xml" );
		Assertions.assertEquals(FleetSize.FINITE, carriers.getCarriers().get(Id.create("testCarrier", Carrier.class)).getCarrierCapabilities().getFleetSize());
	}

	@Test
	void test_whenReadingPlans_nuOfToursIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		Assertions.assertEquals(1, plans.get(0).getScheduledTours().size());
		Assertions.assertEquals(1, plans.get(1).getScheduledTours().size());
		Assertions.assertEquals(1, plans.get(2).getScheduledTours().size());
	}

	@Test
	void test_whenReadingToursOfPlan1_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan1 = plans.get(0);
		ScheduledTour tour1 = plan1.getScheduledTours().iterator().next();
		Assertions.assertEquals(5,tour1.getTour().getTourElements().size());
	}

	@Test
	void test_whenReadingToursOfPlan2_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan2 = plans.get(1);
		ScheduledTour tour1 = plan2.getScheduledTours().iterator().next();
		Assertions.assertEquals(9,tour1.getTour().getTourElements().size());
	}

	@Test
	void test_whenReadingToursOfPlan3_nuOfActivitiesIsCorrect(){
		List<CarrierPlan> plans = new ArrayList<>(testCarrier.getPlans());
		CarrierPlan plan3 = plans.get(2);
		ScheduledTour tour1 = plan3.getScheduledTours().iterator().next();
		Assertions.assertEquals(9,tour1.getTour().getTourElements().size());
	}


	private boolean exactlyTheseVehiclesAreInVehicleCollection(List<Id<Vehicle>> asList, Collection<CarrierVehicle> carrierVehicles) {
		List<CarrierVehicle> vehicles = new ArrayList<>(carrierVehicles);
		for(CarrierVehicle type : carrierVehicles) if(asList.contains(type.getId() )) vehicles.remove(type );
		return vehicles.isEmpty();
	}


	@Test
	void test_CarrierHasAttributes(){
		Assertions.assertEquals((TransportMode.drt), CarriersUtils.getCarrierMode(testCarrier));
		Assertions.assertEquals(50, CarriersUtils.getJspritIterations(testCarrier));
	}

	@Test
	void test_ServicesAndShipmentsHaveAttributes(){
		Object serviceCustomerAtt = testCarrier.getServices().get(Id.create("serv1", CarrierService.class)).getAttributes().getAttribute("customer");
		Assertions.assertNotNull(serviceCustomerAtt);
		Assertions.assertEquals("someRandomCustomer", serviceCustomerAtt);
		Object shipmentCustomerAtt = testCarrier.getShipments().get(Id.create("s1",CarrierShipment.class)).getAttributes().getAttribute("customer");
		Assertions.assertNotNull(shipmentCustomerAtt);
		Assertions.assertEquals("someRandomCustomer", shipmentCustomerAtt);
	}

	@Test
	void test_readStream() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);

		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<carriers>
				  <carrier id="1">
				    <attributes>
				      <attribute name="jspritIterations" class="java.lang.Integer">50</attribute>
				    </attributes>
				    <capabilities fleetSize="INFINITE">
				      <vehicles>
				        <vehicle id="carrier_1_heavyVehicle" depotLinkId="12" typeId="heavy-20t" earliestStart="06:00:00" latestEnd="16:00:00"/>
				      </vehicles>
				    </capabilities>
				    <services>
				      <service id="1" to="31" capacityDemand="2500" earliestStart="04:00:00" latestEnd="10:00:00" serviceDuration="00:45:00"/>
				    </services>
				  </carrier>
				</carriers>
				""";

		InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( utils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );
		// yyyy should rather construct in code.  kai, jan'22

		new CarrierPlanXmlReader(carriers, carrierVehicleTypes ).readStream(is );

		Assertions.assertEquals(1, carriers.getCarriers().size());
	}

}
