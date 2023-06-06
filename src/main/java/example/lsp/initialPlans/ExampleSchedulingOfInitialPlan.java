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

package example.lsp.initialPlans;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
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

/*package-private*/ class ExampleSchedulingOfInitialPlan {

	private static LSP createInitialLSP(Network network) {

		//The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();

		Carrier carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		//The Resource i.e. the Resource is created
		LSPResource collectionResource = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(carrier, network)
				.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		//The adapter is now inserted into the only LogisticsSolutionElement of the only LogisticsSolution of the LSP
		LogisticChainElement collectionElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();

		//The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
		LogisticChain collectionSolution  = LSPUtils.LogisticChainBuilder.newInstance(Id.create("CollectionSolution", LogisticChain.class))
				.addLogisticChainElement(collectionElement)
				.build();

		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addLogisticChain(collectionSolution);

		//The exogenous list of Resoruces for the SolutuionScheduler is compiled and the Scheduler is added to the LSPBuilder
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);

		return LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class))
				.setInitialPlan(collectionPlan)
				.setLogisticChainScheduler(simpleScheduler)
				.build();
	}

	private static Collection<LSPShipment> createInitialLSPShipments(Network network) {
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

		//Create five LSPShipments that are located in the left half of the network.
		for (int i = 1; i < 6; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
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
		LSP lsp = createInitialLSP(network);
		Collection<LSPShipment> shipments = createInitialLSPShipments(network);

		//assign the shipments to the LSP
		for (LSPShipment shipment : shipments) {
			lsp.assignShipmentToLSP(shipment);
		}

		//schedule the LSP with the shipments and according to the scheduler of the Resource
		lsp.scheduleLogisticChains();

		//print the schedules for the assigned LSPShipments
		for (LSPShipment shipment : shipments) {
			System.out.println("Shipment: " + shipment.getId());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(ShipmentUtils.findPlanOfShipment(lsp.getSelectedPlan(), shipment.getId())
					.getPlanElements().values());
			scheduleElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<>(shipment.getShipmentLog().getPlanElements().values());
			logElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

			for (ShipmentPlanElement element : ShipmentUtils.findPlanOfShipment(lsp.getSelectedPlan(), shipment.getId())
					.getPlanElements().values()) {
				System.out.println(
//					"Solution Id: " + element.getSolutionElement().getEmbeddingContainer().getId() +
						" SolutionElement Id: " + element.getLogisticChainElement().getId()
								+ " Resource Id: " + element.getResourceId()
								+ " Type: " + element.getElementType()
								+ " Start Time: " + element.getStartTime()
								+ " End Time: " + element.getEndTime());
			}
			System.out.println();
		}
	}
}
