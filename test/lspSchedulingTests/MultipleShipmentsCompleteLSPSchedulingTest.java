package lspSchedulingTests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;

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
import lsp.usecase.DistributionCarrierAdapter;
import lsp.usecase.DistributionCarrierScheduler;
import lsp.usecase.DistributionServiceEventHandler;
import lsp.usecase.DistributionStartEventHandler;
import lsp.usecase.MainRunCarrierAdapter;
import lsp.usecase.MainRunCarrierScheduler;
import lsp.usecase.MainRunEndEventHandler;
import lsp.usecase.MainRunStartEventHandler;
import lsp.usecase.ReloadingPoint;
import lsp.usecase.ReloadingPointEventHandler;
import lsp.usecase.ReloadingPointScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
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

public class MultipleShipmentsCompleteLSPSchedulingTest {
	
	private Network network;
	private LogisticsSolution completeSolution;
	private ShipmentAssigner assigner;
	private LSPPlanImpl completePlan;
	private SolutionScheduler simpleScheduler;
	private LSP lsp;	
	private Resource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	private Resource firstReloadingPointAdapter;
	private LogisticsSolutionElement firstReloadElement;
	private Resource mainRunAdapter;
	private LogisticsSolutionElement mainRunElement;
	private Resource secondReloadingPointAdapter;
	private LogisticsSolutionElement secondReloadElement;
	private Resource distributionAdapter;
	private LogisticsSolutionElement distributionElement;
	private Id<Link> toLinkId;
	
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
		
		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType mainRunType = mainRunVehicleTypeBuilder.build();
				
		
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
        
        
        
        MainRunCarrierScheduler mainRunScheduler = new MainRunCarrierScheduler();
        Id<Resource> mainRunId = Id.create("MainRunAdapter", Resource.class);
        MainRunCarrierAdapter.Builder mainRunAdapterBuilder = MainRunCarrierAdapter.Builder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setMainRunCarrierScheduler(mainRunScheduler);
        mainRunAdapterBuilder.setFromLinkId(fromLinkId);
        mainRunAdapterBuilder.setToLinkId(toLinkId);
        mainRunAdapterBuilder.setCarrier(mainRunCarrier);
        mainRunAdapter = mainRunAdapterBuilder.build();
	
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder mainRunBuilder = LogisticsSolutionElementImpl.Builder.newInstance(mainRunElementId);
		mainRunBuilder.setResource(mainRunAdapter);
		mainRunElement = mainRunBuilder.build();
		
		ReloadingPointScheduler.Builder secondSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        secondSchedulerBuilder.setCapacityNeedFixed(10);
        secondSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<Resource> secondReloadingId = Id.create("ReloadingPoint2", Resource.class);
        Id<Link> secondReloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        ReloadingPoint.Builder secondReloadingPointBuilder = ReloadingPoint.Builder.newInstance(secondReloadingId, secondReloadingLinkId);
        secondReloadingPointBuilder.setReloadingScheduler(secondSchedulerBuilder.build());
        secondReloadingPointAdapter = secondReloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> secondReloadingElementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder secondReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(secondReloadingElementId);
		secondReloadingElementBuilder.setResource(secondReloadingPointAdapter);
		secondReloadElement = secondReloadingElementBuilder.build();
		
