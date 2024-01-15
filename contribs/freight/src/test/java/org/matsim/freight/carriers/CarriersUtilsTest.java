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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.matsim.testcases.MatsimTestUtils.EPSILON;

/**
 */
public class CarriersUtilsTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testAddAndGetVehicleToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<Vehicle> testVehicleId = Id.createVehicleId("testVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(testVehicleId, Id.createLinkId("link0"),VehicleUtils.getDefaultVehicleType());
//		carrierVehicle.setType(VehicleUtils.getDefaultVehicleType());

		//add Vehicle
		CarriersUtils.addCarrierVehicle(carrier, carrierVehicle);
		Assertions.assertEquals(1, carrier.getCarrierCapabilities().getCarrierVehicles().size());
		CarrierVehicle cv = (CarrierVehicle) carrier.getCarrierCapabilities().getCarrierVehicles().values().toArray()[0];
		Assertions.assertEquals(VehicleUtils.getDefaultVehicleType(), cv.getType());
		Assertions.assertEquals(Id.createLinkId("link0"), cv.getLinkId() );

		//get Vehicle
		CarrierVehicle carrierVehicle1 = CarriersUtils.getCarrierVehicle(carrier, testVehicleId );
		Assertions.assertEquals(testVehicleId, carrierVehicle1.getId());
		Assertions.assertEquals(VehicleUtils.getDefaultVehicleType(), carrierVehicle1.getType());
		Assertions.assertEquals(Id.createLinkId("link0"), carrierVehicle1.getLinkId() );
	}

	@Test
	void testAddAndGetServiceToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<CarrierService> serviceId = Id.create("testVehicle", CarrierService.class);
		CarrierService service1 = CarrierService.Builder.newInstance(serviceId,Id.createLinkId("link0") )
				.setName("service1").setCapacityDemand(15).setServiceDuration(30).build();

		//add Service
		CarriersUtils.addService(carrier, service1);
		Assertions.assertEquals(1, carrier.getServices().size());
		CarrierService cs1a  = (CarrierService) carrier.getServices().values().toArray()[0];
		Assertions.assertEquals(service1, cs1a);
		Assertions.assertEquals(Id.createLinkId("link0"), cs1a.getLocationLinkId());

		//get Service
		CarrierService cs1b  = CarriersUtils.getService(carrier, serviceId );
		Assertions.assertEquals(serviceId, cs1b.getId());
		Assertions.assertEquals(service1.getId(), cs1b.getId());
		Assertions.assertEquals(Id.createLinkId("link0"), cs1b.getLocationLinkId());
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
		Assertions.assertEquals(Id.createLinkId("link0"), carrierShipment1a.getFrom());

		//get Shipment
		CarrierShipment carrierShipment1b  = CarriersUtils.getShipment(carrier, shipmentId );
		Assertions.assertEquals(shipmentId, carrierShipment1b.getId());
		Assertions.assertEquals(service1.getId(), carrierShipment1b.getId());
		Assertions.assertEquals(Id.createLinkId("link0"), carrierShipment1b.getFrom());
		Assertions.assertEquals(20, carrierShipment1b.getSize(), EPSILON);
	}

	@Test
	void testGetSetJspritIteration(){
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		//jspirtIterations is not set. should return Integer.Min_Value (null is not possible because returning (int)
		Assertions.assertEquals(Integer.MIN_VALUE, CarriersUtils.getJspritIterations(carrier) );

		CarriersUtils.setJspritIterations(carrier, 125);
		Assertions.assertEquals(125, CarriersUtils.getJspritIterations(carrier) );
	}

}
