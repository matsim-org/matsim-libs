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
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

class ExampleCheckRequirementsOfAssigner {

	static final String ATTRIBUTE_COLOR = "color";

	public static LSP createLSPWithProperties(Network network) {

		//Create red LogisticsSolution which has the corresponding info
		final Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		final VehicleType collectionType  = CarrierVehicleType.Builder.newInstance(Id.create("RedCarrierVehicleType", VehicleType.class))
				.setCapacity(10)
				.setCostPerDistanceUnit(0.0004)
				.setCostPerTimeUnit(0.38)
				.setFixCost(49)
				.setMaxVelocity(50 / 3.6)
				.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
		CarrierVehicle redVehicle = CarrierVehicle.newInstance(redVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities redCapabilities = CarrierCapabilities.Builder.newInstance()
				.addType(collectionType)
				.addVehicle(redVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier redCarrier = CarrierUtils.createCarrier(redCarrierId);
		redCarrier.setCarrierCapabilities(redCapabilities);

		LSPResource redResource = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(redCarrier, network)
				.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		Id<LogisticChainElement> redElementId = Id.create("RedElement", LogisticChainElement.class);
		LogisticChainElement redElement = LSPUtils.LogisticChainElementBuilder.newInstance(redElementId)
				.setResource(redResource)
				.build();

		Id<LogisticChain> redSolutionId = Id.create("RedSolution", LogisticChain.class);
		LSPUtils.LogisticChainBuilder redSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(redSolutionId);
		redSolutionBuilder.addLogisticChainElement(redElement);
		LogisticChain redSolution = redSolutionBuilder.build();

		//Add info that shows the world the color of the solution
		redSolution.getAttributes().putAttribute(ATTRIBUTE_COLOR, RedRequirement.RED);


		//Create blue LogisticsSolution which has the corresponding info
		Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
		Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities blueCapabilities  = CarrierCapabilities.Builder.newInstance()
				.addType(collectionType)
				.addVehicle(blueVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier blueCarrier = CarrierUtils.createCarrier(blueCarrierId);
		blueCarrier.setCarrierCapabilities(blueCapabilities);

		LSPResource blueResource = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(blueCarrier, network)
				.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		Id<LogisticChainElement> blueElementId = Id.create("BlueCElement", LogisticChainElement.class);
		LogisticChainElement blueElement= LSPUtils.LogisticChainElementBuilder.newInstance(blueElementId)
				.setResource(blueResource)
				.build();

		LogisticChain blueSolution = LSPUtils.LogisticChainBuilder.newInstance(Id.create("BlueSolution", LogisticChain.class))
				.addLogisticChainElement(blueElement)
				.build();

		//Add info that shows the world the color of the solution
		blueSolution.getAttributes().putAttribute(ATTRIBUTE_COLOR, BlueRequirement.BLUE);

		//Create the initial plan, add assigner that checks requirements of the shipments when assigning and add both solutions (red and blue) to the 
		//plan.
		LSPPlan plan = LSPUtils.createLSPPlan()
				.setAssigner(new RequirementsAssigner())
				.addLogisticChain(redSolution)
				.addLogisticChain(blueSolution);

		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(redResource);
		resourcesList.add(blueResource);

		return LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class))
				.setInitialPlan(plan)
				.setLogisticChainScheduler(UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList))
				.build();
	}

	public static Collection<LSPShipment> createShipmentsWithRequirements(Network network) {
		//Create ten shipments with either a red or blue requirement, i.e. that they only can be transported in a solution with the matching color
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
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

			builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
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

			shipmentList.add(builder.build());
		}

		return shipmentList;
	}

	public static void main(String[] args) {

		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		//Create LSP and shipments
		LSP lsp = createLSPWithProperties(network);
		Collection<LSPShipment> shipments = createShipmentsWithRequirements(network);

		//assign the shipments to the LSP
		for (LSPShipment shipment : shipments) {
			lsp.assignShipmentToLSP(shipment);
		}

		for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChain()) {
			if (solution.getId().toString().equals("RedSolution")) {
				for (LSPShipment shipment : solution.getShipments()) {
					if (!(shipment.getRequirements().iterator().next() instanceof RedRequirement)) {
						break;
					}
				}
				System.out.println("All shipments in " + solution.getId() + " are red");
			}
			if (solution.getId().toString().equals("BlueSolution")) {
				for (LSPShipment shipment : solution.getShipments()) {
					if (!(shipment.getRequirements().iterator().next() instanceof BlueRequirement)) {
						break;
					}
				}
				System.out.println("All shipments in " + solution.getId() + " are blue");
			}
		}

	}

}
