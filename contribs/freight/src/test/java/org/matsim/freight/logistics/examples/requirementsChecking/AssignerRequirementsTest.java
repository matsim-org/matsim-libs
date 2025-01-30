/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.requirementsChecking;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class AssignerRequirementsTest {

	private LogisticChain blueChain;
	private LogisticChain redChain;

	@BeforeEach
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50 / 3.6);
		collectionVehType.setNetworkMode(TransportMode.car);

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
		CarrierVehicle redVehicle = CarrierVehicle.newInstance(redVehicleId, collectionLinkId, collectionVehType);

		CarrierCapabilities redCapabilities  = CarrierCapabilities.Builder.newInstance()
				.addVehicle(redVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier redCarrier = CarriersUtils.createCarrier(redCarrierId);
		redCarrier.setCarrierCapabilities(redCapabilities);

		LSPResource redCollectionResource = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(redCarrier)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
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
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId, collectionVehType);

		CarrierCapabilities blueCapabilities = CarrierCapabilities.Builder.newInstance()
				.addVehicle(blueVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier blueCarrier = CarriersUtils.createCarrier(blueCarrierId);
		blueCarrier.setCarrierCapabilities(blueCapabilities);

		LSPResource blueCollectionResource  = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(blueCarrier)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
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
			Id<LspShipment> id = Id.create(i, LspShipment.class);
			LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
			int capacityDemand = rand.nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList);
				Link pendingFromLink = linkList.getFirst();
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

			LspShipment shipment = builder.build();
			collectionLSP.assignShipmentToLSP(shipment);
		}
	}

	@Test
	public void testAssignerRequirements() {
		for (Id<LspShipment> shipmentId : blueChain.getLspShipmentIds()) {
			LspShipment shipment = LSPUtils.findLspShipment(blueChain.getLSP(), shipmentId);
			assert shipment != null;
			assertInstanceOf(BlueRequirement.class, shipment.getRequirements().iterator().next());
		}
		for (Id<LspShipment> shipmentId : redChain.getLspShipmentIds()) {
			LspShipment shipment = LSPUtils.findLspShipment(redChain.getLSP(), shipmentId);
            assert shipment != null;
            assertInstanceOf(RedRequirement.class, shipment.getRequirements().iterator().next());
		}
	}

}
