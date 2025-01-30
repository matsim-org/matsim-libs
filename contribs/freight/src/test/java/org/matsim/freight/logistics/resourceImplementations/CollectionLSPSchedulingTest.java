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
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.CollectionCarrierResourceBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CollectionLSPSchedulingTest {

	private LSP collectionLSP;
	private LSPResource collectionResource;
	private LogisticChainElement collectionElement;

	@BeforeEach
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
				Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50 / 3.6);

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLinkId, collectionVehType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier carrier = CarriersUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);


        Id.create("CollectionCarrierResource", LSPResource.class);
        CollectionCarrierResourceBuilder adapterBuilder = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(carrier);
		adapterBuilder.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario));
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionResource = adapterBuilder.build();

		Id<LogisticChainElement> elementId = Id.create("CollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder collectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionResource);
		collectionElement = collectionElementBuilder.build();

		Id<LogisticChain> collectionSolutionId = Id.create("CollectionSolution", LogisticChain.class);
		LSPUtils.LogisticChainBuilder collectionSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addLogisticChainElement(collectionElement);
		LogisticChain collectionSolution = collectionSolutionBuilder.build();

		InitialShipmentAssigner assigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setInitialShipmentAssigner(assigner);
		collectionPlan.addLogisticChain(collectionSolution);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);

		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());


		for (int i = 1; i < 2; i++) {
			Id<LspShipment> id = Id.create(i, LspShipment.class);
			LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
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
			LspShipment shipment = builder.build();
			collectionLSP.assignShipmentToLSP(shipment);
		}
		collectionLSP.scheduleLogisticChains();

	}

	@Test
	public void testCollectionLSPScheduling() {

		for (LspShipment shipment : collectionLSP.getLspShipments()) {
			ArrayList<LspShipmentPlanElement> scheduleElements = new ArrayList<>(LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			scheduleElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());

			System.out.println();
			for (int i = 0; i < LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().size(); i++) {
				System.out.println("Scheduled: " + scheduleElements.get(i).getLogisticChainElement().getId() + "  " + scheduleElements.get(i).getResourceId() + "  " + scheduleElements.get(i).getElementType() + " Start: " + scheduleElements.get(i).getStartTime() + " End: " + scheduleElements.get(i).getEndTime());
			}
			System.out.println();
		}

		for (LspShipment shipment : collectionLSP.getLspShipments()) {
			assertEquals(3, LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().size());
			ArrayList<LspShipmentPlanElement> planElements = new ArrayList<>(LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			assertEquals("UNLOAD", planElements.get(2).getElementType());
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));
			assertSame(planElements.get(2).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(2).getLogisticChainElement(), collectionElement);
			assertEquals("TRANSPORT", planElements.get(1).getElementType());
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertEquals(planElements.get(2).getStartTime(), planElements.get(1).getEndTime(), 0.0);
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));
			assertSame(planElements.get(1).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(1).getLogisticChainElement(), collectionElement);
			assertEquals("LOAD", planElements.get(0).getElementType());
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertEquals(planElements.get(1).getStartTime(), planElements.get(0).getEndTime(), 0.0);
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.getFirst().getStartTime() <= (24*3600));
			assertSame(planElements.getFirst().getResourceId(), collectionResource.getId());
			assertSame(planElements.getFirst().getLogisticChainElement(), collectionElement);

			assertEquals(2, shipment.getSimulationTrackers().size());
			ArrayList<EventHandler> eventHandlers = new ArrayList<>(shipment.getSimulationTrackers());

			{
			assertInstanceOf(LSPTourEndEventHandler.class, eventHandlers.getFirst());
			LSPTourEndEventHandler endHandler = (LSPTourEndEventHandler) eventHandlers.getFirst();
			assertSame(endHandler.getCarrierService().getServiceLinkId(), shipment.getFrom());
                assertEquals(endHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(endHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(endHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), shipment.getPickupTimeWindow().getStart(), 0.0);
			assertEquals(endHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), shipment.getPickupTimeWindow().getEnd(), 0.0);
			assertSame(endHandler.getLogisticChainElement(), planElements.get(2).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), collectionLSP.getSelectedPlan().getLogisticChains().iterator().next().getLogisticChainElements().iterator().next());
			assertSame(endHandler.getLspShipment(), shipment);
			assertSame(endHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(endHandler.getResourceId(), collectionLSP.getResources().iterator().next().getId());
			}

			{
			assertInstanceOf(CollectionServiceEndEventHandler.class, eventHandlers.get(1));
			CollectionServiceEndEventHandler serviceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertSame(serviceHandler.getCarrierService().getServiceLinkId(), shipment.getFrom());
                assertEquals(serviceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(serviceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(serviceHandler.getCarrierService().getServiceStaringTimeWindow().getStart(), shipment.getPickupTimeWindow().getStart(), 0.0);
			assertEquals(serviceHandler.getCarrierService().getServiceStaringTimeWindow().getEnd(), shipment.getPickupTimeWindow().getEnd(), 0.0);
			assertSame(serviceHandler.getElement(), planElements.get(1).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), collectionLSP.getSelectedPlan().getLogisticChains().iterator().next().getLogisticChainElements().iterator().next());
			assertSame(serviceHandler.getLspShipment(), shipment);
			assertSame(serviceHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(serviceHandler.getResourceId(), collectionLSP.getResources().iterator().next().getId());
			}
		}

		for (LogisticChain solution : collectionLSP.getSelectedPlan().getLogisticChains()) {
			assertEquals(1, solution.getLspShipmentIds().size());
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
