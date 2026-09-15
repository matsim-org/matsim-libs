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

import static org.matsim.testcases.MatsimTestUtils.EPSILON;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Set;

/**
 */
public class CarriersUtilsTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    void testAddRoadPricingForVRPToEnsureSolutionsBasedOnNetworkModes() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		Carrier carrier = CarriersUtils.createCarrier(Id.create("carrier1", Carrier.class));
		VehicleType vehicleTypeCar = VehicleUtils.createDefaultVehicleType();
		vehicleTypeCar.setNetworkMode(TransportMode.car);
		VehicleType vehicleTypeTruck = VehicleUtils.getFactory()
			.createVehicleType(Id.create("default_truck", VehicleType.class));
		vehicleTypeTruck.setNetworkMode(TransportMode.truck);
		CarrierVehicle carrierVehicleCar = CarrierVehicle.newInstance(
			Id.create("vehicle1", Vehicle.class), Id.createLinkId("link1"), vehicleTypeCar);
		CarriersUtils.addCarrierVehicle(carrier, carrierVehicleCar);
		CarrierVehicle carrierVehicleTruck = CarrierVehicle.newInstance(
			Id.create("vehicle2", Vehicle.class), Id.createLinkId("link2"), vehicleTypeTruck);
		CarriersUtils.addCarrierVehicle(carrier, carrierVehicleTruck);
		carrier.getCarrierCapabilities().getVehicleTypes().add(vehicleTypeCar);
		carrier.getCarrierCapabilities().getVehicleTypes().add(vehicleTypeTruck);
		carriers.addCarrier(carrier);
