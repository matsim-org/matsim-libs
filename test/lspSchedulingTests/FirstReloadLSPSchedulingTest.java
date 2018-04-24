package lspSchedulingTests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
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
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.CollectionServiceEventHandler;
import lsp.usecase.CollectionTourEndEventHandler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.ReloadingPoint;
import lsp.usecase.ReloadingPointEventHandler;
import lsp.usecase.ReloadingPointScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
import lsp.usecase.ReloadingPointEventHandler.ReloadingPointEventHandlerPair;
import lsp.LSP;
import lsp.LSPImpl;
import lsp.LSPPlanImpl;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.LogisticsSolutionImpl;
import lsp.ShipmentAssigner;
import lsp.SolutionScheduler;
import lsp.resources.Resource;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.AbstractShipmentPlanElementComparator;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public class FirstReloadLSPSchedulingTest {
	private Network network;
	private LogisticsSolution completeSolution;
	private ShipmentAssigner assigner;
	private LSPPlanImpl completePlan;
	private SolutionScheduler simpleScheduler;
	private LSP lsp;	
	private Resource firstReloadingPointAdapter;
	private LogisticsSolutionElement firstReloadElement;
	private LogisticsSolutionElement collectionElement;
	private Resource collectionAdapter;
	
	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
        this.network = scenario.getNetwork();	
	
   
		CollectionCarrierScheduler collectionScheduler = new CollectionCarrierScheduler();
		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder collectionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		collectionVehicleTypeBuilder.setCapacity(10);
		collectionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		collectionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		collectionVehicleTypeBuilder.setFixCost(49);
		collectionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType collectionType = collectionVehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId);
		collectionCarrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarrierImpl.newInstance(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);
		
		
		Id<Resource> collectionAdapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder collectionAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(collectionAdapterId, network);
		collectionAdapterBuilder.setCollectionScheduler(collectionScheduler);
		collectionAdapterBuilder.setCarrier(collectionCarrier);
		collectionAdapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = collectionAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionAdapter);
		collectionElement = collectionBuilder.build();
		
		ReloadingPointScheduler.Builder firstReloadingSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<Resource> firstReloadingId = Id.create("ReloadingPoint1", Resource.class);
        Id<Link> firstReloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        
        ReloadingPoint.Builder firstReloadingPointBuilder = ReloadingPoint.Builder.newInstance(firstReloadingId, firstReloadingLinkId);
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        firstReloadingPointAdapter = firstReloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> firstReloadingElementId = Id.create("FirstReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder firstReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(firstReloadingElementId);
		firstReloadingElementBuilder.setResource(firstReloadingPointAdapter);
		firstReloadElement = firstReloadingElementBuilder.build();
		
		collectionElement.setNextElement(firstReloadElement);
		firstReloadElement.setPreviousElement(collectionElement);
		
		
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder completeSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(solutionId);
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolution = completeSolutionBuilder.build();
		
		assigner = new DeterministicShipmentAssigner();
		completePlan = new LSPPlanImpl();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPImpl.Builder completeLSPBuilder = LSPImpl.Builder.getInstance();
		completeLSPBuilder.setInitialPlan(completePlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		completeLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		resourcesList.add(firstReloadingPointAdapter);
		

		simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		
		 for(int i = 1; i < 11; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
	        	int capacityDemand = new Random().nextInt(10);
	        	builder.setCapacityDemand(capacityDemand);
	        	
	        	while(true) {
	        		Collections.shuffle(linkList);
	        		Link pendingToLink = linkList.get(0);
	        		if((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
	        			pendingToLink.getFromNode().getCoord().getX() >= 14000 &&       			
	        			pendingToLink.getToNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getToNode().getCoord().getY() <= 4000  &&
	        			pendingToLink.getToNode().getCoord().getX() >= 14000	)) {
	        		   builder.setToLinkId(pendingToLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	while(true) {
	        		Collections.shuffle(linkList);
	        		Link pendingFromLink = linkList.get(0);
	        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getY() <= 4000    ) {
	        		   builder.setFromLinkId(pendingFromLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setEndTimeWindow(endTimeWindow);
	        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setStartTimeWindow(startTimeWindow);
	        	builder.setServiceTime(capacityDemand * 60);
	        	LSPShipment shipment = builder.build();
	        	lsp.assignShipmentToLSP(shipment);
	        }
		lsp.scheduleSoultions();
	
	}

	@Test
	public void testFirstReloadLSPScheduling() {
		
		for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<AbstractShipmentPlanElement> scheduleElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(scheduleElements, new AbstractShipmentPlanElementComparator());
			
			System.out.println();
			for(int i = 0; i < shipment.getSchedule().getPlanElements().size(); i++) {
				System.out.println("Scheduled: " + scheduleElements.get(i).getSolutionElement().getId() + "  " + scheduleElements.get(i).getResourceId() +"  "+ scheduleElements.get(i).getElementType() + " Start: " + scheduleElements.get(i).getStartTime() + " End: " + scheduleElements.get(i).getEndTime());
			}
			System.out.println();	
		}
	
	
		for(LSPShipment shipment : lsp.getShipments()){
			assertTrue(shipment.getSchedule().getPlanElements().size() == 4);
			ArrayList<AbstractShipmentPlanElement> planElements = new ArrayList<>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(planElements, new AbstractShipmentPlanElementComparator());
			assertTrue(planElements.get(3).getElementType() == "HANDLE");
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(3).getResourceId() == firstReloadingPointAdapter.getId());
			assertTrue(planElements.get(3).getSolutionElement() == firstReloadElement);	
			
			assertTrue(planElements.get(3).getStartTime() == planElements.get(2).getEndTime());
			
			assertTrue(planElements.get(2).getElementType() == "UNLOAD");
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(2).getResourceId() == collectionAdapter.getId());
			assertTrue(planElements.get(2).getSolutionElement() == collectionElement);	
		
			assertTrue(planElements.get(2).getStartTime() == planElements.get(1).getEndTime());
			
			assertTrue(planElements.get(1).getElementType() == "TRANSPORT");
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(1).getResourceId() == collectionAdapter.getId());
			assertTrue(planElements.get(1).getSolutionElement() == collectionElement);	
			
			assertTrue(planElements.get(1).getStartTime() == planElements.get(0).getEndTime());
			
			assertTrue(planElements.get(0).getElementType() == "LOAD");
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.get(0).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(0).getResourceId() == collectionAdapter.getId());
			assertTrue(planElements.get(0).getSolutionElement() == collectionElement);	
			
		}
			
		assertTrue(firstReloadingPointAdapter.getEventHandlers().size() ==1);
		ArrayList<EventHandler> eventHandlers = new ArrayList<EventHandler>(firstReloadingPointAdapter.getEventHandlers());
		assertTrue(eventHandlers.iterator().next() instanceof ReloadingPointEventHandler);
		ReloadingPointEventHandler reloadEventHandler = (ReloadingPointEventHandler) eventHandlers.iterator().next();
		Iterator<Entry<CarrierService,ReloadingPointEventHandler.ReloadingPointEventHandlerPair>>  iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<CarrierService,ReloadingPointEventHandler.ReloadingPointEventHandlerPair> entry =  iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticsSolutionElement element = entry.getValue().element;
			assertTrue(service.getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(service.getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(service.getServiceDuration() == shipment.getServiceTime());
			boolean handledByReloadingPoint = false;
			for(LogisticsSolutionElement clientElement : reloadEventHandler.getReloadingPoint().getClientElements()) {
				if(clientElement == element) {
					handledByReloadingPoint = true;
				}
			}
			assertTrue(handledByReloadingPoint);
			//This asserts that the shipments waiting for handling have been handled and the queues have been cleared
			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));	
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));	
		}
		
	
		
		for(LSPShipment shipment : lsp.getShipments()) {
			assertTrue(shipment.getEventHandlers().size() == 2);
			eventHandlers = new ArrayList<EventHandler>(shipment.getEventHandlers());
			ArrayList<AbstractShipmentPlanElement> planElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			
			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler endHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertTrue(endHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(endHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(endHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(endHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(endHandler.getElement() == planElements.get(2).getSolutionElement());
			assertTrue(endHandler.getElement() == lsp.getSelectedPlan().getSolutions().iterator().next().getSolutionElements().iterator().next());
			assertTrue(endHandler.getLspShipment() == shipment);
			assertTrue(endHandler.getResourceId() == planElements.get(2).getResourceId());
			assertTrue(endHandler.getResourceId()  == lsp.getResources().iterator().next().getId());
			
			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEventHandler);
			CollectionServiceEventHandler serviceHandler = (CollectionServiceEventHandler) eventHandlers.get(1);
			assertTrue(serviceHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(serviceHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(serviceHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(serviceHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(serviceHandler.getElement() == planElements.get(0).getSolutionElement());
			assertTrue(serviceHandler.getElement() == lsp.getSelectedPlan().getSolutions().iterator().next().getSolutionElements().iterator().next());
			assertTrue(serviceHandler.getLspShipment() == shipment);
			assertTrue(serviceHandler.getResourceId() == planElements.get(0).getResourceId());
			assertTrue(serviceHandler.getResourceId()  == lsp.getResources().iterator().next().getId());
		}	
	}
}
