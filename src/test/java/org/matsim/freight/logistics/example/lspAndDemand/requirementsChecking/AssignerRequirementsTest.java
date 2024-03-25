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

package org.matsim.freight.logistics.example.lspAndDemand.requirementsChecking;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class AssignerRequirementsTest {

	private LogisticChain blueChain;
	private LogisticChain redChain;

	@BeforeEach
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
		CarrierVehicle redVehicle = CarrierVehicle.newInstance(redVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities redCapabilities  = CarrierCapabilities.Builder.newInstance()
				.addType(collectionType)
				.addVehicle(redVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier redCarrier = CarriersUtils.createCarrier(redCarrierId);
		redCarrier.setCarrierCapabilities(redCapabilities);

		LSPResource redCollectionResource = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(redCarrier, network)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		Id<LogisticChainElement> redElementId = Id.create("RedCollectionElement", LogisticChainElement.class);
		LogisticChainElement redCollectionElement = LSPUtils.LogisticChainElementBuilder.newInstance(redElementId)
				.setResource(redCollectionResource)
				.build();

		redChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("RedCollectionSolution", LogisticChain.class))
				.addLogisticChainElement(redCollectionElement)
				.build();
		redChain.getAttributes().putAttribute("color", "red");

		InitialShipmentAssigner assigner = new RequirementsAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setInitialShipmentAssigner(assigner);
		collectionPlan.addLogisticChain(redChain);

		Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
		Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities blueCapabilities = CarrierCapabilities.Builder.newInstance()
				.addType(collectionType)
				.addVehicle(blueVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier blueCarrier = CarriersUtils.createCarrier(blueCarrierId);
		blueCarrier.setCarrierCapabilities(blueCapabilities);

		LSPResource blueCollectionResource  = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(blueCarrier, network)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		Id<LogisticChainElement> blueElementId = Id.create("BlueCollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder blueCollectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(blueElementId);
		blueCollectionElementBuilder.setResource(blueCollectionResource);
		LogisticChainElement blueCollectionElement = blueCollectionElementBuilder.build();

		blueChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("BlueCollectionSolution", LogisticChain.class))
				.addLogisticChainElement(blueCollectionElement)
				.build();
		blueChain.getAttributes().putAttribute("color", "blue");
		collectionPlan.addLogisticChain(blueChain);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(redCollectionResource);
		resourcesList.add(blueCollectionResource);

		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		LSP collectionLSP = collectionLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

		Random rand = new Random(1);

		for (int i = 1; i < 11; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			int capacityDemand = rand.nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList);
				Link pendingFromLink = linkList.get(0);
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(collectionLinkId);
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			boolean blue = rand.nextBoolean();
			if (blue) {
				builder.addRequirement(new BlueRequirement());
			} else {
				builder.addRequirement(new RedRequirement());
			}

			LSPShipment shipment = builder.build();
			collectionLSP.assignShipmentToLSP(shipment);
		}
	}

	@Test
	public void testAssignerRequirements() {
		for (Id<LSPShipment> shipmentId : blueChain.getShipmentIds()) {
			LSPShipment shipment = LSPUtils.findLspShipment(blueChain.getLSP(), shipmentId);
			assertTrue(shipment.getRequirements().iterator().next() instanceof BlueRequirement);
		}
		for (Id<LSPShipment> shipmentId : redChain.getShipmentIds()) {
			LSPShipment shipment = LSPUtils.findLspShipment(redChain.getLSP(), shipmentId);
			assertTrue(shipment.getRequirements().iterator().next() instanceof RedRequirement);
		}
	}

}
