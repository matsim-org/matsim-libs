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

package example.lsp.simulationTrackers;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CollectionTrackerTest {
	private static final Logger log = LogManager.getLogger(CollectionTrackerTest.class);
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	private Network network;
	private Carrier carrier;
	private LogisticChain logisticChain;
	private double shareOfFixedCosts;

	@Before
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		this.network = scenario.getNetwork();

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
		carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);


		Id<LSPResource> adapterId = Id.create("CollectionCarrierResource", LSPResource.class);
		UsecaseUtils.CollectionCarrierResourceBuilder adapterBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(adapterId, network);
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
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

		ShipmentAssigner assigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addLogisticChain(logisticChain);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);

		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		LSP collectionLSP = collectionLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());


		for (int i = 1; i < 2; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
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

		LSPUtils.addLSPs(scenario, lsps);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
//		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		controler.run();
	}

	@Test
	public void testCollectionTracker() {

		assertEquals(1, logisticChain.getSimulationTrackers().size());
		LSPSimulationTracker<LogisticChain> tracker = logisticChain.getSimulationTrackers().iterator().next();
		assertTrue(tracker instanceof LinearCostTracker);
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
					scheduledCosts += ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getFixedCosts();
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
							scheduledCosts += activity.getService().getServiceDuration() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getCostsPerSecond();
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
							scheduledTimeCosts += leg.getExpectedTransportTime() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getCostsPerSecond();
						}
					}
				}
				totalScheduledCosts += scheduledTimeCosts;
				assertEquals(scheduledTimeCosts, trackedTimeCosts, Math.max(scheduledTimeCosts, trackedTimeCosts) * 0.01);

				double scheduledDistanceCosts = 0;
				double trackedDistanceCosts = distanceHandler.getDistanceCosts();
				totalTrackedCosts += trackedDistanceCosts;
				for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledDistanceCosts += network.getLinks().get(scheduledTour.getTour().getEndLinkId()).getLength() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getCostsPerMeter();
					for (TourElement element : scheduledTour.getTour().getTourElements()) {
						System.out.println(element);
						if (element instanceof Leg leg) {
							NetworkRoute linkRoute = (NetworkRoute) leg.getRoute();
							for (Id<Link> linkId : linkRoute.getLinkIds()) {
								scheduledDistanceCosts += network.getLinks().get(linkId).getLength() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getCostsPerMeter();
							}
						}
						if (element instanceof ServiceActivity activity) {
							scheduledDistanceCosts += network.getLinks().get(activity.getLocation()).getLength() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getCostsPerMeter();
							// (I think that we need this since the last link is not in the route.  Or is it?  kai, jul'22)
							// (yy I do not understand why we do not need to do this for the end activity of the tour.)
						}
					}
					log.warn("scheduledDistanceCosts=" + scheduledDistanceCosts);
				}
				totalScheduledCosts += scheduledDistanceCosts;
				assertEquals(scheduledDistanceCosts, trackedDistanceCosts, Math.max(scheduledDistanceCosts, trackedDistanceCosts) * 0.01);
			}
		}

		double linearTrackedCostsPerShipment = (totalTrackedCosts * (1 - shareOfFixedCosts)) / totalTrackedWeight;
		double linearScheduledCostsPerShipment = (totalScheduledCosts * (1 - shareOfFixedCosts)) / totalScheduledWeight;
		double fixedTrackedCostsPerShipment = (totalTrackedCosts * shareOfFixedCosts) / totalNumberOfTrackedShipments;
		double fixedScheduledCostsPerShipment = (totalScheduledCosts * shareOfFixedCosts) / totalNumberOfScheduledShipments;

		assertEquals(totalTrackedWeight, totalTrackedWeight, 0);
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
		MatsimTestUtils.compareEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}
}
