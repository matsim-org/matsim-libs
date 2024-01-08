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

package org.matsim.freight.logistics.example.lsp.lspScoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler;
import static org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.CollectionCarrierUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

public class CollectionLSPScoringTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private final int numberOfShipments = 25;
	private LSP collectionLSP;

	@BeforeEach
	public void initialize() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		VehicleType collectionVehicleType = CarrierVehicleType.Builder.newInstance(Id.create("CollectionCarrierVehicleType", VehicleType.class))
				.setCapacity(10).setCostPerDistanceUnit(0.0004).setCostPerTimeUnit(0.38).setFixCost(49).setMaxVelocity(50 / 3.6).build();

		Link collectionLink = network.getLinks().get(Id.createLinkId("(4 2) (4 3)")); // (make sure that the link exists)

		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLink.getId(), collectionVehicleType);

		Carrier carrier = CarriersUtils.createCarrier(Id.create("CollectionCarrier", Carrier.class));
		carrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().addType(collectionVehicleType).addVehicle(carrierVehicle).setFleetSize(FleetSize.INFINITE).build());

		LSPResource collectionResource = CollectionCarrierUtils.CollectionCarrierResourceBuilder.newInstance(carrier, network)
				.setCollectionScheduler(CollectionCarrierUtils.createDefaultCollectionCarrierScheduler()).setLocationLinkId(collectionLink.getId()).build();

		LogisticChainElement collectionElement = LSPUtils.LogisticChainElementBuilder
				.newInstance(Id.create("CollectionElement", LogisticChainElement.class)).setResource(collectionResource).build();

		LogisticChain collectionSolution = LSPUtils.LogisticChainBuilder.newInstance(Id.create("CollectionSolution", LogisticChain.class))
				.addLogisticChainElement(collectionElement).build();

		collectionLSP = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class))
				.setInitialPlan(LSPUtils.createLSPPlan().setAssigner(createSingleLogisticChainShipmentAssigner()).addLogisticChain(collectionSolution))
				.setLogisticChainScheduler(createDefaultSimpleForwardLogisticChainScheduler(Collections.singletonList(collectionResource)))
//				.setSolutionScorer(new ExampleLSPScoring.TipScorer())
				.build();

//		TipEventHandler handler = new TipEventHandler();
//		LSPAttribute<Double> value = LSPInfoFunctionUtils.createInfoFunctionValue("TIP IN EUR" );
//		LSPAttributes function = LSPInfoFunctionUtils.createDefaultInfoFunction();
//		function.getAttributes().add(value );
//		TipInfo info = new TipInfo();
//		TipScorer.TipSimulationTracker tipTracker = new TipScorer.TipSimulationTracker();
//		collectionResource.addSimulationTracker(tipTracker);
//		TipScorer tipScorer = new TipScorer();
//		collectionLSP.addSimulationTracker( tipScorer );
//		collectionLSP.setScorer(tipScorer);

		List<Link> linkList = new LinkedList<>(network.getLinks().values());

		for (int i = 1; i < (numberOfShipments + 1); i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
				Link pendingFromLink = linkList.get(0);
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
						&& pendingFromLink.getFromNode().getCoord().getY() <= 4000
						&& pendingFromLink.getToNode().getCoord().getX() <= 4000
						&& pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(collectionLink.getId());
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

		LSPUtils.addLSPs(scenario, lsps);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule( new LSPModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( LSPScorerFactory.class ).toInstance( () -> new ExampleLSPScoring.TipScorer() );
			}
		});

		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();
	}

	@Test
	public void testCollectionLSPScoring() {
		System.out.println(collectionLSP.getSelectedPlan().getScore());
		assertEquals(numberOfShipments, collectionLSP.getShipments().size());
		assertEquals(numberOfShipments, collectionLSP.getSelectedPlan().getLogisticChains().iterator().next().getShipmentIds()
				.size());
		assertTrue(collectionLSP.getSelectedPlan().getScore() > 0);
		assertTrue(collectionLSP.getSelectedPlan().getScore() <= (numberOfShipments * 5));
		/*noch zu testen
		 * tipTracker
		 * InfoFunction
		 * Info
		 * Scorer
		 */
	}

	@Test
	public void compareEvents(){
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}
}
