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
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.DistributionCarrierResourceBuilder;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class DistributionResourceTest {

	//Die Tracker sind ja erst ein Bestandteil des Scheduling bzw. Replanning und kommen hier noch nicht rein.
	//Man kann sie deshalb ja extra außerhalb des Builders einsetzen.

	private org.matsim.vehicles.VehicleType distributionVehType;
	private CarrierVehicle distributionCarrierVehicle;
	private CarrierCapabilities capabilities;
	private Carrier distributionCarrier;
	private LSPCarrierResource distributionResource;
	private Id<Link> distributionLinkId;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
        scenario.getNetwork();

        Id<Carrier> carrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		distributionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		distributionVehType.getCapacity().setOther(10);
		distributionVehType.getCostInformation().setCostsPerMeter(0.0004);
		distributionVehType.getCostInformation().setCostsPerSecond(0.38);
		distributionVehType.getCostInformation().setFixedCost(49.);
		distributionVehType.setMaximumVelocity(50 / 3.6);

		distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, this.distributionVehType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		distributionCarrier = CarriersUtils.createCarrier(carrierId);
		distributionCarrier.setCarrierCapabilities(capabilities);


		DistributionCarrierResourceBuilder builder = ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier);
		builder.setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario));
		builder.setLocationLinkId(distributionLinkId);
		distributionResource = builder.build();
	}


	@Test
	public void testCollectionResource() {
		assertNotNull(distributionResource.getClientElements());
		assertTrue(distributionResource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(distributionResource.getClass()));
		assertSame(distributionResource.getCarrier(), distributionCarrier);
		assertSame(distributionResource.getEndLinkId(), distributionLinkId);
		assertSame(distributionResource.getStartLinkId(), distributionLinkId);
		assertNotNull(distributionResource.getSimulationTrackers());
		assertTrue(distributionResource.getSimulationTrackers().isEmpty());
		assertNotNull(distributionResource.getAttributes());
		assertTrue(distributionResource.getAttributes().isEmpty());
		assertSame(distributionResource.getStartLinkId(), distributionLinkId);
		if (distributionResource.getCarrier() == distributionCarrier) {
			assertSame(distributionCarrier.getCarrierCapabilities(), capabilities);
			assertTrue(Carrier.class.isAssignableFrom(distributionCarrier.getClass()));
			assertTrue(distributionCarrier.getPlans().isEmpty());
			assertNull(distributionCarrier.getSelectedPlan());
			assertTrue(distributionCarrier.getServices().isEmpty());
			assertTrue(distributionCarrier.getShipments().isEmpty());
			if (distributionCarrier.getCarrierCapabilities() == capabilities) {
				assertSame(FleetSize.INFINITE, capabilities.getFleetSize());
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.getFirst(), distributionVehType);
					assertEquals(10, distributionVehType.getCapacity().getOther().intValue());
					assertEquals(0.0004, distributionVehType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, distributionVehType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(49, distributionVehType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), distributionVehType.getMaximumVelocity(), 0.0);

				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.getFirst(), distributionCarrierVehicle);
					assertSame(distributionCarrierVehicle.getType(), distributionVehType);
					assertSame(distributionCarrierVehicle.getLinkId(), distributionLinkId);
				}
			}
		}
	}


}
