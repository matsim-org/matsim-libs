/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
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
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.carrier;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.matsim.testcases.MatsimTestUtils.EPSILON;

/**
 */
public class CarrierUtilsTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testAddAndGetVehicleToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<Vehicle> testVehicleId = Id.createVehicleId("testVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(testVehicleId, Id.createLinkId("link0"),VehicleUtils.getDefaultVehicleType());
//		carrierVehicle.setType(VehicleUtils.getDefaultVehicleType());

		//add Vehicle
		CarrierUtils.addCarrierVehicle(carrier, carrierVehicle);
		Assert.assertEquals(1, carrier.getCarrierCapabilities().getCarrierVehicles().size());
		CarrierVehicle cv = (CarrierVehicle) carrier.getCarrierCapabilities().getCarrierVehicles().values().toArray()[0];
		Assert.assertEquals(VehicleUtils.getDefaultVehicleType(), cv.getType());
		Assert.assertEquals(Id.createLinkId("link0"), cv.getLinkId() );

		//get Vehicle
		CarrierVehicle carrierVehicle1 = CarrierUtils.getCarrierVehicle(carrier, testVehicleId );
		Assert.assertEquals(testVehicleId, carrierVehicle1.getId());
		Assert.assertEquals(VehicleUtils.getDefaultVehicleType(), carrierVehicle1.getType());
		Assert.assertEquals(Id.createLinkId("link0"), carrierVehicle1.getLinkId() );
	}

	@Test
	public void testAddAndGetServiceToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<CarrierService> serviceId = Id.create("testVehicle", CarrierService.class);
		CarrierService service1 = CarrierService.Builder.newInstance(serviceId,Id.createLinkId("link0") )
				.setName("service1").setCapacityDemand(15).setServiceDuration(30).build();

		//add Service
		CarrierUtils.addService(carrier, service1);
		Assert.assertEquals(1, carrier.getServices().size());
		CarrierService cs1a  = (CarrierService) carrier.getServices().values().toArray()[0];
		Assert.assertEquals(service1, cs1a);
		Assert.assertEquals(Id.createLinkId("link0"), cs1a.getLocationLinkId());

		//get Service
		CarrierService cs1b  = CarrierUtils.getService(carrier, serviceId );
		Assert.assertEquals(serviceId, cs1b.getId());
		Assert.assertEquals(service1.getId(), cs1b.getId());
		Assert.assertEquals(Id.createLinkId("link0"), cs1b.getLocationLinkId());
		Assert.assertEquals(30, cs1b.getServiceDuration(), EPSILON);
	}

	@Test
	public void testAddAndGetShipmentToCarrier() {
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		Id<CarrierShipment> shipmentId = Id.create("testVehicle", CarrierShipment.class);
		CarrierShipment service1 = CarrierShipment.Builder.newInstance(shipmentId,Id.createLinkId("link0"), Id.createLinkId("link1"), 20 ).build();

		//add Shipment
		CarrierUtils.addShipment(carrier, service1);
		Assert.assertEquals(1, carrier.getShipments().size());
		CarrierShipment carrierShipment1a  = (CarrierShipment) carrier.getShipments().values().toArray()[0];
		Assert.assertEquals(service1, carrierShipment1a);
		Assert.assertEquals(Id.createLinkId("link0"), carrierShipment1a.getFrom());

		//get Shipment
		CarrierShipment carrierShipment1b  = CarrierUtils.getShipment(carrier, shipmentId );
		Assert.assertEquals(shipmentId, carrierShipment1b.getId());
		Assert.assertEquals(service1.getId(), carrierShipment1b.getId());
		Assert.assertEquals(Id.createLinkId("link0"), carrierShipment1b.getFrom());
		Assert.assertEquals(20, carrierShipment1b.getSize(), EPSILON);
	}

	@Test
	public void testGetSetJspritIteration(){
		Carrier carrier = new CarrierImpl(Id.create("carrier", Carrier.class));
		//jspirtIterations is not set. should return Integer.Min_Value (null is not possible because returning (int)
		Assert.assertEquals(Integer.MIN_VALUE, CarrierUtils.getJspritIterations(carrier) );

		CarrierUtils.setJspritIterations(carrier, 125);
		Assert.assertEquals(125, CarrierUtils.getJspritIterations(carrier) );
	}
	
}
