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

package lspPlanTests;

import lsp.*;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import static org.junit.Assert.*;

public class CompleteLSPPlanTest {

	private ShipmentAssigner assigner;
	private LSPPlan completePlan;
	private LogisticChain logisticChain;

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
		collectionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
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


		Id<LogisticChainElement> collectionElementId = Id.create("CollectionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder collectionBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionResourceBuilder.build());
		LogisticChainElement collectionElement = collectionBuilder.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		UsecaseUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());


		Id<LogisticChainElement> firstHubElementId = Id.create("FiretHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTransshipmentHubBuilder.build());
		LogisticChainElement firstHubElement = firstHubElementBuilder.build();

		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType mainRunType = collectionVehicleTypeBuilder.build();


		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunType);


		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = collectionCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierUtils.createCarrier(collectionCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		Id<LSPResource> mainRunId = Id.create("MainRunResource", LSPResource.class);
		UsecaseUtils.MainRunCarrierResourceBuilder mainRunResourceBuilder = UsecaseUtils.MainRunCarrierResourceBuilder.newInstance(mainRunId, network);
		mainRunResourceBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
		mainRunResourceBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
		mainRunResourceBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
		mainRunResourceBuilder.setCarrier(collectionCarrier);


		Id<LogisticChainElement> mainRunElementId = Id.create("MainRunElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder mainRunBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(mainRunElementId);
		mainRunBuilder.setResource(mainRunResourceBuilder.build());
		LogisticChainElement mainRunElement = mainRunBuilder.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder secondSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		secondSchedulerBuilder.setCapacityNeedFixed(10);
		secondSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		UsecaseUtils.TransshipmentHubBuilder secondTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario );
		secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());


		Id<LogisticChainElement> secondHubElementId = Id.create("SecondHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder secondHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTransshipmentHubBuilder.build());
		LogisticChainElement secondHubElement = secondHubElementBuilder.build();

		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType distributionType = dsitributionVehicleTypeBuilder.build();

		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierUtils.createCarrier(distributionCarrierId);
		carrier.setCarrierCapabilities(distributionCapabilities);


		Id<LSPResource> distributionResourceId = Id.create("DistributionCarrierResource", LSPResource.class);
		UsecaseUtils.DistributionCarrierResourceBuilder distributionResourceBuilder = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(distributionResourceId, network);
		distributionResourceBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		distributionResourceBuilder.setCarrier(carrier);
		distributionResourceBuilder.setLocationLinkId(distributionLinkId);


		Id<LogisticChainElement> distributionElementId = Id.create("DistributionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder distributionBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionResourceBuilder.build());
		LogisticChainElement distributionElement = distributionBuilder.build();

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
		logisticChain = completeSolutionBuilder.build();

		assigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
		completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addLogisticChain(logisticChain);
	}

	@Test
	public void testCompleteLSPPlan() {
		assertSame(completePlan.getAssigner(), assigner);
		assertNull(completePlan.getLSP());
		assertEquals(0, (double) completePlan.getScore(), 0.0);
		assertEquals(1, completePlan.getLogisticChain().size());
		assertSame(completePlan.getLogisticChain().iterator().next(), logisticChain);
	}

}
