package testLSPWithCostTracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.End;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.Start;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.controler.LSPModule;
import lsp.events.EventUtils;
import lsp.functions.Info;
import lsp.functions.InfoFunctionValue;
import lsp.LSP;
import lsp.LSPImpl;
import lsp.LSPPlanImpl;
import lsp.LSPs;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.LogisticsSolutionImpl;
import lsp.ShipmentAssigner;
import lsp.SolutionScheduler;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.resources.Resource;
import lsp.scoring.LSPScoringModuleImpl;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;
import lsp.tracking.SimulationTracker;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.SimpleForwardSolutionScheduler;



public class CollectionTrackerTest {

	private Network network;
	private LSP collectionLSP;	
	private Carrier carrier;
	private Resource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	private LogisticsSolution collectionSolution;
	private double shareOfFixedCosts;
	
	@Before
	public void initialize() {
		
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
        this.network = scenario.getNetwork();
            
        CollectionCarrierScheduler scheduler = new CollectionCarrierScheduler();
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = network.getLinks().get(collectionLinkId);
		
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLink.getId());
		carrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = adapterBuilder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionAdapter);
		collectionElement = collectionElementBuilder.build();
		
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder collectionSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		collectionSolution = collectionSolutionBuilder.build();
		
		shareOfFixedCosts = 0.2;
        LinearCostTracker tracker = new LinearCostTracker(shareOfFixedCosts);
		tracker.getEventHandlers().add(new TourStartHandler());
		tracker.getEventHandlers().add(new CollectionServiceHandler());
		tracker.getEventHandlers().add(new DistanceAndTimeHandler(network));
		
		collectionSolution.addSimulationTracker(tracker);
		
		ShipmentAssigner assigner = new DeterministicShipmentAssigner();
		LSPPlanImpl collectionPlan = new LSPPlanImpl();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);
	
		LSPImpl.Builder collectionLSPBuilder = LSPImpl.Builder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
	    Id<Link> toLinkId = collectionLinkId;
	
	        
	    for(int i = 1; i < 2; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
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
        	
        	builder.setToLinkId(toLinkId);
        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setEndTimeWindow(endTimeWindow);
        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setStartTimeWindow(startTimeWindow);
        	builder.setServiceTime(capacityDemand * 60);
        	LSPShipment shipment = builder.build();
        	collectionLSP.assignShipmentToLSP(shipment);
        }
		collectionLSP.scheduleSoultions();
	
		
		
		ArrayList<LSP> lspList = new ArrayList<LSP>();
		lspList.add(collectionLSP);
		LSPs lsps = new LSPs(lspList);
		
		Controler controler = new Controler(config);
		
		LSPModule module = new LSPModule(lsps, new LSPReplanningModuleImpl(lsps), new LSPScoringModuleImpl(lsps), EventUtils.getStandardEventCreators());

		controler.addOverridingModule(module);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("input\\lsp\\network\\2regions.xml");
		controler.run();
	}
	
	@Test
	public void testCollectionTracker() {
		assertTrue(collectionSolution.getSimulationTrackers().size() == 1);
		SimulationTracker tracker = collectionSolution.getSimulationTrackers().iterator().next();
		assertTrue(tracker instanceof LinearCostTracker);
		LinearCostTracker linearTracker = (LinearCostTracker) tracker;
		double totalScheduledCosts = 0;
		double totalTrackedCosts = 0;
		double totalScheduledWeight = 0;
		double totalTrackedWeight = 0;
		int totalNumberOfScheduledShipments = 0;
		int totalNumberOfTrackedShipments = 0;
		for(EventHandler handler : linearTracker.getEventHandlers()) {
			if(handler instanceof TourStartHandler) {
				TourStartHandler startHandler = (TourStartHandler) handler;
				double scheduledCosts = 0;
				for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledCosts += scheduledTour.getVehicle().getVehicleType().getVehicleCostInformation().fix;
					totalScheduledCosts += scheduledCosts;
				}
				double trackedCosts = startHandler.getVehicleFixedCosts();
				totalTrackedCosts += trackedCosts;
				assertEquals(trackedCosts, scheduledCosts, 0.1);
			}
			if(handler instanceof CollectionServiceHandler) {
				CollectionServiceHandler serviceHandler = (CollectionServiceHandler) handler;
				totalTrackedWeight = serviceHandler.getTotalWeightOfShipments();
				totalNumberOfTrackedShipments = serviceHandler.getTotalNumberOfShipments();
				double scheduledCosts = 0;
				for(ScheduledTour scheduledTour: carrier.getSelectedPlan().getScheduledTours()) {
					Tour tour = scheduledTour.getTour();
					for(TourElement element : tour.getTourElements()) {
						if(element instanceof ServiceActivity){
							ServiceActivity activity = (ServiceActivity) element;
							scheduledCosts += activity.getService().getServiceDuration() * scheduledTour.getVehicle().getVehicleType().getVehicleCostInformation().perTimeUnit;
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
			if(handler instanceof DistanceAndTimeHandler) {
				DistanceAndTimeHandler distanceHandler = (DistanceAndTimeHandler) handler;
				double trackedTimeCosts = distanceHandler.getTimeCosts();
				totalTrackedCosts += trackedTimeCosts;
				double scheduledTimeCosts = 0;			
				for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {	
					Tour tour = scheduledTour.getTour();
					for(TourElement element : tour.getTourElements() ) {
						if(element instanceof Leg) {
							Leg leg = (Leg) element;
							scheduledTimeCosts += leg.getExpectedTransportTime() * scheduledTour.getVehicle().getVehicleType().getVehicleCostInformation().perTimeUnit;
						}
					}
				}
				totalScheduledCosts += scheduledTimeCosts;
				assertEquals(scheduledTimeCosts, trackedTimeCosts, Math.max(scheduledTimeCosts,trackedTimeCosts)*0.01);
				
				double scheduledDistanceCosts = 0;
				double trackedDistanceCosts = distanceHandler.getDistanceCosts();
				totalTrackedCosts += trackedDistanceCosts;
				for(ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
					scheduledDistanceCosts += network.getLinks().get(scheduledTour.getTour().getEndLinkId()).getLength() * scheduledTour.getVehicle().getVehicleType().getVehicleCostInformation().perDistanceUnit;
					for(TourElement element : scheduledTour.getTour().getTourElements()) {
						System.out.println(element);
						if(element instanceof Leg) {
							Leg leg = (Leg) element;
							NetworkRoute linkRoute = (NetworkRoute) leg.getRoute();
							for(Id<Link> linkId: linkRoute.getLinkIds()) {
								scheduledDistanceCosts  += network.getLinks().get(linkId).getLength() * scheduledTour.getVehicle().getVehicleType().getVehicleCostInformation().perDistanceUnit;
							}			
						}
						if(element instanceof ServiceActivity) {
							ServiceActivity activity = (ServiceActivity) element;
							scheduledDistanceCosts  += network.getLinks().get(activity.getLocation()).getLength() * scheduledTour.getVehicle().getVehicleType().getVehicleCostInformation().perDistanceUnit;
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
		
		assertTrue(collectionSolution.getInfos().size() == 1);
		Info info = collectionSolution.getInfos().iterator().next();
		assertTrue(info instanceof CostInfo);
		CostInfo costInfo = (CostInfo) info;
		assertTrue(costInfo.getFunction() instanceof CostInfoFunction);
		CostInfoFunction function = (CostInfoFunction) costInfo.getFunction();
		ArrayList<InfoFunctionValue> values = new ArrayList<InfoFunctionValue>(function.getValues());
		for(InfoFunctionValue value : values) {
			if(value instanceof LinearCostFunctionValue) {
				LinearCostFunctionValue linearValue = (LinearCostFunctionValue) value;
				assertEquals(Double.parseDouble(linearValue.getValue()),linearTrackedCostsPerShipment, Math.max(linearTrackedCostsPerShipment,Double.parseDouble(linearValue.getValue())) * 0.01 );
				assertEquals(Double.parseDouble(linearValue.getValue()),linearScheduledCostsPerShipment, Math.max(linearScheduledCostsPerShipment,Double.parseDouble(linearValue.getValue())) * 0.01 );
			}
			if(value instanceof FixedCostFunctionValue) {
				FixedCostFunctionValue fixedValue = (FixedCostFunctionValue) value;
				assertEquals(Double.parseDouble(fixedValue.getValue()),fixedTrackedCostsPerShipment, Math.max(fixedTrackedCostsPerShipment,Double.parseDouble(fixedValue.getValue())) * 0.01 );
				assertEquals(Double.parseDouble(fixedValue.getValue()),fixedScheduledCostsPerShipment, Math.max(fixedScheduledCostsPerShipment,Double.parseDouble(fixedValue.getValue())) * 0.01 );
			}
		}
	}
}
