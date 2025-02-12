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

package org.matsim.freight.logistics.lspMobsimTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler;
import static org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CollectionLSPMobsimTest {

	private static final Logger log = LogManager.getLogger(CollectionLSPMobsimTest.class);
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private LSP collectionLSP;
	private Carrier carrier;
	private LSPResource collectionResource;

	@BeforeEach
	public void initialize() {

		// create config:
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

		// load scenario:
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// define vehicle type:
				Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50/3.6);

		// define starting link (?):
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = scenario.getNetwork().getLinks().get(collectionLinkId);
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLink.getId(), collectionVehType);

		// define carrier:
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarriersUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);



		collectionResource  = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(carrier)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler(scenario)).setLocationLinkId(collectionLinkId)
				.build();

		final LogisticChainElement collectionElement;
		{
			Id<LogisticChainElement> elementId = Id.create("CollectionElement", LogisticChainElement.class);
			LSPUtils.LogisticChainElementBuilder collectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(elementId);
			collectionElementBuilder.setResource(collectionResource);
			collectionElement = collectionElementBuilder.build();
		}
		final LogisticChain collectionSolution;
		{
			Id<LogisticChain> collectionSolutionId = Id.create("CollectionSolution", LogisticChain.class);
			LSPUtils.LogisticChainBuilder collectionSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(collectionSolutionId);
			collectionSolutionBuilder.addLogisticChainElement(collectionElement);
			collectionSolution = collectionSolutionBuilder.build();
		}
		final LSPPlan collectionPlan;
		{
			InitialShipmentAssigner assigner = createSingleLogisticChainShipmentAssigner();
			collectionPlan = LSPUtils.createLSPPlan();
			collectionPlan.setInitialShipmentAssigner(assigner);
			collectionPlan.addLogisticChain(collectionSolution);
		}
		{
			final LSPUtils.LSPBuilder collectionLSPBuilder;
			ArrayList<LSPResource> resourcesList = new ArrayList<>();
			collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
			collectionLSPBuilder.setInitialPlan(collectionPlan);
			resourcesList.add(collectionResource);
			LogisticChainScheduler simpleScheduler = createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
			simpleScheduler.setBufferTime(300);
			collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);
			collectionLSP = collectionLSPBuilder.build();
		}
		{
			List<Link> linkList = new LinkedList<>(scenario.getNetwork().getLinks().values());
			for (int i = 1; i < 2; i++) {
				Id<LspShipment> id = Id.create(i, LspShipment.class);
				LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
				int capacityDemand = 1 + MatsimRandom.getRandom().nextInt(4);
				builder.setCapacityDemand(capacityDemand);

				while (true) {
					Collections.shuffle(linkList, MatsimRandom.getRandom());
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
		final LSPs lsps;
		{
			ArrayList<LSP> lspList = new ArrayList<>();
			lspList.add(collectionLSP);
			lsps = new LSPs(lspList);
		}
		Controller controller = ControllerUtils.createController(scenario);
		controller.getEvents().addHandler((BasicEventHandler) event -> log.warn(event));

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});

		LSPUtils.addLSPs(scenario, lsps);
		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controller.run();
	}

	@Test
	public void testCollectionLSPMobsim() {
		for (LspShipment shipment : collectionLSP.getLspShipments()) {
			assertFalse(shipment.getShipmentLog().getPlanElements().isEmpty());

			log.warn("");
			log.warn("shipment schedule plan elements:");
			for (LspShipmentPlanElement planElement : LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().values()) {
				log.warn(planElement);
			}
			log.warn("");
			log.warn("shipment log plan elements:");
			for (LspShipmentPlanElement planElement : shipment.getShipmentLog().getPlanElements().values()) {
				log.warn(planElement);
			}
			log.warn("");

			assertEquals(LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().size(), shipment.getShipmentLog().getPlanElements().size());
			ArrayList<LspShipmentPlanElement> scheduleElements = new ArrayList<>(LspShipmentUtils.getOrCreateShipmentPlan(collectionLSP.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			scheduleElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<LspShipmentPlanElement> logElements = new ArrayList<>(shipment.getShipmentLog().getPlanElements().values());
			logElements.sort(LspShipmentUtils.createShipmentPlanElementComparator());

			//Das muss besser in den SchedulingTest rein
			assertSame(collectionLSP.getResources().iterator().next(), collectionResource);
			LSPCarrierResource carrierResource = (LSPCarrierResource) collectionResource;
			assertSame(carrierResource.getCarrier(), carrier);
			assertEquals(1, carrier.getServices().size());

			for (LspShipmentPlanElement scheduleElement : scheduleElements) {
				LspShipmentPlanElement logElement = logElements.get(scheduleElements.indexOf(scheduleElement));
				assertEquals(scheduleElement.getElementType(), logElement.getElementType());
				assertSame(scheduleElement.getResourceId(), logElement.getResourceId());
				assertSame(scheduleElement.getLogisticChainElement(), logElement.getLogisticChainElement());
				assertEquals(scheduleElement.getStartTime(), logElement.getStartTime(), 300);
			}
		}
	}

	@Test
	public void compareEvents(){
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}

}
