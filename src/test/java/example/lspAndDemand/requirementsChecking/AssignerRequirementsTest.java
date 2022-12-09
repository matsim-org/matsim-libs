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

package example.lspAndDemand.requirementsChecking;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
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
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class AssignerRequirementsTest {

	private LogisticChain blueChain;
	private LogisticChain redChain;

	@Before
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

		CarrierCapabilities.Builder redCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		redCapabilitiesBuilder.addType(collectionType);
		redCapabilitiesBuilder.addVehicle(redVehicle);
		redCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities redCapabilities = redCapabilitiesBuilder.build();
		Carrier redCarrier = CarrierUtils.createCarrier(redCarrierId);
		redCarrier.setCarrierCapabilities(redCapabilities);

		Id<LSPResource> redResourceId = Id.create("RedCarrierResource", LSPResource.class);
		UsecaseUtils.CollectionCarrierResourceBuilder redResourceBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(redResourceId, network);
		redResourceBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		redResourceBuilder.setCarrier(redCarrier);
		redResourceBuilder.setLocationLinkId(collectionLinkId);
		LSPResource redCollectionResource = redResourceBuilder.build();

		Id<LogisticChainElement> redElementId = Id.create("RedCollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder redCollectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(redElementId);
		redCollectionElementBuilder.setResource(redCollectionResource);
		LogisticChainElement redCollectionElement = redCollectionElementBuilder.build();

		Id<LogisticChain> redCollectionSolutionId = Id.create("RedCollectionSolution", LogisticChain.class);
		LSPUtils.LogisticChainBuilder redCollectionSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(redCollectionSolutionId);
		redCollectionSolutionBuilder.addLogisticChainElement(redCollectionElement);
		redChain = redCollectionSolutionBuilder.build();
//		redSolution.getAttributes().add(new RedInfo() );
		redChain.getAttributes().putAttribute("color", "red");

		ShipmentAssigner assigner = new RequirementsAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addLogisticChain(redChain);

		Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
		Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder blueCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		blueCapabilitiesBuilder.addType(collectionType);
		blueCapabilitiesBuilder.addVehicle(blueVehicle);
		blueCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities blueCapabilities = blueCapabilitiesBuilder.build();
		Carrier blueCarrier = CarrierUtils.createCarrier(blueCarrierId);
		blueCarrier.setCarrierCapabilities(blueCapabilities);

		Id<LSPResource> blueResourceId = Id.create("BlueCarrierResource", LSPResource.class);
		UsecaseUtils.CollectionCarrierResourceBuilder blueResourceBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(blueResourceId, network);
		blueResourceBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		blueResourceBuilder.setCarrier(blueCarrier);
		blueResourceBuilder.setLocationLinkId(collectionLinkId);
		LSPResource blueCollectionResource = blueResourceBuilder.build();

		Id<LogisticChainElement> blueElementId = Id.create("BlueCollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder blueCollectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(blueElementId);
		blueCollectionElementBuilder.setResource(blueCollectionResource);
		LogisticChainElement blueCollectionElement = blueCollectionElementBuilder.build();

		Id<LogisticChain> blueCollectionSolutionId = Id.create("BlueCollectionSolution", LogisticChain.class);
		LSPUtils.LogisticChainBuilder blueCollectionSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(blueCollectionSolutionId);
		blueCollectionSolutionBuilder.addLogisticChainElement(blueCollectionElement);
		blueChain = blueCollectionSolutionBuilder.build();
//		blueSolution.getAttributes().add(new BlueInfo() );
		blueChain.getAttributes().putAttribute("color", "blue");
		collectionPlan.addLogisticChain(blueChain);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(redCollectionResource);
		resourcesList.add(blueCollectionResource);

		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
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
		for (LSPShipment shipment : blueChain.getShipments()) {
			assertTrue(shipment.getRequirements().iterator().next() instanceof BlueRequirement);
		}
		for (LSPShipment shipment : redChain.getShipments()) {
			assertTrue(shipment.getRequirements().iterator().next() instanceof RedRequirement);
		}
	}

}
