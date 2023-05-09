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

package adapterTests;

import lsp.LSPCarrierResource;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;

import static org.junit.Assert.*;


public class DistributionResourceTest {

	//die Trackers sind ja erst ein Bestandteil des Scheduling bzw. Replanning und kommen hier noch nicht rein.
	//Man kann sie deshalb ja extra au�erhalb des Builders einsetzen.

	private org.matsim.vehicles.VehicleType distributionType;
	private CarrierVehicle distributionCarrierVehicle;
	private CarrierCapabilities capabilities;
	private Carrier distributionCarrier;
	private LSPCarrierResource distributionResource;
	private Id<Link> distributionLinkId;

	@Before
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> carrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		distributionType = vehicleTypeBuilder.build();

		distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		distributionCarrier = CarrierUtils.createCarrier(carrierId);
		distributionCarrier.setCarrierCapabilities(capabilities);


		UsecaseUtils.DistributionCarrierResourceBuilder builder = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier, network);
		builder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		builder.setLocationLinkId(distributionLinkId);
		distributionResource = builder.build();
	}


	@Test
	public void testCollectionResource() {
		assertNotNull(distributionResource.getClientElements());
		assertTrue(distributionResource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(distributionResource.getClass()));
		if (LSPCarrierResource.class.isAssignableFrom(distributionResource.getClass())) {
//				assertTrue(Carrier.class.isAssignableFrom(distributionResource.getClassOfResource()));
			assertSame(distributionResource.getCarrier(), distributionCarrier);
		}
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
				assertSame(capabilities.getFleetSize(), FleetSize.INFINITE);
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.get(0), distributionType);
					assertEquals(10, distributionType.getCapacity().getOther().intValue());
					assertEquals(0.0004, distributionType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, distributionType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(49, distributionType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), distributionType.getMaximumVelocity(), 0.0);

				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.get(0), distributionCarrierVehicle);
					assertSame(distributionCarrierVehicle.getType(), distributionType);
					assertSame(distributionCarrierVehicle.getLinkId(), distributionLinkId);
				}
			}
		}
	}


}
