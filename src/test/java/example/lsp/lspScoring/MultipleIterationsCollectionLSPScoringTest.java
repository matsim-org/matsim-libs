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

package example.lsp.lspScoring;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import lsp.*;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.*;
import org.junit.Before;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import lsp.LSPModule;
import lsp.LSPResource;
import lsp.shipment.LSPShipment;

public class MultipleIterationsCollectionLSPScoringTest {

	private final int numberOfShipments = 25;
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
		if (collectionLink == null) {
			System.exit(1);
		}
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLink.getId(), collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		Id<LSPResource> adapterId = Id.create("CollectionCarrierResource", LSPResource.class);
		UsecaseUtils.CollectionCarrierResourceBuilder adapterBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(adapterId,
				network);
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource collectionResource = adapterBuilder.build();

		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder
				.newInstance(elementId);
		collectionElementBuilder.setResource(collectionResource);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();

		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder
				.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();

		ShipmentAssigner assigner = UsecaseUtils.createSingleSolutionShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		List<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);

		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
//		collectionLSPBuilder.setSolutionScorer(new ExampleLSPScoring.TipScorer());
		collectionLSP = collectionLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

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

			builder.setToLinkId(collectionLinkId);
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			LSPShipment shipment = builder.build();
			collectionLSP.assignShipmentToLSP(shipment);
		}

		collectionLSP.scheduleSolutions();

		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(collectionLSP);
		LSPs lsps = new LSPs(lspList);

		Controler controler = new Controler(scenario);

		LSPUtils.addLSPs(scenario, lsps);
		controler.addOverridingModule( new LSPModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( LSPScorerFactory.class ).toInstance( ( lsp) -> new ExampleLSPScoring.TipScorer() );
				bind( LSPStrategyManager.class ).toInstance( new LSPModule.LSPStrategyManagerEmptyImpl() );
				// yyyyyy It is NOT clear to me why one does not have to set CarrierStrategyManager.  kai, jul'22
			}
		});
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(10);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
//		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		controler.run();
	}

	@Test
	public void testCollectionLSPScoring() {
		System.out.println("score=" + collectionLSP.getSelectedPlan().getScore());
		assertEquals(numberOfShipments, collectionLSP.getShipments().size());
		assertEquals(numberOfShipments, collectionLSP.getSelectedPlan().getSolutions().iterator().next().getShipments().size());
		assertTrue(collectionLSP.getSelectedPlan().getScore() > 0);
		assertTrue(collectionLSP.getSelectedPlan().getScore() <= (numberOfShipments * 5));
	}

}
