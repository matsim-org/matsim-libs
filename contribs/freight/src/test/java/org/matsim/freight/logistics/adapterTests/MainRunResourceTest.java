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

public class MainRunResourceTest {

	private org.matsim.vehicles.VehicleType mainRunType;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private CarrierVehicle carrierVehicle;
	private CarrierCapabilities capabilities;
	private Carrier carrier;
	private LSPCarrierResource mainRunResource;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");

        Id<Carrier> carrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		mainRunType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		mainRunType.getCapacity().setOther(30);
		mainRunType.getCostInformation().setCostsPerMeter(0.0008);
		mainRunType.getCostInformation().setCostsPerSecond(0.38);
		mainRunType.getCostInformation().setFixedCost(120.);
		mainRunType.setMaximumVelocity(50 / 3.6);

		toLinkId = Id.createLinkId("(14 2) (14 3)");
		fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("MainRunVehicle");
		carrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, fromLinkId, this.mainRunType);


		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		carrier = CarriersUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		mainRunResource = ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(carrier)
				.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
				.setFromLinkId(Id.createLinkId("(4 2) (4 3)")).setToLinkId(Id.createLinkId("(14 2) (14 3)"))
				.build();

	}

	@Test
	public void testMainRunResource() {
		assertNotNull(mainRunResource.getClientElements());
		assertTrue(mainRunResource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(mainRunResource.getClass()));
		assertSame(mainRunResource.getCarrier(), carrier);
		assertSame(mainRunResource.getEndLinkId(), toLinkId);
		assertSame(mainRunResource.getStartLinkId(), fromLinkId);
		assertNotNull(mainRunResource.getSimulationTrackers());
		assertTrue(mainRunResource.getSimulationTrackers().isEmpty());
		assertNotNull(mainRunResource.getAttributes());
		assertTrue(mainRunResource.getAttributes().isEmpty());
		if (mainRunResource.getCarrier() == carrier) {
			assertSame(carrier.getCarrierCapabilities(), capabilities);
			assertTrue(Carrier.class.isAssignableFrom(carrier.getClass()));
			assertTrue(carrier.getPlans().isEmpty());
			assertNull(carrier.getSelectedPlan());
			assertTrue(carrier.getServices().isEmpty());
			assertTrue(carrier.getShipments().isEmpty());
			if (carrier.getCarrierCapabilities() == capabilities) {
				assertSame(FleetSize.INFINITE, capabilities.getFleetSize());
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.getFirst(), mainRunType);
					assertEquals(30, mainRunType.getCapacity().getOther().intValue());
					assertEquals(0.0008, mainRunType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, mainRunType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(120, mainRunType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), mainRunType.getMaximumVelocity(), 0.0);
				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.getFirst(), carrierVehicle);
					assertSame(carrierVehicle.getType(), mainRunType);
					assertSame(carrierVehicle.getLinkId(), fromLinkId);
				}
			}
		}


	}
}
