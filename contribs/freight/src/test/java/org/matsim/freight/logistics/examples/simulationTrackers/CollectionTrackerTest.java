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

package org.matsim.freight.logistics.examples.simulationTrackers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.CollectionCarrierResourceBuilder;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CollectionTrackerTest {
	private static final Logger log = LogManager.getLogger(CollectionTrackerTest.class);
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private Network network;
	private Carrier carrier;
	private LogisticChain logisticChain;
	private double shareOfFixedCosts;

	@BeforeEach
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		this.network = scenario.getNetwork();

		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
				Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		org.matsim.vehicles.VehicleType collectionVehType = VehicleUtils.createVehicleType(vehicleTypeId, TransportMode.car);
		collectionVehType.getCapacity().setOther(10);
		collectionVehType.getCostInformation().setCostsPerMeter(0.0004);
		collectionVehType.getCostInformation().setCostsPerSecond(0.38);
		collectionVehType.getCostInformation().setFixedCost(49.);
		collectionVehType.setMaximumVelocity(50 / 3.6);

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = network.getLinks().get(collectionLinkId);

		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLink.getId(), collectionVehType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarriersUtils.createCarrier(carrierId);
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
		logisticChain = collectionSolutionBuilder.build();

		{
			shareOfFixedCosts = 0.2;
			LinearCostTracker tracker = new LinearCostTracker(shareOfFixedCosts);
			tracker.getEventHandlers().add(new TourStartHandler(scenario));
			tracker.getEventHandlers().add(new CollectionServiceHandler(scenario));
			tracker.getEventHandlers().add(new DistanceAndTimeHandler(scenario));
			// I think that it would be better to use delegation inside LinearCostTracker, i.e. to not expose getEventHandlers(). kai, jun'22

			logisticChain.addSimulationTracker(tracker);
		}

		InitialShipmentAssigner assigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setInitialShipmentAssigner(assigner);
		collectionPlan.addLogisticChain(logisticChain);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);

		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		LSP collectionLSP = collectionLSPBuilder.build();

		List<Link> linkList = new LinkedList<>(network.getLinks().values());


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


		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(collectionLSP);
		LSPs lsps = new LSPs(lspList);

		LSPUtils.addLSPs(scenario, lsps);

		Controller controller = ControllerUtils.createController(scenario);

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
//		config.network().setInputFile(ExamplesUtils.getTestScenarioURL("logistics-2regions") + "2regions-network.xml");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controller.run();
	}

	@Test
	public void testCollectionTracker() {

		assertEquals(1, logisticChain.getSimulationTrackers().size());
		LSPSimulationTracker<LogisticChain> tracker = logisticChain.getSimulationTrackers().iterator().next();
		assertInstanceOf(LinearCostTracker.class, tracker);
		LinearCostTracker linearTracker = (LinearCostTracker) tracker;
		double totalScheduledCosts = 0;
		double totalTrackedCosts = 0;
		double totalScheduledWeight = 0;
		double totalTrackedWeight = 0;
		int totalNumberOfScheduledShipments = 0;
		int totalNumberOfTrackedShipments = 0;
		for (EventHandler handler : linearTracker.getEventHandlers()) {
			if (handler instanceof TourStartHandler startHandler) {
				double scheduledCosts = 0;
				for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledCosts += scheduledTour.getVehicle().getType().getCostInformation().getFixedCosts();
					totalScheduledCosts += scheduledCosts;
				}
				double trackedCosts = startHandler.getVehicleFixedCosts();
				totalTrackedCosts += trackedCosts;
				assertEquals(trackedCosts, scheduledCosts, 0.1);
			}
			if (handler instanceof CollectionServiceHandler serviceHandler) {
				totalTrackedWeight = serviceHandler.getTotalWeightOfShipments();
				totalNumberOfTrackedShipments = serviceHandler.getTotalNumberOfShipments();
				double scheduledCosts = 0;
				for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					Tour tour = scheduledTour.getTour();
					for (TourElement element : tour.getTourElements()) {
						if (element instanceof ServiceActivity activity) {
							scheduledCosts += activity.getService().getServiceDuration() * scheduledTour.getVehicle().getType().getCostInformation().getCostsPerSecond();
							totalScheduledCosts += scheduledCosts;
                            totalScheduledWeight += activity.getService().getCapacityDemand();
							totalNumberOfScheduledShipments++;
						}
					}
				}
				double trackedCosts = serviceHandler.getTotalLoadingCosts();
				totalTrackedCosts += trackedCosts;
				assertEquals(trackedCosts, scheduledCosts, 0.1);
			}
			if (handler instanceof DistanceAndTimeHandler distanceHandler) {
				double trackedTimeCosts = distanceHandler.getTimeCosts();
				totalTrackedCosts += trackedTimeCosts;
				double scheduledTimeCosts = 0;
				for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					Tour tour = scheduledTour.getTour();
					for (TourElement element : tour.getTourElements()) {
						if (element instanceof Leg leg) {
							scheduledTimeCosts += leg.getExpectedTransportTime() * scheduledTour.getVehicle().getType().getCostInformation().getCostsPerSecond();
						}
					}
				}
				totalScheduledCosts += scheduledTimeCosts;
				assertEquals(scheduledTimeCosts, trackedTimeCosts, Math.max(scheduledTimeCosts, trackedTimeCosts) * 0.01);

				double scheduledDistanceCosts = 0;
				double trackedDistanceCosts = distanceHandler.getDistanceCosts();
				totalTrackedCosts += trackedDistanceCosts;
				for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledDistanceCosts += network.getLinks().get(scheduledTour.getTour().getEndLinkId()).getLength() * scheduledTour.getVehicle().getType().getCostInformation().getCostsPerMeter();
					for (TourElement element : scheduledTour.getTour().getTourElements()) {
						System.out.println(element);
						if (element instanceof Leg leg) {
							NetworkRoute linkRoute = (NetworkRoute) leg.getRoute();
							for (Id<Link> linkId : linkRoute.getLinkIds()) {
								scheduledDistanceCosts += network.getLinks().get(linkId).getLength() * scheduledTour.getVehicle().getType().getCostInformation().getCostsPerMeter();
							}
						}
						if (element instanceof ServiceActivity activity) {
							scheduledDistanceCosts += network.getLinks().get(activity.getLocation()).getLength() * scheduledTour.getVehicle().getType().getCostInformation().getCostsPerMeter();
							// (I think that we need this since the last link is not in the route.  Or is it?  kai, jul'22)
							// (yy I do not understand why we do not need to do this for the end activity of the tour.)
						}
					}
					log.warn("scheduledDistanceCosts={}", scheduledDistanceCosts);
				}
				totalScheduledCosts += scheduledDistanceCosts;
				assertEquals(scheduledDistanceCosts, trackedDistanceCosts, Math.max(scheduledDistanceCosts, trackedDistanceCosts) * 0.01);
			}
		}

		double linearTrackedCostsPerShipment = (totalTrackedCosts * (1 - shareOfFixedCosts)) / totalTrackedWeight;
		double linearScheduledCostsPerShipment = (totalScheduledCosts * (1 - shareOfFixedCosts)) / totalScheduledWeight;
		double fixedTrackedCostsPerShipment = (totalTrackedCosts * shareOfFixedCosts) / totalNumberOfTrackedShipments;
		double fixedScheduledCostsPerShipment = (totalScheduledCosts * shareOfFixedCosts) / totalNumberOfScheduledShipments;

		assertEquals(totalTrackedWeight, totalScheduledWeight, 0);
		assertEquals(totalNumberOfTrackedShipments, totalNumberOfScheduledShipments, 0);
		assertEquals(totalTrackedCosts, totalScheduledCosts, Math.max(totalScheduledCosts, totalTrackedCosts) * 0.01);
		assertEquals(linearTrackedCostsPerShipment, linearScheduledCostsPerShipment, Math.max(linearTrackedCostsPerShipment, linearScheduledCostsPerShipment) * 0.01);
		assertEquals(fixedScheduledCostsPerShipment, fixedTrackedCostsPerShipment, Math.max(fixedTrackedCostsPerShipment, fixedScheduledCostsPerShipment) * 0.01);

