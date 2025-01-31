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

package org.matsim.freight.logistics.resourceImplementations;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TranshipmentHubSchedulerBuilder;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TransshipmentHubBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class MultipleShipmentsCompleteLSPSchedulingTest {

	private LSP lsp;
	private LSPResource collectionResource;
	private LogisticChainElement collectionElement;
	private LSPResource firstTranshipmentHubResource;
	private LogisticChainElement firstHubElement;
	private LSPResource mainRunResource;
	private LogisticChainElement mainRunElement;
	private LSPResource secondTranshipmentHubResource;
	private LogisticChainElement secondHubElement;
	private LSPResource distributionResource;
	private LogisticChainElement distributionElement;
	private Id<Link> toLinkId;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType collectionVehType = VehicleUtils.createVehicleType(collectionVehTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50/3.6);

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId, collectionVehType);

		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarriersUtils.createCarrier(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);


		collectionResource = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario))
				.setLocationLinkId(collectionLinkId)
				.build();

		collectionElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();

		TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		TransshipmentHubBuilder firstTransshipmentHubBuilder = ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());
		firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTranshipmentHubResource);
		firstHubElement = firstHubElementBuilder.build();

		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType mainRunVehType = VehicleUtils.createVehicleType(mainRunVehTypeId, TransportMode.car);
		mainRunVehType.getCapacity().setOther(30);
		mainRunVehType.getCostInformation().setCostsPerMeter(0.0002);
		mainRunVehType.getCostInformation().setCostsPerSecond(0.38);
		mainRunVehType.getCostInformation().setFixedCost(120.);
		mainRunVehType.setMaximumVelocity(50/3.6);


		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		toLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunVehType);


		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarriersUtils.createCarrier(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		mainRunResource = ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrier)
				.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler(scenario))
				.setFromLinkId(fromLinkId)
				.setToLinkId(toLinkId)
				.build();

		mainRunElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("MainRunElement", LogisticChainElement.class))
				.setResource(mainRunResource)
				.build();

		TranshipmentHubSchedulerBuilder secondSchedulerBuilder = ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance();
		secondSchedulerBuilder.setCapacityNeedFixed(10);
		secondSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		TransshipmentHubBuilder secondTransshipmentHubBuilder = ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario);
		secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());
		secondTranshipmentHubResource = secondTransshipmentHubBuilder.build();

		Id<LogisticChainElement> secondHubElementId = Id.create("SecondHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder secondHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTranshipmentHubResource);
		secondHubElement = secondHubElementBuilder.build();

		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType distributionVehType = VehicleUtils.createVehicleType(distributionVehTypeId, TransportMode.car);
		distributionVehType.getCapacity().setOther(10);
		distributionVehType.getCostInformation().setCostsPerMeter(0.0004);
		distributionVehType.getCostInformation().setCostsPerSecond(0.38);
		distributionVehType.getCostInformation().setFixedCost(49.);
		distributionVehType.setMaximumVelocity(50/3.6);

		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionVehType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier distributionCarrier = CarriersUtils.createCarrier(distributionCarrierId);
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);


		distributionResource  = ResourceImplementationUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier)
				.setDistributionScheduler(ResourceImplementationUtils.createDefaultDistributionCarrierScheduler(scenario))
				.setLocationLinkId(distributionLinkId)
				.build();

		Id<LogisticChainElement> distributionElementId = Id.create("DistributionElement", LogisticChainElement.class);
		distributionElement = LSPUtils.LogisticChainElementBuilder.newInstance(distributionElementId)
				.setResource(distributionResource)
				.build();

		collectionElement.connectWithNextElement(firstHubElement);
		firstHubElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondHubElement);
		secondHubElement.connectWithNextElement(distributionElement);

		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LogisticChain completeSolution = LSPUtils.LogisticChainBuilder.newInstance(solutionId)
				.addLogisticChainElement(collectionElement)
				.addLogisticChainElement(firstHubElement)
				.addLogisticChainElement(mainRunElement)
				.addLogisticChainElement(secondHubElement)
				.addLogisticChainElement(distributionElement)
				.build();

		InitialShipmentAssigner assigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setInitialShipmentAssigner(assigner);
		completePlan.addLogisticChain(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		resourcesList.add(firstTranshipmentHubResource);
		resourcesList.add(mainRunResource);
		resourcesList.add(secondTranshipmentHubResource);
		resourcesList.add(distributionResource);


		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());
		Random rand = new Random(1);
		for (int i = 1; i < 100; i++) {
			Id<LspShipment> id = Id.create(i, LspShipment.class);
			LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
			int capacityDemand = rand.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, rand);
				Link pendingToLink = linkList.getFirst();
				if ((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
						pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingToLink.getFromNode().getCoord().getX() >= 14000 &&
						pendingToLink.getToNode().getCoord().getX() <= 18000 &&
						pendingToLink.getToNode().getCoord().getY() <= 4000 &&
						pendingToLink.getToNode().getCoord().getX() >= 14000)) {
					builder.setToLinkId(pendingToLink.getId());
					break;
				}

			}

			while (true) {
				Collections.shuffle(linkList, rand);
				Link pendingFromLink = linkList.getFirst();
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}

			}

			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			LspShipment shipment = builder.build();
			lsp.assignShipmentToLSP(shipment);
		}
		lsp.scheduleLogisticChains();

	}

	@Test
	public void testCompletedLSPScheduling() {

		for (LspShipment shipment : lsp.getLspShipments()) {
			ArrayList<LspShipmentPlanElement> elementList = new ArrayList<>(LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			elementList.sort(LspShipmentUtils.createShipmentPlanElementComparator());
			System.out.println();
			for (LspShipmentPlanElement element : elementList) {
				System.out.println(element.getLogisticChainElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());
			}
			System.out.println();
		}

		ArrayList<LogisticChainElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getLogisticChains().iterator().next().getLogisticChainElements());
		ArrayList<LSPResource> resources = new ArrayList<>(lsp.getResources());

		for (LspShipment shipment : lsp.getLspShipments()) {
			assertEquals(11, LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().size());
			ArrayList<LspShipmentPlanElement> planElements = new ArrayList<>(LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			planElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());

			assertEquals("UNLOAD", planElements.get(10).getElementType());
			assertTrue(planElements.get(10).getEndTime() >= (0));
			assertTrue(planElements.get(10).getEndTime() <= (24*3600));
			assertTrue(planElements.get(10).getStartTime() <= planElements.get(10).getEndTime());
			assertTrue(planElements.get(10).getStartTime() >= (0));
			assertTrue(planElements.get(10).getStartTime() <= (24*3600));
			assertSame(planElements.get(10).getResourceId(), distributionResource.getId());
			assertSame(planElements.get(10).getLogisticChainElement(), distributionElement);

			assertEquals(planElements.get(10).getStartTime(), planElements.get(9).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(9).getElementType());
			assertTrue(planElements.get(9).getEndTime() >= (0));
			assertTrue(planElements.get(9).getEndTime() <= (24*3600));
			assertTrue(planElements.get(9).getStartTime() <= planElements.get(9).getEndTime());
			assertTrue(planElements.get(9).getStartTime() >= (0));
			assertTrue(planElements.get(9).getStartTime() <= (24*3600));
			assertSame(planElements.get(9).getResourceId(), distributionResource.getId());
			assertSame(planElements.get(9).getLogisticChainElement(), distributionElement);

			assertEquals(planElements.get(9).getStartTime(), planElements.get(8).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(8).getElementType());
			assertTrue(planElements.get(8).getEndTime() >= (0));
			assertTrue(planElements.get(8).getEndTime() <= (24*3600));
			assertTrue(planElements.get(8).getStartTime() <= planElements.get(8).getEndTime());
			assertTrue(planElements.get(8).getStartTime() >= (0));
			assertTrue(planElements.get(8).getStartTime() <= (24*3600));
			assertSame(planElements.get(8).getResourceId(), distributionResource.getId());
			assertSame(planElements.get(8).getLogisticChainElement(), distributionElement);

			assertTrue(planElements.get(8).getStartTime() >= planElements.get(7).getEndTime() / 1.0001 + 300);

			assertEquals("HANDLING", planElements.get(7).getElementType());
			assertTrue(planElements.get(7).getEndTime() >= (0));
			assertTrue(planElements.get(7).getEndTime() <= (24*3600));
			assertTrue(planElements.get(7).getStartTime() <= planElements.get(7).getEndTime());
			assertTrue(planElements.get(7).getStartTime() >= (0));
			assertTrue(planElements.get(7).getStartTime() <= (24*3600));
			assertSame(planElements.get(7).getResourceId(), secondTranshipmentHubResource.getId());
			assertSame(planElements.get(7).getLogisticChainElement(), secondHubElement);

			assertEquals(planElements.get(7).getStartTime(), planElements.get(6).getEndTime() + 300, 0.0);

			assertEquals("UNLOAD", planElements.get(6).getElementType());
			assertTrue(planElements.get(6).getEndTime() >= (0));
			assertTrue(planElements.get(6).getEndTime() <= (24*3600));
			assertTrue(planElements.get(6).getStartTime() <= planElements.get(6).getEndTime());
			assertTrue(planElements.get(6).getStartTime() >= (0));
			assertTrue(planElements.get(6).getStartTime() <= (24*3600));
			assertSame(planElements.get(6).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(6).getLogisticChainElement(), mainRunElement);

			assertEquals(planElements.get(6).getStartTime(), planElements.get(5).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(5).getElementType());
			assertTrue(planElements.get(5).getEndTime() >= (0));
			assertTrue(planElements.get(5).getEndTime() <= (24*3600));
			assertTrue(planElements.get(5).getStartTime() <= planElements.get(5).getEndTime());
			assertTrue(planElements.get(5).getStartTime() >= (0));
			assertTrue(planElements.get(5).getStartTime() <= (24*3600));
			assertSame(planElements.get(5).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(5).getLogisticChainElement(), mainRunElement);

			assertEquals(planElements.get(5).getStartTime(), planElements.get(4).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(4).getElementType());
			assertTrue(planElements.get(4).getEndTime() >= (0));
			assertTrue(planElements.get(4).getEndTime() <= (24*3600));
			assertTrue(planElements.get(4).getStartTime() <= planElements.get(4).getEndTime());
			assertTrue(planElements.get(4).getStartTime() >= (0));
			assertTrue(planElements.get(4).getStartTime() <= (24*3600));
			assertSame(planElements.get(4).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(4).getLogisticChainElement(), mainRunElement);

			assertTrue(planElements.get(4).getStartTime() >= planElements.get(3).getEndTime() / (1.0001) + 300);

			assertEquals("HANDLING", planElements.get(3).getElementType());
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));
			assertSame(planElements.get(3).getResourceId(), firstTranshipmentHubResource.getId());
			assertSame(planElements.get(3).getLogisticChainElement(), firstHubElement);

			assertEquals(planElements.get(3).getStartTime(), planElements.get(2).getEndTime() + 300, 0.0);

			assertEquals("UNLOAD", planElements.get(2).getElementType());
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));
			assertSame(planElements.get(2).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(2).getLogisticChainElement(), collectionElement);

			assertEquals(planElements.get(2).getStartTime(), planElements.get(1).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(1).getElementType());
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));
			assertSame(planElements.get(1).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(1).getLogisticChainElement(), collectionElement);

			assertEquals(planElements.get(1).getStartTime(), planElements.get(0).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(0).getElementType());
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.getFirst().getEndTime() <= (24*3600));
			assertTrue(planElements.getFirst().getStartTime() <= planElements.getFirst().getEndTime());
			assertTrue(planElements.getFirst().getStartTime() >= (0));
			assertTrue(planElements.getFirst().getStartTime() <= (24*3600));
			assertSame(planElements.getFirst().getResourceId(), collectionResource.getId());
			assertSame(planElements.getFirst().getLogisticChainElement(), collectionElement);
		}

		assertEquals(1, firstTranshipmentHubResource.getSimulationTrackers().size());
		ArrayList<EventHandler> eventHandlers = new ArrayList<>(firstTranshipmentHubResource.getSimulationTrackers());
		assertInstanceOf(TransshipmentHubTourEndEventHandler.class, eventHandlers.getFirst());
		TransshipmentHubTourEndEventHandler reloadEventHandler = (TransshipmentHubTourEndEventHandler) eventHandlers.getFirst();
		Iterator<Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair>> iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();

		while (iter.hasNext()) {
			Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair> entry = iter.next();
			CarrierService service = entry.getKey();
			LspShipment shipment = entry.getValue().lspShipment;
			LogisticChainElement element = entry.getValue().logisticChainElement;
			assertSame(service.getServiceLinkId(), shipment.getFrom());
            assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticChainElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
					break;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getLspShipmentsWTime().contains(shipment));
			assertFalse(element.getIncomingShipments().getLspShipmentsWTime().contains(shipment));
		}

		assertEquals(1, secondTranshipmentHubResource.getSimulationTrackers().size());
		eventHandlers = new ArrayList<>(secondTranshipmentHubResource.getSimulationTrackers());
		assertInstanceOf(TransshipmentHubTourEndEventHandler.class, eventHandlers.getFirst());
		reloadEventHandler = (TransshipmentHubTourEndEventHandler) eventHandlers.getFirst();
		iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();

		while (iter.hasNext()) {
			Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair> entry = iter.next();
			CarrierService service = entry.getKey();
			LspShipment shipment = entry.getValue().lspShipment;
			LogisticChainElement element = entry.getValue().logisticChainElement;
			assertSame(service.getServiceLinkId(), toLinkId);
            assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticChainElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
					break;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getLspShipmentsWTime().contains(shipment));
			assertFalse(element.getIncomingShipments().getLspShipmentsWTime().contains(shipment));
		}

		for (LspShipment shipment : lsp.getLspShipments()) {
			assertEquals(6, shipment.getSimulationTrackers().size());
			eventHandlers = new ArrayList<>(shipment.getSimulationTrackers());
			ArrayList<LspShipmentPlanElement> planElements = new ArrayList<>(LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			planElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());

			assertInstanceOf(LSPTourEndEventHandler.class, eventHandlers.getFirst());
			LSPTourEndEventHandler endHandler = (LSPTourEndEventHandler) eventHandlers.getFirst();
			assertSame(endHandler.getCarrierService().getServiceLinkId(), shipment.getFrom());
            assertEquals(endHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(endHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(endHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), shipment.getPickupTimeWindow().getStart(), 0.0);
			assertEquals(endHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), shipment.getPickupTimeWindow().getEnd(), 0.0);
			assertSame(endHandler.getLogisticChainElement(), planElements.get(0).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), planElements.get(1).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), planElements.get(2).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), solutionElements.getFirst());
			assertSame(endHandler.getLspShipment(), shipment);
			assertSame(endHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(endHandler.getResourceId(), resources.getFirst().getId());

			//CollectionServiceEnd
			assertInstanceOf(CollectionServiceEndEventHandler.class, eventHandlers.get(1));
			CollectionServiceEndEventHandler serviceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertSame(serviceHandler.getCarrierService().getServiceLinkId(), shipment.getFrom());
            assertEquals(serviceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(serviceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(serviceHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), shipment.getPickupTimeWindow().getStart(), 0.0);
			assertEquals(serviceHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), shipment.getPickupTimeWindow().getEnd(), 0.0);
			assertSame(serviceHandler.getElement(), planElements.get(0).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), planElements.get(1).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), planElements.get(2).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), solutionElements.getFirst());
			assertSame(serviceHandler.getLspShipment(), shipment);
			assertSame(serviceHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(serviceHandler.getResourceId(), resources.getFirst().getId());

			//MainRunTourStart
			assertInstanceOf(LSPTourStartEventHandler.class, eventHandlers.get(2));
			LSPTourStartEventHandler mainRunStartHandler = (LSPTourStartEventHandler) eventHandlers.get(2);
			assertSame(mainRunStartHandler.getCarrierService().getServiceLinkId(), toLinkId);
			assertEquals(mainRunStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
            assertEquals(mainRunStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunStartHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunStartHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), 0.0);
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(4).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(5).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(6).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), solutionElements.get(2));
			assertSame(mainRunStartHandler.getLspShipment(), shipment);
			assertSame(mainRunStartHandler.getResourceId(), planElements.get(4).getResourceId());
			assertSame(mainRunStartHandler.getResourceId(), planElements.get(5).getResourceId());
			assertSame(mainRunStartHandler.getResourceId(), planElements.get(6).getResourceId());
			assertSame(mainRunStartHandler.getResourceId(), resources.get(2).getId());

			//MainRunTourEnd
			assertInstanceOf(LSPTourEndEventHandler.class, eventHandlers.get(3));
			LSPTourEndEventHandler mainRunEndHandler = (LSPTourEndEventHandler) eventHandlers.get(3);
			assertSame(mainRunEndHandler.getCarrierService().getServiceLinkId(), toLinkId);
			assertEquals(mainRunEndHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
            assertEquals(mainRunEndHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunEndHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunEndHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), 0.0);
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(4).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(5).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(6).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), solutionElements.get(2));
			assertSame(mainRunEndHandler.getLspShipment(), shipment);
			assertSame(mainRunEndHandler.getResourceId(), planElements.get(4).getResourceId());
			assertSame(mainRunEndHandler.getResourceId(), planElements.get(5).getResourceId());
			assertSame(mainRunEndHandler.getResourceId(), planElements.get(6).getResourceId());
			assertSame(mainRunEndHandler.getResourceId(), resources.get(2).getId());

			//DistributionTourStart
			assertInstanceOf(LSPTourStartEventHandler.class, eventHandlers.get(4));
			LSPTourStartEventHandler lspTourStartEventHandler = (LSPTourStartEventHandler) eventHandlers.get(4);
			assertSame(lspTourStartEventHandler.getCarrierService().getServiceLinkId(), shipment.getTo());
			assertEquals(lspTourStartEventHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
            assertEquals(lspTourStartEventHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, lspTourStartEventHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, lspTourStartEventHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), 0.0);
			assertSame(lspTourStartEventHandler.getLogisticChainElement(), planElements.get(8).getLogisticChainElement());
			assertSame(lspTourStartEventHandler.getLogisticChainElement(), planElements.get(9).getLogisticChainElement());
			assertSame(lspTourStartEventHandler.getLogisticChainElement(), planElements.get(10).getLogisticChainElement());
			assertSame(lspTourStartEventHandler.getLogisticChainElement(), solutionElements.get(4));
			assertSame(lspTourStartEventHandler.getLspShipment(), shipment);
			assertSame(lspTourStartEventHandler.getResourceId(), planElements.get(8).getResourceId());
			assertSame(lspTourStartEventHandler.getResourceId(), planElements.get(9).getResourceId());
			assertSame(lspTourStartEventHandler.getResourceId(), planElements.get(10).getResourceId());
			assertSame(lspTourStartEventHandler.getResourceId(), resources.get(4).getId());

			//DistributionServiceStart
			assertInstanceOf(DistributionServiceStartEventHandler.class, eventHandlers.get(5));
			DistributionServiceStartEventHandler distributionServiceHandler = (DistributionServiceStartEventHandler) eventHandlers.get(5);
			assertSame(distributionServiceHandler.getCarrierService().getServiceLinkId(), shipment.getTo());
			assertEquals(distributionServiceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
            assertEquals(distributionServiceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, distributionServiceHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, distributionServiceHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), 0.0);
			assertSame(distributionServiceHandler.getLogisticChainElement(), planElements.get(8).getLogisticChainElement());
			assertSame(distributionServiceHandler.getLogisticChainElement(), planElements.get(9).getLogisticChainElement());
			assertSame(distributionServiceHandler.getLogisticChainElement(), planElements.get(10).getLogisticChainElement());
			assertSame(distributionServiceHandler.getLogisticChainElement(), solutionElements.get(4));
			assertSame(distributionServiceHandler.getLspShipment(), shipment);
			assertSame(distributionServiceHandler.getResource().getId(), planElements.get(8).getResourceId());
			assertSame(distributionServiceHandler.getResource().getId(), planElements.get(9).getResourceId());
			assertSame(distributionServiceHandler.getResource().getId(), planElements.get(10).getResourceId());
			assertSame(distributionServiceHandler.getResource().getId(), resources.get(4).getId());

		}

		for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
			for (LogisticChainElement element : solution.getLogisticChainElements()) {
				assertTrue(element.getIncomingShipments().getLspShipmentsWTime().isEmpty());
				if (element.getNextElement() != null) {
					assertTrue(element.getOutgoingShipments().getLspShipmentsWTime().isEmpty());
				} else {
					assertFalse(element.getOutgoingShipments().getLspShipmentsWTime().isEmpty());
				}
			}
		}

	}

}
