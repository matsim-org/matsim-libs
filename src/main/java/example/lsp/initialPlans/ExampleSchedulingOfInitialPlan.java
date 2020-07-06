package example.lsp.initialPlans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import lsp.*;
import lsp.shipment.*;
import lsp.usecase.*;
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
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.resources.Resource;

/*package-private*/ class ExampleSchedulingOfInitialPlan {
	
	private static LSP createInitialLSP(Network network) {
		
		//The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
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
		
		Carrier carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		//The Adapter i.e. the Resource is created
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder adapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		Resource collectionAdapter = adapterBuilder.build();
		
		//The adapter is now inserted into the only LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();
		
		//The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(collectionSolutionId );
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();
		
		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = new DeterministicShipmentAssigner();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);
		
		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		
		//The exogenous list of Resoruces for the SolutuionScheduler is compiled and the Scheduler is added to the LSPBuilder 
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		
		return collectionLSPBuilder.build();
		
	}
	
	private static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<LSPShipment>();
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Link> toLinkId = collectionLinkId;
		
		//Create five LSPShipments that are located in the left half of the network.
		for(int i = 1; i < 6; i++) {
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
	        	shipmentList.add(builder.build());
		 } 	
	    return shipmentList;
	}
	
	
	public static void main(String [] args) {
		
		//Set up required MATSim classes
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        Network network = scenario.getNetwork();
        
        //Create LSP and shipments
        LSP lsp = createInitialLSP(network);
        Collection<LSPShipment> shipments =  createInitialLSPShipments(network);
        
        //assign the shipments to the LSP
        for(LSPShipment shipment : shipments) {
        	lsp.assignShipmentToLSP(shipment);
        }
        
        //schedule the LSP with the shipments and according to the scheduler of the Resource
        lsp.scheduleSoultions();
        
        //print the schedules for the assigned LSPShipments
        for(LSPShipment shipment : shipments) {
        	System.out.println("Shipment: " + shipment.getId());
        	ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<ShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(scheduleElements, new ShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<ShipmentPlanElement>(shipment.getLog().getPlanElements().values());
			Collections.sort(logElements, new ShipmentPlanElementComparator());
        	
			for(ShipmentPlanElement element : shipment.getSchedule().getPlanElements().values()) {
        		System.out.println("Solution Id: " + element.getSolutionElement().getLogisticsSolution().getId() 
        		+ " SolutionElement Id: " + element.getSolutionElement().getId()
        		+ " Resource Id: " + element.getResourceId()
        		+ " Type: " + element.getElementType()
        		+ " Start Time: " + element.getStartTime()
        		+ " End Time: " + element.getEndTime());	
        	}
        	System.out.println();
        }
 	}	
}
