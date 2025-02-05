/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 */

package org.matsim.freight.logistics.adapterTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CollectionResourceTest {

	//Die Tracker sind ja erst ein Bestandteil des Scheduling bzw. Replanning und kommen hier noch nicht rein.
	//Man kann sie deshalb ja extra außerhalb des Builders einsetzen.
	private VehicleType collectionVehType;
	private CarrierVehicle collectionCarrierVehicle;
	private Carrier collectionCarrier;
	private LSPCarrierResource carrierResource;
	private Id<Link> collectionLinkId;
	private CarrierCapabilities capabilities;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
        scenario.getNetwork();

        Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
				Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50 / 3.6);

		collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		collectionCarrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionVehType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		collectionCarrier = CarriersUtils.createCarrier(carrierId);
		collectionCarrier.setCarrierCapabilities(capabilities);


		carrierResource = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
				.setLocationLinkId(collectionLinkId)
				.build();
	}


	@Test
	public void testCollectionResource() {
		assertNotNull(carrierResource.getClientElements());
		assertTrue(carrierResource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(carrierResource.getClass()));
		assertSame(carrierResource.getCarrier(), collectionCarrier);
		assertSame(carrierResource.getEndLinkId(), collectionLinkId);
		assertSame(carrierResource.getStartLinkId(), collectionLinkId);
		assertNotNull(carrierResource.getSimulationTrackers());
		assertTrue(carrierResource.getSimulationTrackers().isEmpty());
		assertNotNull(carrierResource.getAttributes());
		assertTrue(carrierResource.getAttributes().isEmpty());
		assertSame(carrierResource.getStartLinkId(), collectionLinkId);
		if (carrierResource.getCarrier() == collectionCarrier) {
			assertSame(collectionCarrier.getCarrierCapabilities(), capabilities);
			assertTrue(Carrier.class.isAssignableFrom(collectionCarrier.getClass()));
			assertTrue(collectionCarrier.getPlans().isEmpty());
			assertNull(collectionCarrier.getSelectedPlan());
			assertTrue(collectionCarrier.getServices().isEmpty());
			assertTrue(collectionCarrier.getShipments().isEmpty());
			if (collectionCarrier.getCarrierCapabilities() == capabilities) {
				assertSame(FleetSize.INFINITE, capabilities.getFleetSize());
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.getFirst(), collectionVehType);
					assertEquals(10, collectionVehType.getCapacity().getOther().intValue());
					assertEquals(0.0004, collectionVehType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, collectionVehType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(49, collectionVehType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), collectionVehType.getMaximumVelocity(), 0.0);

				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.getFirst(), collectionCarrierVehicle);
					assertSame(collectionCarrierVehicle.getType(), collectionVehType);
					assertSame(collectionCarrierVehicle.getLinkId(), collectionLinkId);
				}
			}
		}
	}
}