//		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		// Mock network with two links having different allowed modes
        Network network = scenario.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("n1"), new Coord(1000.0, 0.0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("n2"), new Coord(2000.0, 0.0));

		Link link1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("link1"), node1, node2, 1000, 60, 60, 1);
		Link link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId("link2"), node2, node1, 1000, 60, 60, 1);

        link1.setAllowedModes(Set.of(TransportMode.car));
        link2.setAllowedModes(Set.of(TransportMode.car, TransportMode.truck));

        CarriersUtils.addRoadPricingForVRPToEnsureSolutionsBasedOnNetworkModes(scenario);

        // Verify road pricing scheme
        var roadPricingScheme = RoadPricingUtils.getRoadPricingScheme(scenario);

        Assertions.assertEquals("PricingForVRP", roadPricingScheme.getName());
        Assertions.assertEquals(Set.of(link1.getId()), roadPricingScheme.getTolledLinkIds()); // Only 'link1' should have toll
        Assertions.assertEquals(RoadPricingScheme.TOLL_TYPE_LINK, roadPricingScheme.getType());

		Assertions.assertEquals(0, roadPricingScheme.getLinkCostInfo(Id.createLinkId("link1"), 0, null, carrierVehicleCar.getId()).amount);
		Assertions.assertNull(roadPricingScheme.getLinkCostInfo(Id.createLinkId("link2"), 0, null, carrierVehicleCar.getId()));
		Assertions.assertEquals(1000, roadPricingScheme.getLinkCostInfo(Id.createLinkId("link1"), 0, null, carrierVehicleTruck.getId()).amount);
		Assertions.assertNull(roadPricingScheme.getLinkCostInfo(Id.createLinkId("link2"), 0, null, carrierVehicleTruck.getId()));
	}


	@Test
	void testAddAndGetVehicleToCarrier() {
		VehicleType vehicleType = VehicleUtils.createDefaultVehicleType();

		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<Vehicle> testVehicleId = Id.createVehicleId("testVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(testVehicleId, Id.createLinkId("link0"), vehicleType);
//		carrierVehicle.setType(VehicleUtils.getDefaultVehicleType());

		//add Vehicle
		CarriersUtils.addCarrierVehicle(carrier, carrierVehicle);
		Assertions.assertEquals(1, carrier.getCarrierCapabilities().getCarrierVehicles().size());
		CarrierVehicle cv = (CarrierVehicle) carrier.getCarrierCapabilities().getCarrierVehicles().values().toArray()[0];
		Assertions.assertEquals(vehicleType, cv.getType());
		Assertions.assertEquals(Id.createLinkId("link0"), cv.getLinkId());

		//get Vehicle
		CarrierVehicle carrierVehicle1 = CarriersUtils.getCarrierVehicle(carrier, testVehicleId);
		assert carrierVehicle1 != null;
		Assertions.assertEquals(testVehicleId, carrierVehicle1.getId());
		Assertions.assertEquals(vehicleType, carrierVehicle1.getType());
		Assertions.assertEquals(Id.createLinkId("link0"), carrierVehicle1.getLinkId());
	}

	@Test
	void testAddAndGetServiceToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<CarrierService> serviceId = Id.create("testVehicle", CarrierService.class);
		CarrierService service1 = CarrierService.Builder.newInstance(serviceId,Id.createLinkId("link0"), 15)
			.setServiceDuration(30)
			.build();

		//add Service
		CarriersUtils.addService(carrier, service1);
		Assertions.assertEquals(1, carrier.getServices().size());
		CarrierService cs1a  = (CarrierService) carrier.getServices().values().toArray()[0];
		Assertions.assertEquals(service1, cs1a);
		Assertions.assertEquals(Id.createLinkId("link0"), cs1a.getServiceLinkId());

		//get Service
		CarrierService cs1b  = CarriersUtils.getService(carrier, serviceId );
		assert cs1b != null;
		Assertions.assertEquals(serviceId, cs1b.getId());
		Assertions.assertEquals(service1.getId(), cs1b.getId());
		Assertions.assertEquals(Id.createLinkId("link0"), cs1b.getServiceLinkId());
		Assertions.assertEquals(30, cs1b.getServiceDuration(), EPSILON);
	}

	@Test
	void testAddAndGetShipmentToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<CarrierShipment> shipmentId = Id.create("testVehicle", CarrierShipment.class);
		CarrierShipment service1 = CarrierShipment.Builder.newInstance(shipmentId,Id.createLinkId("link0"), Id.createLinkId("link1"), 20 ).build();

		//add Shipment
		CarriersUtils.addShipment(carrier, service1);
		Assertions.assertEquals(1, carrier.getShipments().size());
		CarrierShipment carrierShipment1a  = (CarrierShipment) carrier.getShipments().values().toArray()[0];
		Assertions.assertEquals(service1, carrierShipment1a);
		Assertions.assertEquals(Id.createLinkId("link0"), carrierShipment1a.getPickupLinkId());

		//get Shipment
		CarrierShipment carrierShipment1b  = CarriersUtils.getShipment(carrier, shipmentId );
		assert carrierShipment1b != null;
		Assertions.assertEquals(shipmentId, carrierShipment1b.getId());
		Assertions.assertEquals(service1.getId(), carrierShipment1b.getId());
		Assertions.assertEquals(Id.createLinkId("link0"), carrierShipment1b.getPickupLinkId());
		Assertions.assertEquals(20, carrierShipment1b.getCapacityDemand(), EPSILON);
	}

	@Test
	void testGetSetJspritIteration(){
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		//jspritIterations is not set. should return Integer.Min_Value (null is not possible because returning (int)
		Assertions.assertEquals(Integer.MIN_VALUE, CarriersUtils.getJspritIterations(carrier) );

		CarriersUtils.setJspritIterations(carrier, 125);
		Assertions.assertEquals(125, CarriersUtils.getJspritIterations(carrier) );
	}

	@Test
	void testGetSetJspritComputationTime(){
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		//Computation time is not set. should return Integer.Min_Value (null is not possible because returning (int)
		Assertions.assertEquals(Integer.MIN_VALUE, CarriersUtils.getJspritComputationTime(carrier) );

		CarriersUtils.setJspritComputationTime(carrier, 125);
		Assertions.assertEquals(125, CarriersUtils.getJspritComputationTime(carrier) );
	}

}
