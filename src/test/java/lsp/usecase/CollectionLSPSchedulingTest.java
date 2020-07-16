package lsp.usecase;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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

public class CollectionLSPSchedulingTest {
	
	private Network network;
	private LSP collectionLSP;	
	private Carrier carrier;
	private LSPResource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	
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
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId);
		carrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder adapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = adapterBuilder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionElementBuilder.setResource(collectionAdapter);
		collectionElement = collectionElementBuilder.build();
		
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(collectionSolutionId );
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();
		
		ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);
	
		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		ArrayList<LSPResource> resourcesList = new ArrayList<LSPResource>();
		resourcesList.add(collectionAdapter);
		
		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
	    Id<Link> toLinkId = collectionLinkId;
	
	        
	    for(int i = 1; i < 2; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
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
		
	}
	
	@Test
	public void testCollectionLSPScheduling() {
		
		for(LSPShipment shipment : collectionLSP.getShipments()) {
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<ShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(scheduleElements, new ShipmentPlanElementComparator());
			
			System.out.println();
			for(int i = 0; i < shipment.getSchedule().getPlanElements().size(); i++) {
				System.out.println("Scheduled: " + scheduleElements.get(i).getSolutionElement().getId() + "  " + scheduleElements.get(i).getResourceId() +"  "+ scheduleElements.get(i).getElementType() + " Start: " + scheduleElements.get(i).getStartTime() + " End: " + scheduleElements.get(i).getEndTime());
			}
			System.out.println();	
		}
				
		for(LSPShipment shipment : collectionLSP.getShipments()) {
			assertTrue(shipment.getSchedule().getPlanElements().size() == 3);
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<ShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			assertTrue(planElements.get(2).getElementType() == "UNLOAD");
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(2).getResourceId() == collectionAdapter.getId());
			assertTrue(planElements.get(2).getSolutionElement() == collectionElement);	
			assertTrue(planElements.get(1).getElementType() == "TRANSPORT");
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() == planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(1).getResourceId() == collectionAdapter.getId());
			assertTrue(planElements.get(1).getSolutionElement() == collectionElement);
			assertTrue(planElements.get(0).getElementType() == "LOAD");
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertTrue(planElements.get(1).getStartTime() == planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.get(0).getStartTime() <= (24*3600));	
			assertTrue(planElements.get(0).getResourceId() == collectionAdapter.getId());
			assertTrue(planElements.get(0).getSolutionElement() == collectionElement);
			
			assertTrue(shipment.getEventHandlers().size() == 2);
			ArrayList<EventHandler> eventHandlers = new ArrayList<EventHandler>(shipment.getEventHandlers());
			
			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler endHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertTrue(endHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(endHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(endHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(endHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(endHandler.getElement() == planElements.get(2).getSolutionElement());
			assertTrue(endHandler.getElement() == collectionLSP.getSelectedPlan().getSolutions().iterator().next().getSolutionElements().iterator().next());
			assertTrue(endHandler.getLspShipment() == shipment);
			assertTrue(endHandler.getResourceId() == planElements.get(2).getResourceId());
			assertTrue(endHandler.getResourceId()  == collectionLSP.getResources().iterator().next().getId());
		
			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEndEventHandler);
			CollectionServiceEndEventHandler serviceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertTrue(serviceHandler.getCarrierService().getLocationLinkId() == shipment.getFromLinkId());
			assertTrue(serviceHandler.getCarrierService().getCapacityDemand() == shipment.getCapacityDemand());
			assertTrue(serviceHandler.getCarrierService().getServiceDuration() == shipment.getServiceTime());
			assertTrue(serviceHandler.getCarrierService().getServiceStartTimeWindow() == shipment.getStartTimeWindow());
			assertTrue(serviceHandler.getElement() == planElements.get(1).getSolutionElement());
			assertTrue(serviceHandler.getElement() == collectionLSP.getSelectedPlan().getSolutions().iterator().next().getSolutionElements().iterator().next());
			assertTrue(serviceHandler.getLspShipment() == shipment);
			assertTrue(serviceHandler.getResourceId() == planElements.get(1).getResourceId());
			assertTrue(serviceHandler.getResourceId()  == collectionLSP.getResources().iterator().next().getId());
		}		
	
		for(LogisticsSolution solution : collectionLSP.getSelectedPlan().getSolutions()) {
			assertTrue(solution.getShipments().size() == 1);
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
