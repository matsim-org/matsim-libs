package lsp.usecase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;

import lsp.*;
import lsp.shipment.*;
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

import lsp.resources.LSPResource;

public class SecondReloadLSPSchedulingTest {
	private Network network;
	private LogisticsSolution completeSolution;
	private ShipmentAssigner assigner;
	private LSPPlan completePlan;
	private SolutionScheduler simpleScheduler;
	private LSP lsp;	
	private LSPResource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	private LSPResource firstReloadingPointAdapter;
	private LogisticsSolutionElement firstReloadElement;
	private LSPResource mainRunAdapter;
	private LogisticsSolutionElement mainRunElement;
	private LSPResource secondReloadingPointAdapter;
	private LogisticsSolutionElement secondReloadElement;
	private Id<Link> toLinkId;
	
	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();


		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder collectionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		collectionVehicleTypeBuilder.setCapacity(10);
		collectionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		collectionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		collectionVehicleTypeBuilder.setFixCost(49);
		collectionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = collectionVehicleTypeBuilder.build();
		
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
		
		
		Id<LSPResource> collectionAdapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder collectionAdapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(collectionAdapterId, network);
		collectionAdapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		collectionAdapterBuilder.setCarrier(collectionCarrier);
		collectionAdapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = collectionAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(collectionElementId );
		collectionBuilder.setResource(collectionAdapter);
		collectionElement = collectionBuilder.build();
		
		UsecaseUtils.ReloadingPointSchedulerBuilder firstReloadingSchedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<LSPResource> firstReloadingId = Id.create("ReloadingPoint1", LSPResource.class);
        Id<Link> firstReloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        
        UsecaseUtils.ReloadingPointBuilder firstReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(firstReloadingId, firstReloadingLinkId);
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        firstReloadingPointAdapter = firstReloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> firstReloadingElementId = Id.create("FirstReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder firstReloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(firstReloadingElementId );
		firstReloadingElementBuilder.setResource(firstReloadingPointAdapter);
		firstReloadElement = firstReloadingElementBuilder.build();
		
		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType mainRunType = mainRunVehicleTypeBuilder.build();
				
		
		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		toLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId);
		mainRunCarrierVehicle.setVehicleType(mainRunType);
				
		
		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierImpl.newInstance(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		Id<LSPResource> mainRunId = Id.create("MainRunAdapter", LSPResource.class);
        UsecaseUtils.MainRunCarrierAdapterBuilder mainRunAdapterBuilder = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
        mainRunAdapterBuilder.setFromLinkId(fromLinkId);
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId(toLinkId));
        mainRunAdapterBuilder.setCarrier(mainRunCarrier);
        mainRunAdapter = mainRunAdapterBuilder.build();
	
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(mainRunElementId );
		mainRunBuilder.setResource(mainRunAdapter);
		mainRunElement = mainRunBuilder.build();
		
		UsecaseUtils.ReloadingPointSchedulerBuilder secondSchedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        secondSchedulerBuilder.setCapacityNeedFixed(10);
        secondSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<LSPResource> secondReloadingId = Id.create("ReloadingPoint2", LSPResource.class);
        Id<Link> secondReloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        UsecaseUtils.ReloadingPointBuilder secondReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(secondReloadingId, secondReloadingLinkId);
        secondReloadingPointBuilder.setReloadingScheduler(secondSchedulerBuilder.build());
        secondReloadingPointAdapter = secondReloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> secondReloadingElementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder secondReloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(secondReloadingElementId );
		secondReloadingElementBuilder.setResource(secondReloadingPointAdapter);
		secondReloadElement = secondReloadingElementBuilder.build();
		
		
		
		collectionElement.setNextElement(firstReloadElement);
		firstReloadElement.setPreviousElement(collectionElement);
		firstReloadElement.setNextElement(mainRunElement);
		mainRunElement.setPreviousElement(firstReloadElement);
		mainRunElement.setNextElement(secondReloadElement);
		secondReloadElement.setPreviousElement(mainRunElement);
		
		
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder completeSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(solutionId );
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolution = completeSolutionBuilder.build();
		
		assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		completeLSPBuilder.setInitialPlan(completePlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		completeLSPBuilder.setId(collectionLSPId);
		ArrayList<LSPResource> resourcesList = new ArrayList<LSPResource>();
		resourcesList.add(collectionAdapter);
		resourcesList.add(firstReloadingPointAdapter);
		resourcesList.add(mainRunAdapter);
		resourcesList.add(secondReloadingPointAdapter);

		simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		
		 for(int i = 1; i < 2; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
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
	public void testSecondReloadLSPScheduling() {
		
		for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<ShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(elementList, new ShipmentPlanElementComparator());
			System.out.println();
			for(ShipmentPlanElement element : elementList) {
				System.out.println(element.getSolutionElement().getId() + " " + element.getResourceId() + " " + element.getElementType() + " " + element.getStartTime() + " " + element.getEndTime());	
			}			
			System.out.println();
		}
	
		for(LSPShipment shipment : lsp.getShipments()){
			assertTrue(shipment.getSchedule().getPlanElements().size() == 8);
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(planElements, new ShipmentPlanElementComparator());
			
			assertTrue(planElements.get(7).getElementType() == "HANDLE");
			assertTrue(planElements.get(7).getEndTime() >= (0));
			assertTrue(planElements.get(7).getEndTime() <= (24*3600));
			assertTrue(planElements.get(7).getStartTime() <= planElements.get(7).getEndTime());
			assertTrue(planElements.get(7).getStartTime() >= (0));
			assertTrue(planElements.get(7).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(7).getResourceId() == secondReloadingPointAdapter.getId());
			assertTrue(planElements.get(7).getSolutionElement() == secondReloadElement);	
			
			assertTrue(planElements.get(7).getStartTime() == (planElements.get(6).getEndTime() + 300));
			
			assertTrue(planElements.get(6).getElementType() == "UNLOAD");
			assertTrue(planElements.get(6).getEndTime() >= (0));
			assertTrue(planElements.get(6).getEndTime() <= (24*3600));
			assertTrue(planElements.get(6).getStartTime() <= planElements.get(6).getEndTime());
			assertTrue(planElements.get(6).getStartTime() >= (0));
			assertTrue(planElements.get(6).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(6).getResourceId() == mainRunAdapter.getId());
			assertTrue(planElements.get(6).getSolutionElement() == mainRunElement);	
			
			assertTrue(planElements.get(6).getStartTime() == planElements.get(5).getEndTime());
			
			assertTrue(planElements.get(5).getElementType() == "TRANSPORT");
			assertTrue(planElements.get(5).getEndTime() >= (0));
			assertTrue(planElements.get(5).getEndTime() <= (24*3600));
			assertTrue(planElements.get(5).getStartTime() <= planElements.get(5).getEndTime());
			assertTrue(planElements.get(5).getStartTime() >= (0));
			assertTrue(planElements.get(5).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(5).getResourceId() == mainRunAdapter.getId());
			assertTrue(planElements.get(5).getSolutionElement() == mainRunElement);	
		
			assertTrue(planElements.get(5).getStartTime() == planElements.get(4).getEndTime());
			
			assertTrue(planElements.get(4).getElementType() == "LOAD");
			assertTrue(planElements.get(4).getEndTime() >= (0));
			assertTrue(planElements.get(4).getEndTime() <= (24*3600));
			assertTrue(planElements.get(4).getStartTime() <= planElements.get(4).getEndTime());
			assertTrue(planElements.get(4).getStartTime() >= (0));
			assertTrue(planElements.get(4).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(4).getResourceId() == mainRunAdapter.getId());
			assertTrue(planElements.get(4).getSolutionElement() == mainRunElement);	
			
			assertTrue(planElements.get(4).getStartTime() >= (planElements.get(3).getEndTime() / (1.0001)) + 300);
			
			assertTrue(planElements.get(3).getElementType() == "HANDLE");
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(3).getResourceId() == firstReloadingPointAdapter.getId());
			assertTrue(planElements.get(3).getSolutionElement() == firstReloadElement);	
			
			assertTrue(planElements.get(3).getStartTime() == (planElements.get(2).getEndTime() + 300));
			
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
		assertTrue(eventHandlers.iterator().next() instanceof ReloadingPointTourEndEventHandler);
		ReloadingPointTourEndEventHandler reloadEventHandler = (ReloadingPointTourEndEventHandler) eventHandlers.iterator().next();
		Iterator<Entry<CarrierService, ReloadingPointTourEndEventHandler.ReloadingPointEventHandlerPair>>  iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<CarrierService, ReloadingPointTourEndEventHandler.ReloadingPointEventHandlerPair> entry =  iter.next();
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
			
			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));	
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));	
		}
	
		assertTrue(secondReloadingPointAdapter.getEventHandlers().size() ==1);
		eventHandlers = new ArrayList<EventHandler>(secondReloadingPointAdapter.getEventHandlers());
		assertTrue(eventHandlers.iterator().next() instanceof ReloadingPointTourEndEventHandler);
		reloadEventHandler = (ReloadingPointTourEndEventHandler) eventHandlers.iterator().next();
		iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<CarrierService, ReloadingPointTourEndEventHandler.ReloadingPointEventHandlerPair> entry =  iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticsSolutionElement element = entry.getValue().element;
			assertTrue(service.getLocationLinkId() == toLinkId);
			assertTrue(service.getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(service.getServiceDuration() == shipment.getServiceTime());
			boolean handledByReloadingPoint = false;
			for(LogisticsSolutionElement clientElement : reloadEventHandler.getReloadingPoint().getClientElements()) {
				if(clientElement == element) {
					handledByReloadingPoint = true;
				}
			}
			assertTrue(handledByReloadingPoint);
			
			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));	
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));	
		}
		
		for(LSPShipment shipment : lsp.getShipments()) {
			assertTrue(shipment.getEventHandlers().size() == 4);
			eventHandlers = new ArrayList<EventHandler>(shipment.getEventHandlers());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<ShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(planElements, new ShipmentPlanElementComparator());
			ArrayList<LogisticsSolutionElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getSolutions().iterator().next().getSolutionElements());
			ArrayList<LSPResource> resources = new ArrayList<>(lsp.getResources());
	
			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler collectionEndHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertTrue(collectionEndHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(collectionEndHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(collectionEndHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(collectionEndHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(collectionEndHandler.getElement() == planElements.get(0).getSolutionElement());
			assertTrue(collectionEndHandler.getElement() == planElements.get(1).getSolutionElement());
			assertTrue(collectionEndHandler.getElement() == planElements.get(2).getSolutionElement());
			assertTrue(collectionEndHandler.getElement() == solutionElements.get(0));
			assertTrue(collectionEndHandler.getLspShipment() == shipment);
			assertTrue(collectionEndHandler.getResourceId() == planElements.get(0).getResourceId());
			assertTrue(collectionEndHandler.getResourceId() == planElements.get(1).getResourceId());
			assertTrue(collectionEndHandler.getResourceId() == planElements.get(2).getResourceId());
			assertTrue(collectionEndHandler.getResourceId()  == resources.get(0).getId());
			
			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEndEventHandler);
			CollectionServiceEndEventHandler collectionServiceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertTrue(collectionServiceHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(collectionServiceHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(collectionServiceHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(collectionServiceHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(collectionServiceHandler.getElement() == planElements.get(0).getSolutionElement());
			assertTrue(collectionServiceHandler.getElement() == planElements.get(1).getSolutionElement());
			assertTrue(collectionServiceHandler.getElement() == planElements.get(2).getSolutionElement());
			assertTrue(collectionServiceHandler.getElement() == solutionElements.get(0));
			assertTrue(collectionServiceHandler.getLspShipment() == shipment);
			assertTrue(collectionServiceHandler.getResourceId() == planElements.get(0).getResourceId());
			assertTrue(collectionServiceHandler.getResourceId() == planElements.get(1).getResourceId());
			assertTrue(collectionServiceHandler.getResourceId() == planElements.get(2).getResourceId());
			assertTrue(collectionServiceHandler.getResourceId()  == resources.get(0).getId());
			
			assertTrue(eventHandlers.get(2) instanceof MainRunTourStartEventHandler);
			MainRunTourStartEventHandler mainRunStartHandler = (MainRunTourStartEventHandler) eventHandlers.get(2);
			assertTrue(mainRunStartHandler.getCarrierService().getLocationLinkId() == toLinkId);
			assertTrue(mainRunStartHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(mainRunStartHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getStart() == 0);
			assertTrue(mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd() == Integer.MAX_VALUE);
			assertTrue(mainRunStartHandler.getSolutionElement() == planElements.get(4).getSolutionElement());
			assertTrue(mainRunStartHandler.getSolutionElement() == planElements.get(5).getSolutionElement());
			assertTrue(mainRunStartHandler.getSolutionElement() == planElements.get(6).getSolutionElement());
			assertTrue(mainRunStartHandler.getSolutionElement() == solutionElements.get(2));
			assertTrue(mainRunStartHandler.getLspShipment() == shipment);
			assertTrue(mainRunStartHandler.getResource().getId() == planElements.get(4).getResourceId());
			assertTrue(mainRunStartHandler.getResource().getId() == planElements.get(5).getResourceId());
			assertTrue(mainRunStartHandler.getResource().getId() == planElements.get(6).getResourceId());
			assertTrue(mainRunStartHandler.getResource().getId()  == resources.get(2).getId());
			
			assertTrue(eventHandlers.get(3) instanceof MainRunTourEndEventHandler);
			MainRunTourEndEventHandler mainRunEndHandler = (MainRunTourEndEventHandler) eventHandlers.get(3);
			assertTrue(mainRunEndHandler.getCarrierService().getLocationLinkId() == toLinkId);
			assertTrue(mainRunEndHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(mainRunEndHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getStart() == 0);
			assertTrue(mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getEnd() == Integer.MAX_VALUE);
			assertTrue(mainRunEndHandler.getSolutionElement() == planElements.get(4).getSolutionElement());
			assertTrue(mainRunEndHandler.getSolutionElement() == planElements.get(5).getSolutionElement());
			assertTrue(mainRunEndHandler.getSolutionElement() == planElements.get(6).getSolutionElement());
			assertTrue(mainRunEndHandler.getSolutionElement() == solutionElements.get(2));
			assertTrue(mainRunEndHandler.getLspShipment() == shipment);
			assertTrue(mainRunEndHandler.getResource().getId() == planElements.get(4).getResourceId());
			assertTrue(mainRunEndHandler.getResource().getId() == planElements.get(5).getResourceId());
			assertTrue(mainRunEndHandler.getResource().getId() == planElements.get(6).getResourceId());
			assertTrue(mainRunEndHandler.getResource().getId()  == resources.get(2).getId());
		}
		
		for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			for(LogisticsSolutionElement element : solution.getSolutionElements()) {
				assertTrue(element.getIncomingShipments().getShipments().isEmpty());
				if(element.getNextElement() != null) {
					assertTrue(element.getOutgoingShipments().getShipments().isEmpty());	
				}
				else {
					assertFalse(element.getOutgoingShipments().getShipments().isEmpty());
				}
			}
		}
	}
}
