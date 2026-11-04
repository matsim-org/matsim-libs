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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
	private Carrier collectionCarrier;
	private LSPCarrierResource carrierResource;
	private Id<Link> collectionLinkId;
	private VehicleType vehicleType;
	private CarrierCapabilities capabilities;

	@BeforeEach
	public void initialize() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");

		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		vehicleType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		vehicleType.getCapacity().setOther(10);
		vehicleType.getCostInformation().setCostsPerMeter(0.0004);
		vehicleType.getCostInformation().setCostsPerSecond(0.38);
		vehicleType.getCostInformation().setFixedCost(49.);
		vehicleType.setMaximumVelocity(50 / 3.6);

		collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLinkId, vehicleType);

		capabilities = CarrierCapabilities.Builder.newInstance()
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		collectionCarrier = CarriersUtils.createCarrier(carrierId);
		collectionCarrier.setCarrierCapabilities(capabilities);

		carrierResource = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
				.setLocationLinkId(collectionLinkId)
				.build();
	}


	@Test
	public void testCollectionResource() {
		testCollectionResource(carrierResource, collectionCarrier, collectionLinkId, vehicleType, capabilities);
	}

	private void testCollectionResource(LSPCarrierResource resource, Carrier carrier, Id<Link> linkId, VehicleType vehicleType, CarrierCapabilities capabilities) {
		assertNotNull(resource.getClientElements());
		assertTrue(resource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(resource.getClass()));
		assertSame(resource.getCarrier(), carrier);
		assertSame(resource.getEndLinkId(), linkId);
		assertSame(resource.getStartLinkId(), linkId);
		assertNotNull(resource.getSimulationTrackers());
		assertTrue(resource.getSimulationTrackers().isEmpty());
		assertNotNull(resource.getAttributes());
		assertTrue(resource.getAttributes().isEmpty());

		if (resource.getCarrier() == carrier) {
			assertSame(carrier.getCarrierCapabilities(), capabilities);
			assertTrue(Carrier.class.isAssignableFrom(carrier.getClass()));
			assertTrue(carrier.getPlans().isEmpty());
			assertNull(carrier.getSelectedPlan());
			assertTrue(carrier.getServices().isEmpty());
			assertTrue(carrier.getShipments().isEmpty());
			if (carrier.getCarrierCapabilities() == capabilities) {
				assertSame(FleetSize.INFINITE, capabilities.getFleetSize());
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				List<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.getFirst(), vehicleType);
					assertEquals(10, vehicleType.getCapacity().getOther().intValue());
					assertEquals(0.0004, vehicleType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, vehicleType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(49, vehicleType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), vehicleType.getMaximumVelocity(), 0.0);
				}
				List<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.getFirst(), vehicleVehicle(carrier));
					assertSame(vehicles.getFirst().getType(), vehicleType);
					assertSame(vehicles.getFirst().getLinkId(), linkId);
				}
			}
		}
	}

	private static CarrierVehicle vehicleVehicle(Carrier carrier) {
		return carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
	}
}
