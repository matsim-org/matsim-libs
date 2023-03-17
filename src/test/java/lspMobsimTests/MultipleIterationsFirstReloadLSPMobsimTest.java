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
import org.junit.Assert;
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
import org.matsim.contrib.freight.controler.CarrierStrategyManager;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class MultipleIterationsFirstReloadLSPMobsimTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private LSP lsp;

	@Before
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);


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
		collectionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType collectionType = collectionVehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLinkId, collectionType);

		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarrierUtils.createCarrier(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);


		LSPResource collectionResource = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier, network)
				.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		LogisticChainElement collectionElement  = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		UsecaseUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());
		LSPResource firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTranshipmentHubResource);
		LogisticChainElement firstHubElement = firstHubElementBuilder.build();

		collectionElement.connectWithNextElement(firstHubElement);


		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LSPUtils.LogisticChainBuilder completeSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(solutionId);
		completeSolutionBuilder.addLogisticChainElement(collectionElement);
		completeSolutionBuilder.addLogisticChainElement(firstHubElement);
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


		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();

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

			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			LSPShipment shipment = builder.build();
			lsp.assignShipmentToLSP(shipment);
		}
		lsp.scheduleLogisticChains();

		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(lsp);
		LSPs lsps = new LSPs(lspList);

		Controler controler = new Controler(scenario);

		LSPUtils.addLSPs(scenario, lsps);
		controler.addOverridingModule(new LSPModule());
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( LSPStrategyManager.class ).toInstance( new LSPModule.LSPStrategyManagerEmptyImpl() );
				bind( CarrierStrategyManager.class ).toProvider(() -> {
					CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new RandomPlanSelector<>()), null, 1);
					return strategyManager;
				});
			}
		} );
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1 + MatsimRandom.getRandom().nextInt(10));
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		controler.run();
	}

	@Test
	public void testFirstReloadLSPMobsim() {

		for (LSPShipment shipment : lsp.getShipments()) {
			assertFalse(shipment.getLog().getPlanElements().isEmpty());
			assertEquals(shipment.getShipmentPlan().getPlanElements().size(), shipment.getLog().getPlanElements().size());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			scheduleElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<>(shipment.getLog().getPlanElements().values());
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
