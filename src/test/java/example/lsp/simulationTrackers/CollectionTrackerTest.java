package example.lsp.simulationTrackers;

import lsp.*;
import lsp.controler.LSPModule;
import lsp.shipment.LSPShipmentImpl;
import org.junit.Assert;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreatorUtils;
import lsp.functions.LSPInfo;
import lsp.replanning.LSPReplanningUtils;
import lsp.resources.LSPResource;
import lsp.scoring.LSPScoringUtils;
import lsp.shipment.LSPShipment;
import lsp.controler.LSPSimulationTracker;
import lsp.usecase.*;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



public class CollectionTrackerTest {

	private Network network;
	private Carrier carrier;
	private LogisticsSolution collectionSolution;
	private double shareOfFixedCosts;

	@Before
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();
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
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = network.getLinks().get(collectionLinkId);

		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLink.getId());
		carrierVehicle.setType( collectionType );

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarrierUtils.createCarrier( carrierId );
		carrier.setCarrierCapabilities(capabilities);


		Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder adapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource collectionAdapter = adapterBuilder.build();

		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();

		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(collectionSolutionId );
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		collectionSolution = collectionSolutionBuilder.build();

		shareOfFixedCosts = 0.2;
		LinearCostTracker tracker = new LinearCostTracker(shareOfFixedCosts);
		tracker.getEventHandlers().add(new TourStartHandler() );
		tracker.getEventHandlers().add(new CollectionServiceHandler() );
		tracker.getEventHandlers().add(new DistanceAndTimeHandler(network) );

		collectionSolution.addSimulationTracker(tracker);

		ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionAdapter);

		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		LSP collectionLSP = collectionLSPBuilder.build();

		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());


		for(int i = 1; i < 2; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			LSPShipmentImpl.LSPShipmentBuilder builder = LSPShipmentImpl.LSPShipmentBuilder.newInstance(id );
			Random random = new Random(1);
			int capacityDemand = random.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while(true) {
				Collections.shuffle(linkList, random);
				Link pendingFromLink = linkList.get(0);
				if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(collectionLinkId);
			TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60 );
			LSPShipment shipment = builder.build();
			collectionLSP.assignShipmentToLSP(shipment);
		}
		collectionLSP.scheduleSolutions();



		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(collectionLSP);
		LSPs lsps = new LSPs(lspList);

		Controler controler = new Controler(config);

		LSPModule module = new LSPModule(lsps, LSPReplanningUtils.createDefaultLSPReplanningModule(lsps), LSPScoringUtils.createDefaultLSPScoringModule(lsps ), LSPEventCreatorUtils.getStandardEventCreators());

		controler.addOverridingModule(module);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		controler.run();
	}

	@Test
	public void testCollectionTracker() {
		assertEquals(1, collectionSolution.getSimulationTrackers().size());
		LSPSimulationTracker tracker = collectionSolution.getSimulationTrackers().iterator().next();
		assertTrue(tracker instanceof LinearCostTracker);
		LinearCostTracker linearTracker = (LinearCostTracker) tracker;
		double totalScheduledCosts = 0;
		double totalTrackedCosts = 0;
		double totalScheduledWeight = 0;
		double totalTrackedWeight = 0;
		int totalNumberOfScheduledShipments = 0;
		int totalNumberOfTrackedShipments = 0;
		for(EventHandler handler : linearTracker.getEventHandlers()) {
			if(handler instanceof TourStartHandler ) {
				TourStartHandler startHandler = (TourStartHandler) handler;
				double scheduledCosts = 0;
				for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledCosts += ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getFix();
					totalScheduledCosts += scheduledCosts;
				}
				double trackedCosts = startHandler.getVehicleFixedCosts();
				totalTrackedCosts += trackedCosts;
				assertEquals(trackedCosts, scheduledCosts, 0.1);
			}
			if(handler instanceof CollectionServiceHandler ) {
				CollectionServiceHandler serviceHandler = (CollectionServiceHandler) handler;
				totalTrackedWeight = serviceHandler.getTotalWeightOfShipments();
				totalNumberOfTrackedShipments = serviceHandler.getTotalNumberOfShipments();
				double scheduledCosts = 0;
				for(ScheduledTour scheduledTour: carrier.getSelectedPlan().getScheduledTours()) {
					Tour tour = scheduledTour.getTour();
					for(TourElement element : tour.getTourElements()) {
						if(element instanceof ServiceActivity){
							ServiceActivity activity = (ServiceActivity) element;
							scheduledCosts += activity.getService().getServiceDuration() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getPerTimeUnit();
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
			if(handler instanceof DistanceAndTimeHandler ) {
				DistanceAndTimeHandler distanceHandler = (DistanceAndTimeHandler) handler;
				double trackedTimeCosts = distanceHandler.getTimeCosts();
				totalTrackedCosts += trackedTimeCosts;
				double scheduledTimeCosts = 0;
				for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					Tour tour = scheduledTour.getTour();
					for(TourElement element : tour.getTourElements() ) {
						if(element instanceof Leg) {
							Leg leg = (Leg) element;
							scheduledTimeCosts += leg.getExpectedTransportTime() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getPerTimeUnit();
						}
					}
				}
				totalScheduledCosts += scheduledTimeCosts;
				assertEquals(scheduledTimeCosts, trackedTimeCosts, Math.max(scheduledTimeCosts,trackedTimeCosts)*0.01);

				double scheduledDistanceCosts = 0;
				double trackedDistanceCosts = distanceHandler.getDistanceCosts();
				totalTrackedCosts += trackedDistanceCosts;
				for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledDistanceCosts += network.getLinks().get(scheduledTour.getTour().getEndLinkId()).getLength() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getPerDistanceUnit();
					for(TourElement element : scheduledTour.getTour().getTourElements()) {
						System.out.println(element);
						if(element instanceof Leg) {
							Leg leg = (Leg) element;
							NetworkRoute linkRoute = (NetworkRoute) leg.getRoute();
							for(Id<Link> linkId: linkRoute.getLinkIds()) {
								scheduledDistanceCosts  += network.getLinks().get(linkId).getLength() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getPerDistanceUnit();
							}
						}
						if(element instanceof ServiceActivity) {
							ServiceActivity activity = (ServiceActivity) element;
							scheduledDistanceCosts  += network.getLinks().get(activity.getLocation()).getLength() * ((Vehicle) scheduledTour.getVehicle()).getType().getCostInformation().getPerDistanceUnit();
						}
					}
				}
				totalScheduledCosts += scheduledDistanceCosts;
				assertEquals(scheduledDistanceCosts, trackedDistanceCosts, Math.max(scheduledDistanceCosts,trackedDistanceCosts)*0.01);
			}
		}

		double linearTrackedCostsPerShipment = (totalTrackedCosts * (1-shareOfFixedCosts))/totalTrackedWeight;
		double linearScheduledCostsPerShipment = (totalScheduledCosts * (1-shareOfFixedCosts))/totalScheduledWeight;
		double fixedTrackedCostsPerShipment = (totalTrackedCosts * shareOfFixedCosts)/totalNumberOfTrackedShipments;
		double fixedScheduledCostsPerShipment = (totalScheduledCosts * shareOfFixedCosts)/totalNumberOfScheduledShipments;

		assertEquals(totalTrackedWeight, totalTrackedWeight, 0);
		assertEquals(totalNumberOfTrackedShipments, totalNumberOfScheduledShipments, 0);
		assertEquals(totalTrackedCosts, totalScheduledCosts, Math.max(totalScheduledCosts, totalTrackedCosts)*0.01);
		assertEquals(linearTrackedCostsPerShipment, linearScheduledCostsPerShipment, Math.max(linearTrackedCostsPerShipment, linearScheduledCostsPerShipment)*0.01);
		assertEquals(fixedScheduledCostsPerShipment, fixedTrackedCostsPerShipment, Math.max(fixedTrackedCostsPerShipment, fixedScheduledCostsPerShipment)*0.01);

		assertEquals(1, collectionSolution.getInfos().size());
		LSPInfo info = collectionSolution.getInfos().iterator().next();
		assertTrue(info instanceof CostInfo );
		CostInfo costInfo = (CostInfo) info;

		assertEquals(costInfo.getVariableCost() ,linearTrackedCostsPerShipment , Math.max(linearTrackedCostsPerShipment,costInfo.getVariableCost() ) * 0.01 );
		assertEquals(costInfo.getVariableCost()  , linearScheduledCostsPerShipment, Math.max(linearScheduledCostsPerShipment,costInfo.getVariableCost() ) * 0.01 );
		assertEquals(costInfo.getFixedCost() ,fixedTrackedCostsPerShipment, Math.max(fixedTrackedCostsPerShipment,costInfo.getFixedCost()) * 0.01 );
		assertEquals(costInfo.getFixedCost(),fixedScheduledCostsPerShipment, Math.max(fixedScheduledCostsPerShipment,costInfo.getFixedCost()) * 0.01 );
	}
}
