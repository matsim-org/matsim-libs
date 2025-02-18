/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.lspReplanning;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.CollectionCarrierResourceBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * This class is deprecated and will be removed in the future.
 *  It follows the old and no longer wanted approach.
 *  Now, an Assigner is used to assign all LSPShipments to one LogisticChain of the LSPPlan.
 *  <p></p>
 *  This class here is in contrast used as a Replanning strategy. This behavior is not wanted anymore.
 *  <p></p>
 *  Please use the new Approach as shown in
 *  org.matsim.freight.logistics.example.lsp.multipleChains.ExampleMultipleOneEchelonChainsReplanning instead.
 *  <p></p>
 *  * KMT, Jul'24
 */
@Deprecated
public class CollectionLSPReplanningTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private LSP collectionLSP;

	@BeforeEach
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);


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
		collectionVehType.setMaximumVelocity(50/3.6);

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = network.getLinks().get(collectionLinkId);
		if (collectionLink == null) {
			System.exit(1);
		}
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLink.getId(), collectionVehType);

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
		LSPResource collectionResource = adapterBuilder.build();

		Id<LogisticChainElement> elementId = Id.create("CollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder collectionElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionResource);
		LogisticChainElement collectionElement = collectionElementBuilder.build();

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


		List<Link> linkList = new LinkedList<>(network.getLinks().values());


		for (int i = 1; i < 21; i++) {
			Id<LspShipment> id = Id.create(i, LspShipment.class);
			LspShipmentUtils.LspShipmentBuilder builder = LspShipmentUtils.LspShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(10);
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


//		ShipmentAssigner maybeTodayAssigner = new MaybeTodayAssigner();
//		maybeTodayAssigner.setLSP(collectionLSP);
//		final GenericPlanStrategy<LSPPlan, LSP> strategy = new TomorrowShipmentAssignerStrategyFactory(maybeTodayAssigner).createStrategy();
//
//		GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManagerImpl<>();
//		strategyManager.addStrategy(strategy, null, 1);
//
//		LSPReplanner replanner = LSPReplanningUtils.createDefaultLSPReplanner(strategyManager);
//
//
//		collectionLSP.setReplanner(replanner);


		LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(collectionLSP)));

		Controller controller = ControllerUtils.createController(scenario);

		controller.addOverridingModule(new LSPModule() );

		controller.addOverridingModule(new AbstractModule(){
			@Override public void install(){
				bind( LSPStrategyManager.class ).toProvider(() -> {
					LSPStrategyManager manager = new LSPStrategyManagerImpl();
					{
                        final GenericPlanStrategy<LSPPlan, LSP> strategy = new TomorrowShipmentAssignerStrategyFactory(new MaybeTodayAssigner()).createStrategy();
						// (a factory makes sense if it is passed around; in this case it feels like overkill.  kai, jul'22)
						manager.addStrategy( strategy, null, 1 );
					}
					return manager;
				});

				bind( CarrierStrategyManager.class ).toProvider(() -> {
					CarrierStrategyManager strategyManager = CarrierControllerUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new RandomPlanSelector<>()), null, 1);
					return strategyManager;
				});
			}
		} );

		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(1);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controller.run();
	}



	@Test
	public void testCollectionLSPReplanning() {
		System.out.println(collectionLSP.getSelectedPlan().getLogisticChains().iterator().next().getLspShipmentIds().size());
		assertTrue(collectionLSP.getSelectedPlan().getLogisticChains().iterator().next().getLspShipmentIds().size() < 20);
	}

	@Test
	public void compareEvents(){
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}

}
