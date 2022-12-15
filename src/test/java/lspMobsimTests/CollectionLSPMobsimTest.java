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

package lspMobsimTests;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;

import static lsp.usecase.UsecaseUtils.*;
import static org.junit.Assert.*;

public class CollectionLSPMobsimTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private static final Logger log = LogManager.getLogger(CollectionLSPMobsimTest.class);

	private LSP collectionLSP;
	private Carrier carrier;
	private LSPResource collectionResource;

	@Before
	public void initialize() {

		// create config:
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);

		// load scenario:
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// define vehicle type:
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

		// define starting link (?):
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = scenario.getNetwork().getLinks().get(collectionLinkId);
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLink.getId(), collectionType);

		// define carrier:
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);


		Id<LSPResource> adapterId = Id.create("CollectionCarrierResource", LSPResource.class);
		CollectionCarrierResourceBuilder adapterBuilder = CollectionCarrierResourceBuilder.newInstance(adapterId, scenario.getNetwork());
		adapterBuilder.setCollectionScheduler(createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionResource = adapterBuilder.build();

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
			ShipmentAssigner assigner = createSingleLogisticChainShipmentAssigner();
			collectionPlan = LSPUtils.createLSPPlan();
			collectionPlan.setAssigner(assigner);
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
			ArrayList<Link> linkList = new ArrayList<>(scenario.getNetwork().getLinks().values());
			for (int i = 1; i < 2; i++) {
				Id<LSPShipment> id = Id.create(i, LSPShipment.class);
				ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
				int capacityDemand = 1 + MatsimRandom.getRandom().nextInt(4);
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
				LSPShipment shipment = builder.build();
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
		Controler controler = new Controler(scenario);
		controler.getEvents().addHandler(new BasicEventHandler() {
			@Override
			public void handleEvent(Event event) {
				log.warn(event);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});

		LSPUtils.addLSPs(scenario, lsps);
		controler.run();
	}

	@Test
	public void testCollectionLSPMobsim() {
		for (LSPShipment shipment : collectionLSP.getShipments()) {
			assertFalse(shipment.getLog().getPlanElements().isEmpty());

			log.warn("");
			log.warn("shipment schedule plan elements:");
			for (ShipmentPlanElement planElement : shipment.getShipmentPlan().getPlanElements().values()) {
				log.warn(planElement);
			}
			log.warn("");
			log.warn("shipment log plan elements:");
			for (ShipmentPlanElement planElement : shipment.getLog().getPlanElements().values()) {
				log.warn(planElement);
			}
			log.warn("");

			assertEquals(shipment.getShipmentPlan().getPlanElements().size(), shipment.getLog().getPlanElements().size());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			scheduleElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<>(shipment.getLog().getPlanElements().values());
			logElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

			//Das muss besser in den SchedulingTest rein
			assertSame(collectionLSP.getResources().iterator().next(), collectionResource);
			LSPCarrierResource carrierResource = (LSPCarrierResource) collectionResource;
			assertSame(carrierResource.getCarrier(), carrier);
			assertEquals(1, carrier.getServices().size());

			for (ShipmentPlanElement scheduleElement : scheduleElements) {
				ShipmentPlanElement logElement = logElements.get(scheduleElements.indexOf(scheduleElement));
				assertEquals(scheduleElement.getElementType(), logElement.getElementType());
				assertSame(scheduleElement.getResourceId(), logElement.getResourceId());
				assertSame(scheduleElement.getLogisticChainElement(), logElement.getLogisticChainElement());
				assertEquals(scheduleElement.getStartTime(), logElement.getStartTime(), 300);
			}
		}
	}

	@Test
	public void compareEvents(){
		// 0 = "Files are equal".
		Assert.assertEquals(0, MatsimTestUtils.compareEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" ));
	}
}
