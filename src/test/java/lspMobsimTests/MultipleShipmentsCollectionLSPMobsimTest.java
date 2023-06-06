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
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class MultipleShipmentsCollectionLSPMobsimTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private LSP collectionLSP;

	@Before
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

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
		Link collectionLink = network.getLinks().get(collectionLinkId);
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLink.getId(), collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);


		Id<LSPResource> adapterId = Id.create("CollectionCarrierResource", LSPResource.class);
				UsecaseUtils.CollectionCarrierResourceBuilder adapterBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(carrier, network);
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource collectionResource = adapterBuilder.build();

		Id<LogisticChainElement> elementId = Id.create("CollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder collectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionResource);
		LogisticChainElement collectionElement = collectionElementBuilder.build();

		Id<LogisticChain> collectionSolutionId = Id.create("CollectionSolution", LogisticChain.class);
		LSPUtils.LogisticChainBuilder collectionSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addLogisticChainElement(collectionElement);
		LogisticChain collectionSolution = collectionSolutionBuilder.build();

		ShipmentAssigner assigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addLogisticChain(collectionSolution);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);

		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();

		List<Link> linkList = new LinkedList<>(network.getLinks().values());

		int numberOfShipments = 1 + MatsimRandom.getRandom().nextInt(50);
		for (int i = 1; i < 1 + numberOfShipments; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			//Random random = new Random(1);
			int capacityDemand = 1 + MatsimRandom.getRandom().nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, MatsimRandom.getRandom());
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

		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(collectionLSP);
		LSPs lsps = new LSPs(lspList);

		Controler controler = new Controler(scenario);

		LSPUtils.addLSPs(scenario, lsps);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();
	}

	@Test
	public void testCollectionLSPMobsim() {

		for (LSPShipment shipment : collectionLSP.getShipments()) {
			assertFalse(shipment.getShipmentLog().getPlanElements().isEmpty());
			assertEquals(ShipmentUtils.findPlanOfShipment(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().size(), shipment.getShipmentLog().getPlanElements().size());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(ShipmentUtils.findPlanOfShipment(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			scheduleElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<>(shipment.getShipmentLog().getPlanElements().values());
			logElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

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
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}
}
