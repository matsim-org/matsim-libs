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
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.MainRunCarrierResourceBuilder;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class MainRunResourceTest {

	private Carrier carrier;
	private LSPCarrierResource mainRunResource;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private VehicleType vehicleType;
	private CarrierCapabilities capabilities;

	@BeforeEach
	public void initialize() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");

		Id<Carrier> carrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		vehicleType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		vehicleType.getCapacity().setOther(30);
		vehicleType.getCostInformation().setCostsPerMeter(0.0008);
		vehicleType.getCostInformation().setCostsPerSecond(0.38);
		vehicleType.getCostInformation().setFixedCost(120.);
		vehicleType.setMaximumVelocity(50 / 3.6);

		toLinkId = Id.createLinkId("(14 2) (14 3)");
		fromLinkId = Id.createLinkId("(4 2) (4 3)");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("MainRunVehicle"), fromLinkId, vehicleType);

		capabilities = CarrierCapabilities.Builder.newInstance()
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		carrier = CarriersUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		mainRunResource = MainRunCarrierResourceBuilder.newInstance(carrier)
				.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
				.setFromLinkId(fromLinkId)
				.setToLinkId(toLinkId)
				.build();
	}

	@Test
	public void testMainRunResource() {
		testMainRunResource(mainRunResource, carrier, fromLinkId, toLinkId, vehicleType, capabilities);
	}

	private void testMainRunResource(LSPCarrierResource resource, Carrier carrier, Id<Link> fromLinkId, Id<Link> toLinkId, VehicleType vehicleType, CarrierCapabilities capabilities) {
		assertNotNull(resource.getClientElements());
		assertTrue(resource.getClientElements().isEmpty());
		assertTrue(resource.getClass().isAssignableFrom(LSPCarrierResource.class));
		assertSame(resource.getCarrier(), carrier);
		assertSame(resource.getEndLinkId(), toLinkId);
		assertSame(resource.getStartLinkId(), fromLinkId);
		assertNotNull(resource.getSimulationTrackers());
		assertTrue(resource.getSimulationTrackers().isEmpty());
		assertNotNull(resource.getAttributes());
		assertTrue(resource.getAttributes().isEmpty());

		if (resource.getCarrier() == carrier) {
			assertSame(carrier.getCarrierCapabilities(), capabilities);
			assertTrue(carrier.getClass().isAssignableFrom(Carrier.class));
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
					assertEquals(30, vehicleType.getCapacity().getOther().intValue());
					assertEquals(0.0008, vehicleType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, vehicleType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(120, vehicleType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), vehicleType.getMaximumVelocity(), 0.0);
				}
				List<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.getFirst(), vehicleVehicle(carrier));
					assertSame(vehicles.getFirst().getType(), vehicleType);
					assertSame(vehicles.getFirst().getLinkId(), fromLinkId);
				}
			}
		}
	}

	private static CarrierVehicle vehicleVehicle(Carrier carrier) {
		return carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
	}
}
