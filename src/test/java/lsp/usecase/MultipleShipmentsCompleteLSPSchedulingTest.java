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

package lsp.usecase;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import static org.junit.Assert.*;

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

	@Before
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder collectionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		collectionVehicleTypeBuilder.setCapacity(10);
		collectionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		collectionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		collectionVehicleTypeBuilder.setFixCost(49);
		collectionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = collectionVehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarrierUtils.createCarrier(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);


		Id<LSPResource> collectionResourceId = Id.create("CollectionCarrierResource", LSPResource.class);
		UsecaseUtils.CollectionCarrierResourceBuilder collectionResourceBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(collectionResourceId, network);
		collectionResourceBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		collectionResourceBuilder.setCarrier(collectionCarrier);
		collectionResourceBuilder.setLocationLinkId(collectionLinkId);
		collectionResource = collectionResourceBuilder.build();

		Id<LogisticChainElement> collectionElementId = Id.create("CollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder collectionBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionResource);
		collectionElement = collectionBuilder.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		UsecaseUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());
		firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTranshipmentHubResource);
		firstHubElement = firstHubElementBuilder.build();

		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType mainRunType = mainRunVehicleTypeBuilder.build();


		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		toLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunType);


		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierUtils.createCarrier(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		Id<LSPResource> mainRunId = Id.create("MainRunResource", LSPResource.class);
		UsecaseUtils.MainRunCarrierResourceBuilder mainRunResourceBuilder = UsecaseUtils.MainRunCarrierResourceBuilder.newInstance(mainRunId, network);
		mainRunResourceBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
		mainRunResourceBuilder.setFromLinkId(fromLinkId);
		mainRunResourceBuilder.setToLinkId(toLinkId);
		mainRunResourceBuilder.setCarrier(mainRunCarrier);
		mainRunResource = mainRunResourceBuilder.build();

		Id<LogisticChainElement> mainRunElementId = Id.create("MainRunElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder mainRunBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(mainRunElementId);
		mainRunBuilder.setResource(mainRunResource);
		mainRunElement = mainRunBuilder.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder secondSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		secondSchedulerBuilder.setCapacityNeedFixed(10);
		secondSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		UsecaseUtils.TransshipmentHubBuilder secondTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario);
		secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());
		secondTranshipmentHubResource = secondTransshipmentHubBuilder.build();

		Id<LogisticChainElement> secondHubElementId = Id.create("SecondHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder secondHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTranshipmentHubResource);
		secondHubElement = secondHubElementBuilder.build();

		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder distributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		distributionVehicleTypeBuilder.setCapacity(10);
		distributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		distributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		distributionVehicleTypeBuilder.setFixCost(49);
		distributionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType distributionType = distributionVehicleTypeBuilder.build();

		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierUtils.createCarrier(distributionCarrierId);
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);


		Id<LSPResource> distributionResourceId = Id.create("DistributionCarrierResource", LSPResource.class);
		UsecaseUtils.DistributionCarrierResourceBuilder distributionResourceBuilder = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(distributionResourceId, network);
		distributionResourceBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		distributionResourceBuilder.setCarrier(distributionCarrier);
		distributionResourceBuilder.setLocationLinkId(distributionLinkId);
		distributionResource = distributionResourceBuilder.build();

		Id<LogisticChainElement> distributionElementId = Id.create("DistributionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder distributionBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionResource);
		distributionElement = distributionBuilder.build();

		collectionElement.connectWithNextElement(firstHubElement);
		firstHubElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondHubElement);
		secondHubElement.connectWithNextElement(distributionElement);

		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LSPUtils.LogisticChainBuilder completeSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(solutionId);
		completeSolutionBuilder.addLogisticChainElement(collectionElement);
		completeSolutionBuilder.addLogisticChainElement(firstHubElement);
		completeSolutionBuilder.addLogisticChainElement(mainRunElement);
		completeSolutionBuilder.addLogisticChainElement(secondHubElement);
		completeSolutionBuilder.addLogisticChainElement(distributionElement);
		LogisticChain completeSolution = completeSolutionBuilder.build();

		ShipmentAssigner assigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addLogisticChain(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		resourcesList.add(firstTranshipmentHubResource);
		resourcesList.add(mainRunResource);
		resourcesList.add(secondTranshipmentHubResource);
		resourcesList.add(distributionResource);


		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());
		Random rand = new Random(1);
		for (int i = 1; i < 100; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			int capacityDemand = rand.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, rand);
				Link pendingToLink = linkList.get(0);
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
				Link pendingFromLink = linkList.get(0);
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
			LSPShipment shipment = builder.build();
			lsp.assignShipmentToLSP(shipment);
		}
		lsp.scheduleLogisticChains();

	}

	@Test
	public void testCompletedLSPScheduling() {

		for (LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
			System.out.println();
			for (ShipmentPlanElement element : elementList) {
				System.out.println(element.getLogisticChainElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());
			}
			System.out.println();
		}

		ArrayList<LogisticChainElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getLogisticChain().iterator().next().getLogisticChainElements());
		ArrayList<LSPResource> resources = new ArrayList<>(lsp.getResources());

		for (LSPShipment shipment : lsp.getShipments()) {
			assertEquals(11, shipment.getShipmentPlan().getPlanElements().size());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			planElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

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

			assertEquals("HANDLE", planElements.get(7).getElementType());
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

			assertEquals("HANDLE", planElements.get(3).getElementType());
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
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.get(0).getStartTime() <= (24*3600));
			assertSame(planElements.get(0).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(0).getLogisticChainElement(), collectionElement);
		}

		assertEquals(1, firstTranshipmentHubResource.getSimulationTrackers().size());
		ArrayList<EventHandler> eventHandlers = new ArrayList<>(firstTranshipmentHubResource.getSimulationTrackers());
		assertTrue(eventHandlers.iterator().next() instanceof TransshipmentHubTourEndEventHandler);
		TransshipmentHubTourEndEventHandler reloadEventHandler = (TransshipmentHubTourEndEventHandler) eventHandlers.iterator().next();
		Iterator<Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair>> iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();

		while (iter.hasNext()) {
			Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair> entry = iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticChainElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), shipment.getFrom());
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticChainElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));
		}

		assertEquals(1, secondTranshipmentHubResource.getSimulationTrackers().size());
		eventHandlers = new ArrayList<>(secondTranshipmentHubResource.getSimulationTrackers());
		assertTrue(eventHandlers.iterator().next() instanceof TransshipmentHubTourEndEventHandler);
		reloadEventHandler = (TransshipmentHubTourEndEventHandler) eventHandlers.iterator().next();
		iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();

		while (iter.hasNext()) {
			Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair> entry = iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticChainElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), toLinkId);
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticChainElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));
		}

		for (LSPShipment shipment : lsp.getShipments()) {
			assertEquals(6, shipment.getSimulationTrackers().size());
			eventHandlers = new ArrayList<>(shipment.getSimulationTrackers());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			planElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler endHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertSame(endHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(endHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(endHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(endHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(endHandler.getElement(), planElements.get(0).getLogisticChainElement());
			assertSame(endHandler.getElement(), planElements.get(1).getLogisticChainElement());
			assertSame(endHandler.getElement(), planElements.get(2).getLogisticChainElement());
			assertSame(endHandler.getElement(), solutionElements.get(0));
			assertSame(endHandler.getLspShipment(), shipment);
			assertSame(endHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(endHandler.getResourceId(), resources.get(0).getId());

			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEndEventHandler);
			CollectionServiceEndEventHandler serviceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertSame(serviceHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(serviceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(serviceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(serviceHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(serviceHandler.getElement(), planElements.get(0).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), planElements.get(1).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), planElements.get(2).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), solutionElements.get(0));
			assertSame(serviceHandler.getLspShipment(), shipment);
			assertSame(serviceHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(serviceHandler.getResourceId(), resources.get(0).getId());

			assertTrue(eventHandlers.get(2) instanceof MainRunTourStartEventHandler);
			MainRunTourStartEventHandler mainRunStartHandler = (MainRunTourStartEventHandler) eventHandlers.get(2);
			assertSame(mainRunStartHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(4).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(5).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(6).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), solutionElements.get(2));
			assertSame(mainRunStartHandler.getLspShipment(), shipment);
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(4).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(5).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(6).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), resources.get(2).getId());

			assertTrue(eventHandlers.get(3) instanceof MainRunTourEndEventHandler);
			MainRunTourEndEventHandler mainRunEndHandler = (MainRunTourEndEventHandler) eventHandlers.get(3);
			assertSame(mainRunEndHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunEndHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunEndHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(4).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(5).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(6).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), solutionElements.get(2));
			assertSame(mainRunEndHandler.getLspShipment(), shipment);
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(4).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(5).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(6).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), resources.get(2).getId());

			assertTrue(eventHandlers.get(4) instanceof DistributionTourStartEventHandler);
			DistributionTourStartEventHandler distributionStartHandler = (DistributionTourStartEventHandler) eventHandlers.get(4);
			assertSame(distributionStartHandler.getCarrierService().getLocationLinkId(), shipment.getTo());
			assertEquals(distributionStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(distributionStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, distributionStartHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, distributionStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(distributionStartHandler.getElement(), planElements.get(8).getLogisticChainElement());
			assertSame(distributionStartHandler.getElement(), planElements.get(9).getLogisticChainElement());
			assertSame(distributionStartHandler.getElement(), planElements.get(10).getLogisticChainElement());
			assertSame(distributionStartHandler.getElement(), solutionElements.get(4));
			assertSame(distributionStartHandler.getLspShipment(), shipment);
			assertSame(distributionStartHandler.getResource().getId(), planElements.get(8).getResourceId());
			assertSame(distributionStartHandler.getResource().getId(), planElements.get(9).getResourceId());
			assertSame(distributionStartHandler.getResource().getId(), planElements.get(10).getResourceId());
			assertSame(distributionStartHandler.getResource().getId(), resources.get(4).getId());

			assertTrue(eventHandlers.get(5) instanceof DistributionServiceStartEventHandler);
			DistributionServiceStartEventHandler distributionServiceHandler = (DistributionServiceStartEventHandler) eventHandlers.get(5);
			assertSame(distributionServiceHandler.getCarrierService().getLocationLinkId(), shipment.getTo());
			assertEquals(distributionServiceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(distributionServiceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, distributionServiceHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, distributionServiceHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
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

		for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChain()) {
			for (LogisticChainElement element : solution.getLogisticChainElements()) {
				assertTrue(element.getIncomingShipments().getShipments().isEmpty());
				if (element.getNextElement() != null) {
					assertTrue(element.getOutgoingShipments().getShipments().isEmpty());
				} else {
					assertFalse(element.getOutgoingShipments().getShipments().isEmpty());
				}
			}
		}

	}

}