		DistributionCarrierScheduler distriutionScheduler = new DistributionCarrierScheduler();
		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType distributionType = dsitributionVehicleTypeBuilder.build();
		
		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId);
		distributionCarrierVehicle.setVehicleType(distributionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierImpl.newInstance(distributionCarrierId);
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);
		
		
		Id<Resource> distributionAdapterId = Id.create("DistributionCarrierAdapter", Resource.class);
		DistributionCarrierAdapter.Builder distributionAdapterBuilder = DistributionCarrierAdapter.Builder.newInstance(distributionAdapterId, network);
		distributionAdapterBuilder.setDistributionScheduler(distriutionScheduler);
		distributionAdapterBuilder.setCarrier(distributionCarrier);
		distributionAdapterBuilder.setLocationLinkId(distributionLinkId);
		distributionAdapter = distributionAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> distributionElementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder distributionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionAdapter);
		distributionElement =    distributionBuilder.build();
		
		collectionElement.setNextElement(firstReloadElement);
		firstReloadElement.setPreviousElement(collectionElement);
		firstReloadElement.setNextElement(mainRunElement);
		mainRunElement.setPreviousElement(firstReloadElement);
		mainRunElement.setNextElement(secondReloadElement);
		secondReloadElement.setPreviousElement(mainRunElement);
		secondReloadElement.setNextElement(distributionElement);
		distributionElement.setPreviousElement(secondReloadElement);
		
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder completeSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(solutionId);
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolutionBuilder.addSolutionElement(distributionElement);
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
		resourcesList.add(mainRunAdapter);
		resourcesList.add(secondReloadingPointAdapter);
		resourcesList.add(distributionAdapter);


		simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Random rand = new Random(1);
		 for(int i = 1; i < 100; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
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
	        	builder.setServiceTime(capacityDemand * 60);
	        	LSPShipment shipment = builder.build();
	        	lsp.assignShipmentToLSP(shipment);
	        }
		lsp.scheduleSoultions();
	
	}

	@Test
	public void testCompletedLSPScheduling() {
		
		for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<AbstractShipmentPlanElement> elementList = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(elementList, new AbstractShipmentPlanElementComparator());
			System.out.println();
			for(AbstractShipmentPlanElement element : elementList) {
				System.out.println(element.getSolutionElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());	
			}			
			System.out.println();
		}
	
		ArrayList<LogisticsSolutionElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getSolutions().iterator().next().getSolutionElements());
		ArrayList<Resource> resources = new ArrayList<>(lsp.getResources());
		
		for(LSPShipment shipment : lsp.getShipments()){
			assertTrue(shipment.getSchedule().getPlanElements().size() == 11);
			ArrayList<AbstractShipmentPlanElement> planElements = new ArrayList<>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(planElements, new AbstractShipmentPlanElementComparator());
			
			assertTrue(planElements.get(10).getElementType() == "UNLOAD");
			assertTrue(planElements.get(10).getEndTime() >= (0));
			assertTrue(planElements.get(10).getEndTime() <= (24*3600));
			assertTrue(planElements.get(10).getStartTime() <= planElements.get(10).getEndTime());
			assertTrue(planElements.get(10).getStartTime() >= (0));
			assertTrue(planElements.get(10).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(10).getResourceId() == distributionAdapter.getId());
			assertTrue(planElements.get(10).getSolutionElement() == distributionElement);	
									
			assertTrue(planElements.get(10).getStartTime() == planElements.get(9).getEndTime());
				
			assertTrue(planElements.get(9).getElementType() == "TRANSPORT");
			assertTrue(planElements.get(9).getEndTime() >= (0));
			assertTrue(planElements.get(9).getEndTime() <= (24*3600));
			assertTrue(planElements.get(9).getStartTime() <= planElements.get(9).getEndTime());
			assertTrue(planElements.get(9).getStartTime() >= (0));
			assertTrue(planElements.get(9).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(9).getResourceId() == distributionAdapter.getId());
			assertTrue(planElements.get(9).getSolutionElement() == distributionElement);	
			
			assertTrue(planElements.get(9).getStartTime() == planElements.get(8).getEndTime());
			
			assertTrue(planElements.get(8).getElementType() == "LOAD");
			assertTrue(planElements.get(8).getEndTime() >= (0));
			assertTrue(planElements.get(8).getEndTime() <= (24*3600));
			assertTrue(planElements.get(8).getStartTime() <= planElements.get(8).getEndTime());
			assertTrue(planElements.get(8).getStartTime() >= (0));
			assertTrue(planElements.get(8).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(8).getResourceId() == distributionAdapter.getId());
			assertTrue(planElements.get(8).getSolutionElement() == distributionElement);	
			
			assertTrue(planElements.get(8).getStartTime() >= planElements.get(7).getEndTime()/ 1.0001 + 300);
			
			assertTrue(planElements.get(7).getElementType() == "HANDLE");
			assertTrue(planElements.get(7).getEndTime() >= (0));
			assertTrue(planElements.get(7).getEndTime() <= (24*3600));
			assertTrue(planElements.get(7).getStartTime() <= planElements.get(7).getEndTime());
			assertTrue(planElements.get(7).getStartTime() >= (0));
			assertTrue(planElements.get(7).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(7).getResourceId() == secondReloadingPointAdapter.getId());
			assertTrue(planElements.get(7).getSolutionElement() == secondReloadElement);	
			
			assertTrue(planElements.get(7).getStartTime() == planElements.get(6).getEndTime() + 300);
			
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
			
			assertTrue(planElements.get(4).getStartTime() >= planElements.get(3).getEndTime() / (1.0001) + 300);
			
			assertTrue(planElements.get(3).getElementType() == "HANDLE");
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(3).getResourceId() == firstReloadingPointAdapter.getId());
			assertTrue(planElements.get(3).getSolutionElement() == firstReloadElement);	
			
			assertTrue(planElements.get(3).getStartTime() == planElements.get(2).getEndTime() + 300);
			
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
			
			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));	
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));	
		}
		
		assertTrue(secondReloadingPointAdapter.getEventHandlers().size() ==1);
		eventHandlers = new ArrayList<EventHandler>(secondReloadingPointAdapter.getEventHandlers());
		assertTrue(eventHandlers.iterator().next() instanceof ReloadingPointEventHandler);
		reloadEventHandler = (ReloadingPointEventHandler) eventHandlers.iterator().next();
		iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<CarrierService,ReloadingPointEventHandler.ReloadingPointEventHandlerPair> entry =  iter.next();
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
			assertTrue(shipment.getEventHandlers().size() == 6);
			eventHandlers = new ArrayList<EventHandler>(shipment.getEventHandlers());
			ArrayList<AbstractShipmentPlanElement> planElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(planElements, new AbstractShipmentPlanElementComparator());
				
			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler endHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertTrue(endHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(endHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(endHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(endHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(endHandler.getElement() == planElements.get(0).getSolutionElement());
			assertTrue(endHandler.getElement() == planElements.get(1).getSolutionElement());
			assertTrue(endHandler.getElement() == planElements.get(2).getSolutionElement());
			assertTrue(endHandler.getElement() == solutionElements.get(0));
			assertTrue(endHandler.getLspShipment() == shipment);
			assertTrue(endHandler.getResourceId() == planElements.get(0).getResourceId());
			assertTrue(endHandler.getResourceId() == planElements.get(1).getResourceId());
			assertTrue(endHandler.getResourceId() == planElements.get(2).getResourceId());
			assertTrue(endHandler.getResourceId()  == resources.get(0).getId());
			
			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEventHandler);
			CollectionServiceEventHandler serviceHandler = (CollectionServiceEventHandler) eventHandlers.get(1);
			assertTrue(serviceHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(serviceHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(serviceHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(serviceHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(serviceHandler.getElement() == planElements.get(0).getSolutionElement());
			assertTrue(serviceHandler.getElement() == planElements.get(1).getSolutionElement());
			assertTrue(serviceHandler.getElement() == planElements.get(2).getSolutionElement());
			assertTrue(serviceHandler.getElement() == solutionElements.get(0));
			assertTrue(serviceHandler.getLspShipment() == shipment);
			assertTrue(serviceHandler.getResourceId() == planElements.get(0).getResourceId());
			assertTrue(serviceHandler.getResourceId() == planElements.get(1).getResourceId());
			assertTrue(serviceHandler.getResourceId() == planElements.get(2).getResourceId());
			assertTrue(serviceHandler.getResourceId()  == resources.get(0).getId());
			
			assertTrue(eventHandlers.get(2) instanceof MainRunStartEventHandler);
			MainRunStartEventHandler mainRunStartHandler = (MainRunStartEventHandler) eventHandlers.get(2);
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
			
			assertTrue(eventHandlers.get(3) instanceof MainRunEndEventHandler);
			MainRunEndEventHandler mainRunEndHandler = (MainRunEndEventHandler) eventHandlers.get(3);
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
			
			assertTrue(eventHandlers.get(4) instanceof DistributionStartEventHandler);
			DistributionStartEventHandler distributionStartHandler = (DistributionStartEventHandler) eventHandlers.get(4);
			assertTrue(distributionStartHandler.getCarrierService().getLocationLinkId() == shipment.getToLinkId());
			assertTrue(distributionStartHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(distributionStartHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(distributionStartHandler.getCarrierService().getServiceStartTimeWindow().getStart() == 0);
			assertTrue(distributionStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd() == Integer.MAX_VALUE);
			assertTrue(distributionStartHandler.getElement() == planElements.get(8).getSolutionElement());
			assertTrue(distributionStartHandler.getElement() == planElements.get(9).getSolutionElement());
			assertTrue(distributionStartHandler.getElement() == planElements.get(10).getSolutionElement());
			assertTrue(distributionStartHandler.getElement() == solutionElements.get(4));
			assertTrue(distributionStartHandler.getLspShipment() == shipment);
			assertTrue(distributionStartHandler.getResource().getId() == planElements.get(8).getResourceId());
			assertTrue(distributionStartHandler.getResource().getId() == planElements.get(9).getResourceId());
			assertTrue(distributionStartHandler.getResource().getId() == planElements.get(10).getResourceId());
			assertTrue(distributionStartHandler.getResource().getId()  == resources.get(4).getId());
			
			assertTrue(eventHandlers.get(5) instanceof DistributionServiceEventHandler);
			DistributionServiceEventHandler distributionServiceHandler = (DistributionServiceEventHandler) eventHandlers.get(5);
			assertTrue(distributionServiceHandler.getCarrierService().getLocationLinkId() == shipment.getToLinkId());
			assertTrue(distributionServiceHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(distributionServiceHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(distributionServiceHandler.getCarrierService().getServiceStartTimeWindow().getStart() == 0);
			assertTrue(distributionServiceHandler.getCarrierService().getServiceStartTimeWindow().getEnd() == Integer.MAX_VALUE);
			assertTrue(distributionServiceHandler.getSolutionElement() == planElements.get(8).getSolutionElement());
			assertTrue(distributionServiceHandler.getSolutionElement() == planElements.get(9).getSolutionElement());
			assertTrue(distributionServiceHandler.getSolutionElement() == planElements.get(10).getSolutionElement());
			assertTrue(distributionServiceHandler.getSolutionElement() == solutionElements.get(4));
			assertTrue(distributionServiceHandler.getLspShipment() == shipment);
			assertTrue(distributionServiceHandler.getResource().getId() == planElements.get(8).getResourceId());
			assertTrue(distributionServiceHandler.getResource().getId() == planElements.get(9).getResourceId());
			assertTrue(distributionServiceHandler.getResource().getId() == planElements.get(10).getResourceId());
			assertTrue(distributionServiceHandler.getResource().getId()  == resources.get(4).getId());
			
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
