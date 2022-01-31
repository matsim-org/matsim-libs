package lsp.usecase;

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
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.resources.LSPResource;

import static org.junit.Assert.*;

public class MultipleShipmentsCompleteLSPSchedulingTest {

	private LSP lsp;
	private LSPResource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	private LSPResource firstReloadingPointAdapter;
	private LogisticsSolutionElement firstReloadElement;
	private LSPResource mainRunAdapter;
	private LogisticsSolutionElement mainRunElement;
	private LSPResource secondReloadingPointAdapter;
	private LogisticsSolutionElement secondReloadElement;
	private LSPResource distributionAdapter;
	private LogisticsSolutionElement distributionElement;
	private Id<Link> toLinkId;
	
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
		collectionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = collectionVehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId);
		collectionCarrierVehicle.setType( collectionType );

		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarrierUtils.createCarrier( collectionCarrierId );
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
		mainRunCarrierVehicle.setType( mainRunType );


		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierUtils.createCarrier( mainRunCarrierId );
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		Id<LSPResource> mainRunId = Id.create("MainRunAdapter", LSPResource.class);
        UsecaseUtils.MainRunCarrierAdapterBuilder mainRunAdapterBuilder = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
        mainRunAdapterBuilder.setFromLinkId(fromLinkId);
        mainRunAdapterBuilder.setToLinkId(toLinkId);
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

		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder distributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		distributionVehicleTypeBuilder.setCapacity(10);
		distributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		distributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		distributionVehicleTypeBuilder.setFixCost(49);
		distributionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType distributionType = distributionVehicleTypeBuilder.build();
		
		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId);
		distributionCarrierVehicle.setType( distributionType );

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierUtils.createCarrier( distributionCarrierId );
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);
		
		
		Id<LSPResource> distributionAdapterId = Id.create("DistributionCarrierAdapter", LSPResource.class);
		UsecaseUtils.DistributionCarrierAdapterBuilder distributionAdapterBuilder = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(distributionAdapterId, network);
		distributionAdapterBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		distributionAdapterBuilder.setCarrier(distributionCarrier);
		distributionAdapterBuilder.setLocationLinkId(distributionLinkId);
		distributionAdapter = distributionAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> distributionElementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder distributionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(distributionElementId );
		distributionBuilder.setResource(distributionAdapter);
		distributionElement =    distributionBuilder.build();
		
		collectionElement.connectWithNextElement(firstReloadElement);
		firstReloadElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondReloadElement);
		secondReloadElement.connectWithNextElement(distributionElement);

		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder completeSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(solutionId );
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolutionBuilder.addSolutionElement(distributionElement);
		LogisticsSolution completeSolution = completeSolutionBuilder.build();

		ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionAdapter);
		resourcesList.add(firstReloadingPointAdapter);
		resourcesList.add(mainRunAdapter);
		resourcesList.add(secondReloadingPointAdapter);
		resourcesList.add(distributionAdapter);


		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());
		Random rand = new Random(1);
		 for(int i = 1; i < 100; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
	        	int capacityDemand = rand.nextInt(4);
	        	builder.setCapacityDemand(capacityDemand);
	        	
	        	while(true) {
	        		Collections.shuffle(linkList, rand);
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
	        		Collections.shuffle(linkList, rand);
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
	        	builder.setDeliveryServiceTime(capacityDemand * 60 );
	        	LSPShipment shipment = builder.build();
	        	lsp.assignShipmentToLSP(shipment);
	        }
		lsp.scheduleSolutions();
	
	}

	@Test
	public void testCompletedLSPScheduling() {
		
		for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			elementList.sort(new ShipmentPlanElementComparator());
			System.out.println();
			for(ShipmentPlanElement element : elementList) {
				System.out.println(element.getSolutionElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());	
			}			
			System.out.println();
		}
	
		ArrayList<LogisticsSolutionElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getSolutions().iterator().next().getSolutionElements());
		ArrayList<LSPResource> resources = new ArrayList<>(lsp.getResources());
		
		for(LSPShipment shipment : lsp.getShipments()){
			assertEquals(11, shipment.getShipmentPlan().getPlanElements().size());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			planElements.sort(new ShipmentPlanElementComparator());

			assertEquals("UNLOAD", planElements.get(10).getElementType());
			assertTrue(planElements.get(10).getEndTime() >= (0));
			assertTrue(planElements.get(10).getEndTime() <= (24*3600));
			assertTrue(planElements.get(10).getStartTime() <= planElements.get(10).getEndTime());
			assertTrue(planElements.get(10).getStartTime() >= (0));
			assertTrue(planElements.get(10).getStartTime() <= (24*3600));
			assertSame(planElements.get(10).getResourceId(), distributionAdapter.getId());
			assertSame(planElements.get(10).getSolutionElement(), distributionElement);

			assertEquals(planElements.get(10).getStartTime(), planElements.get(9).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(9).getElementType());
			assertTrue(planElements.get(9).getEndTime() >= (0));
			assertTrue(planElements.get(9).getEndTime() <= (24*3600));
			assertTrue(planElements.get(9).getStartTime() <= planElements.get(9).getEndTime());
			assertTrue(planElements.get(9).getStartTime() >= (0));
			assertTrue(planElements.get(9).getStartTime() <= (24*3600));
			assertSame(planElements.get(9).getResourceId(), distributionAdapter.getId());
			assertSame(planElements.get(9).getSolutionElement(), distributionElement);

			assertEquals(planElements.get(9).getStartTime(), planElements.get(8).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(8).getElementType());
			assertTrue(planElements.get(8).getEndTime() >= (0));
			assertTrue(planElements.get(8).getEndTime() <= (24*3600));
			assertTrue(planElements.get(8).getStartTime() <= planElements.get(8).getEndTime());
			assertTrue(planElements.get(8).getStartTime() >= (0));
			assertTrue(planElements.get(8).getStartTime() <= (24*3600));
			assertSame(planElements.get(8).getResourceId(), distributionAdapter.getId());
			assertSame(planElements.get(8).getSolutionElement(), distributionElement);
			
			assertTrue(planElements.get(8).getStartTime() >= planElements.get(7).getEndTime()/ 1.0001 + 300);

			assertEquals("HANDLE", planElements.get(7).getElementType());
			assertTrue(planElements.get(7).getEndTime() >= (0));
			assertTrue(planElements.get(7).getEndTime() <= (24*3600));
			assertTrue(planElements.get(7).getStartTime() <= planElements.get(7).getEndTime());
			assertTrue(planElements.get(7).getStartTime() >= (0));
			assertTrue(planElements.get(7).getStartTime() <= (24*3600));
			assertSame(planElements.get(7).getResourceId(), secondReloadingPointAdapter.getId());
			assertSame(planElements.get(7).getSolutionElement(), secondReloadElement);

			assertEquals(planElements.get(7).getStartTime(), planElements.get(6).getEndTime() + 300, 0.0);

			assertEquals("UNLOAD", planElements.get(6).getElementType());
			assertTrue(planElements.get(6).getEndTime() >= (0));
			assertTrue(planElements.get(6).getEndTime() <= (24*3600));
			assertTrue(planElements.get(6).getStartTime() <= planElements.get(6).getEndTime());
			assertTrue(planElements.get(6).getStartTime() >= (0));
			assertTrue(planElements.get(6).getStartTime() <= (24*3600));
			assertSame(planElements.get(6).getResourceId(), mainRunAdapter.getId());
			assertSame(planElements.get(6).getSolutionElement(), mainRunElement);

			assertEquals(planElements.get(6).getStartTime(), planElements.get(5).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(5).getElementType());
			assertTrue(planElements.get(5).getEndTime() >= (0));
			assertTrue(planElements.get(5).getEndTime() <= (24*3600));
			assertTrue(planElements.get(5).getStartTime() <= planElements.get(5).getEndTime());
			assertTrue(planElements.get(5).getStartTime() >= (0));
			assertTrue(planElements.get(5).getStartTime() <= (24*3600));
			assertSame(planElements.get(5).getResourceId(), mainRunAdapter.getId());
			assertSame(planElements.get(5).getSolutionElement(), mainRunElement);

			assertEquals(planElements.get(5).getStartTime(), planElements.get(4).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(4).getElementType());
			assertTrue(planElements.get(4).getEndTime() >= (0));
			assertTrue(planElements.get(4).getEndTime() <= (24*3600));
			assertTrue(planElements.get(4).getStartTime() <= planElements.get(4).getEndTime());
			assertTrue(planElements.get(4).getStartTime() >= (0));
			assertTrue(planElements.get(4).getStartTime() <= (24*3600));
			assertSame(planElements.get(4).getResourceId(), mainRunAdapter.getId());
			assertSame(planElements.get(4).getSolutionElement(), mainRunElement);
			
			assertTrue(planElements.get(4).getStartTime() >= planElements.get(3).getEndTime() / (1.0001) + 300);

			assertEquals("HANDLE", planElements.get(3).getElementType());
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));
			assertSame(planElements.get(3).getResourceId(), firstReloadingPointAdapter.getId());
			assertSame(planElements.get(3).getSolutionElement(), firstReloadElement);

			assertEquals(planElements.get(3).getStartTime(), planElements.get(2).getEndTime() + 300, 0.0);

			assertEquals("UNLOAD", planElements.get(2).getElementType());
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));
			assertSame(planElements.get(2).getResourceId(), collectionAdapter.getId());
			assertSame(planElements.get(2).getSolutionElement(), collectionElement);

			assertEquals(planElements.get(2).getStartTime(), planElements.get(1).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(1).getElementType());
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));
			assertSame(planElements.get(1).getResourceId(), collectionAdapter.getId());
			assertSame(planElements.get(1).getSolutionElement(), collectionElement);

			assertEquals(planElements.get(1).getStartTime(), planElements.get(0).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(0).getElementType());
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.get(0).getStartTime() <= (24*3600));
			assertSame(planElements.get(0).getResourceId(), collectionAdapter.getId());
			assertSame(planElements.get(0).getSolutionElement(), collectionElement);
		}

		assertEquals(1, firstReloadingPointAdapter.getEventHandlers().size());
		ArrayList<EventHandler> eventHandlers = new ArrayList<>(firstReloadingPointAdapter.getEventHandlers());
		assertTrue(eventHandlers.iterator().next() instanceof ReloadingPointTourEndEventHandler);
		ReloadingPointTourEndEventHandler reloadEventHandler = (ReloadingPointTourEndEventHandler) eventHandlers.iterator().next();
		Iterator<Entry<CarrierService, ReloadingPointTourEndEventHandler.ReloadingPointEventHandlerPair>>  iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<CarrierService, ReloadingPointTourEndEventHandler.ReloadingPointEventHandlerPair> entry =  iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticsSolutionElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), shipment.getFrom());
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
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

		assertEquals(1, secondReloadingPointAdapter.getEventHandlers().size());
		eventHandlers = new ArrayList<>(secondReloadingPointAdapter.getEventHandlers());
		assertTrue(eventHandlers.iterator().next() instanceof ReloadingPointTourEndEventHandler);
		reloadEventHandler = (ReloadingPointTourEndEventHandler) eventHandlers.iterator().next();
		iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<CarrierService, ReloadingPointTourEndEventHandler.ReloadingPointEventHandlerPair> entry =  iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticsSolutionElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), toLinkId);
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
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
			assertEquals(6, shipment.getEventHandlers().size());
			eventHandlers = new ArrayList<>(shipment.getEventHandlers());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			planElements.sort(new ShipmentPlanElementComparator());
				
			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler endHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertSame(endHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(endHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(endHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(endHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(endHandler.getElement(), planElements.get(0).getSolutionElement());
			assertSame(endHandler.getElement(), planElements.get(1).getSolutionElement());
			assertSame(endHandler.getElement(), planElements.get(2).getSolutionElement());
			assertSame(endHandler.getElement(), solutionElements.get(0));
			assertSame(endHandler.getLspShipment(), shipment);
			assertSame(endHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(endHandler.getResourceId(), resources.get(0).getId());
			
			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEndEventHandler);
			CollectionServiceEndEventHandler serviceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertSame(serviceHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(serviceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(serviceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(serviceHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(serviceHandler.getElement(), planElements.get(0).getSolutionElement());
			assertSame(serviceHandler.getElement(), planElements.get(1).getSolutionElement());
			assertSame(serviceHandler.getElement(), planElements.get(2).getSolutionElement());
			assertSame(serviceHandler.getElement(), solutionElements.get(0));
			assertSame(serviceHandler.getLspShipment(), shipment);
			assertSame(serviceHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(serviceHandler.getResourceId(), resources.get(0).getId());
			
			assertTrue(eventHandlers.get(2) instanceof MainRunTourStartEventHandler);
			MainRunTourStartEventHandler mainRunStartHandler = (MainRunTourStartEventHandler) eventHandlers.get(2);
			assertSame(mainRunStartHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunStartHandler.getSolutionElement(), planElements.get(4).getSolutionElement());
			assertSame(mainRunStartHandler.getSolutionElement(), planElements.get(5).getSolutionElement());
			assertSame(mainRunStartHandler.getSolutionElement(), planElements.get(6).getSolutionElement());
			assertSame(mainRunStartHandler.getSolutionElement(), solutionElements.get(2));
			assertSame(mainRunStartHandler.getLspShipment(), shipment);
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(4).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(5).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(6).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), resources.get(2).getId());
			
			assertTrue(eventHandlers.get(3) instanceof MainRunTourEndEventHandler);
			MainRunTourEndEventHandler mainRunEndHandler = (MainRunTourEndEventHandler) eventHandlers.get(3);
			assertSame(mainRunEndHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunEndHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunEndHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunEndHandler.getSolutionElement(), planElements.get(4).getSolutionElement());
			assertSame(mainRunEndHandler.getSolutionElement(), planElements.get(5).getSolutionElement());
			assertSame(mainRunEndHandler.getSolutionElement(), planElements.get(6).getSolutionElement());
			assertSame(mainRunEndHandler.getSolutionElement(), solutionElements.get(2));
			assertSame(mainRunEndHandler.getLspShipment(), shipment);
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(4).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(5).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(6).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), resources.get(2).getId());
			
			assertTrue(eventHandlers.get(4) instanceof DistributionTourStartEventHandler);
			DistributionTourStartEventHandler distributionStartHandler = (DistributionTourStartEventHandler) eventHandlers.get(4);
			assertSame(distributionStartHandler.getCarrierService().getLocationLinkId(), shipment.getTo());
			assertEquals(distributionStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(distributionStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, distributionStartHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, distributionStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(distributionStartHandler.getElement(), planElements.get(8).getSolutionElement());
			assertSame(distributionStartHandler.getElement(), planElements.get(9).getSolutionElement());
			assertSame(distributionStartHandler.getElement(), planElements.get(10).getSolutionElement());
			assertSame(distributionStartHandler.getElement(), solutionElements.get(4));
			assertSame(distributionStartHandler.getLspShipment(), shipment);
			assertSame(distributionStartHandler.getResource().getId(), planElements.get(8).getResourceId());
			assertSame(distributionStartHandler.getResource().getId(), planElements.get(9).getResourceId());
			assertSame(distributionStartHandler.getResource().getId(), planElements.get(10).getResourceId());
			assertSame(distributionStartHandler.getResource().getId(), resources.get(4).getId());
			
			assertTrue(eventHandlers.get(5) instanceof DistributionServiceStartEventHandler);
			DistributionServiceStartEventHandler distributionServiceHandler = (DistributionServiceStartEventHandler) eventHandlers.get(5);
			assertSame(distributionServiceHandler.getCarrierService().getLocationLinkId(), shipment.getTo());
			assertEquals(distributionServiceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(distributionServiceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, distributionServiceHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, distributionServiceHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(distributionServiceHandler.getSolutionElement(), planElements.get(8).getSolutionElement());
			assertSame(distributionServiceHandler.getSolutionElement(), planElements.get(9).getSolutionElement());
			assertSame(distributionServiceHandler.getSolutionElement(), planElements.get(10).getSolutionElement());
			assertSame(distributionServiceHandler.getSolutionElement(), solutionElements.get(4));
			assertSame(distributionServiceHandler.getLspShipment(), shipment);
			assertSame(distributionServiceHandler.getResource().getId(), planElements.get(8).getResourceId());
			assertSame(distributionServiceHandler.getResource().getId(), planElements.get(9).getResourceId());
			assertSame(distributionServiceHandler.getResource().getId(), planElements.get(10).getResourceId());
			assertSame(distributionServiceHandler.getResource().getId(), resources.get(4).getId());
			
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
