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

package org.matsim.freight.logistics.logisticChainElementTests;

import static org.junit.jupiter.api.Assertions.*;

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
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class MainRunElementTest {

	private LSPResource mainRunResource;
	private LogisticChainElement mainRunElement;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
        scenario.getNetwork();


        Id<Carrier> carrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType mainRunVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		mainRunVehType.getCapacity().setOther(30);
		mainRunVehType.getCostInformation().setCostsPerMeter(0.0002);
		mainRunVehType.getCostInformation().setCostsPerSecond(0.38);
		mainRunVehType.getCostInformation().setFixedCost(120.);
		mainRunVehType.setMaximumVelocity(50 / 3.6);


		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, fromLinkId, mainRunVehType);


		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier mainRunCarrierResourceBuilder = CarriersUtils.createCarrier(carrierId);
		mainRunCarrierResourceBuilder.setCarrierCapabilities(capabilities);

		mainRunResource = ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrierResourceBuilder)
				.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
				.setFromLinkId(Id.createLinkId("(4 2) (4 3)"))
				.setToLinkId(Id.createLinkId("(14 2) (14 3)"))
				.build();

		mainRunElement  = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("MainRunElement", LogisticChainElement.class))
				.setResource(mainRunResource)
				.build();
	}

	@Test
	public void testDistributionElement() {
		assertNotNull(mainRunElement.getIncomingShipments());
		assertNotNull(mainRunElement.getIncomingShipments().getLspShipmentsWTime());
		assertTrue(mainRunElement.getIncomingShipments().getSortedLspShipments().isEmpty());
		assertNotNull(mainRunElement.getAttributes());
		assertTrue(mainRunElement.getAttributes().isEmpty());
//		assertNull(mainRunElement.getEmbeddingContainer() );
		assertNull(mainRunElement.getNextElement());
		assertNotNull(mainRunElement.getOutgoingShipments());
		assertNotNull(mainRunElement.getOutgoingShipments().getLspShipmentsWTime());
		assertTrue(mainRunElement.getOutgoingShipments().getSortedLspShipments().isEmpty());
		assertNull(mainRunElement.getPreviousElement());
		assertSame(mainRunElement.getResource(), mainRunResource);
		assertSame(mainRunElement.getResource().getClientElements().iterator().next(), mainRunElement);
	}


}