//		LSPInfo info = collectionSolution.getAttributes().iterator().next();
//		assertTrue(info instanceof CostInfo );
//		CostInfo costInfo = (CostInfo) info;

//		assertEquals(costInfo.getVariableCost() ,linearTrackedCostsPerShipment , Math.max(linearTrackedCostsPerShipment,costInfo.getVariableCost() ) * 0.01 );
//		assertEquals(costInfo.getVariableCost()  , linearScheduledCostsPerShipment, Math.max(linearScheduledCostsPerShipment,costInfo.getVariableCost() ) * 0.01 );
//		assertEquals(costInfo.getFixedCost() ,fixedTrackedCostsPerShipment, Math.max(fixedTrackedCostsPerShipment,costInfo.getFixedCost()) * 0.01 );
//		assertEquals(costInfo.getFixedCost(),fixedScheduledCostsPerShipment, Math.max(fixedScheduledCostsPerShipment,costInfo.getFixedCost()) * 0.01 );

		// I cannot say how the above was supposed to work, since costInfo.getVariableCost() was the same in both cases.  kai, may'22

		assertEquals(2, logisticChain.getAttributes().size());
		assertEquals(LSPUtils.getVariableCost(logisticChain), linearTrackedCostsPerShipment, Math.max(linearTrackedCostsPerShipment, LSPUtils.getVariableCost(logisticChain)) * 0.01);
		assertEquals(LSPUtils.getVariableCost(logisticChain), linearScheduledCostsPerShipment, Math.max(linearScheduledCostsPerShipment, LSPUtils.getVariableCost(logisticChain)) * 0.01);
		assertEquals(LSPUtils.getFixedCost(logisticChain), fixedTrackedCostsPerShipment, Math.max(fixedTrackedCostsPerShipment, LSPUtils.getFixedCost(logisticChain)) * 0.01);
		assertEquals(LSPUtils.getFixedCost(logisticChain), fixedScheduledCostsPerShipment, Math.max(fixedScheduledCostsPerShipment, LSPUtils.getFixedCost(logisticChain)) * 0.01);
	}

	@Test
	public void compareEvents(){
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}
}
