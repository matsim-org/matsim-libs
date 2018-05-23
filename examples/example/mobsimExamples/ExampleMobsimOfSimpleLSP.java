package example.mobsimExamples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

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
import lsp.controler.LSPModule;
import lsp.events.EventUtils;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.resources.Resource;
import lsp.scoring.LSPScoringModuleImpl;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.AbstractShipmentPlanElementComparator;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.SimpleForwardSolutionScheduler;

public class ExampleMobsimOfSimpleLSP {

public static LSP createInitialLSP(Network network) {
		
		//The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
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
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		CollectionCarrierScheduler scheduler = new CollectionCarrierScheduler();
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		Resource collectionAdapter = adapterBuilder.build();
		
		//The adapter is now inserted into the only LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();
		
		//The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder collectionSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();
		
		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlanImpl collectionPlan = new LSPPlanImpl();
		ShipmentAssigner assigner = new DeterministicShipmentAssigner();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);
		
		LSPImpl.Builder collectionLSPBuilder = LSPImpl.Builder.getInstance();
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
	
	public static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<LSPShipment>();
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Link> toLinkId = collectionLinkId;
		
		//Create five LSPShipments that are located in the left half of the network.
		for(int i = 1; i < 6; i++) {
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
	        	shipmentList.add(builder.build());
		 } 	
	    return shipmentList;
	}
	
	
	
	public static void main (String[]args) {
		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("input/lsp/network/2regions.xml");
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
	
		//set up simulation controler and LSPModule
		ArrayList<LSP> lspList = new ArrayList<LSP>();
		lspList.add(lsp);
		LSPs lsps = new LSPs(lspList);
		LSPModule module = new LSPModule(lsps, new LSPReplanningModuleImpl(lsps), new LSPScoringModuleImpl(lsps), EventUtils.getStandardEventCreators());
		
		Controler controler = new Controler(config);
		controler.addOverridingModule(module);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("input/lsp/network/2regions.xml");
		controler.run();
	
		for(LSPShipment shipment : lsp.getShipments()) {
			System.out.println("Shipment: " + shipment.getId());
			ArrayList<AbstractShipmentPlanElement> scheduleElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(scheduleElements, new AbstractShipmentPlanElementComparator());
			ArrayList<AbstractShipmentPlanElement> logElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getLog().getPlanElements().values());
			Collections.sort(logElements, new AbstractShipmentPlanElementComparator());
			
			for(int i = 0; i < shipment.getSchedule().getPlanElements().size(); i++) {
				System.out.println("Scheduled: " + scheduleElements.get(i).getSolutionElement().getId() + "  " + scheduleElements.get(i).getResourceId()+ "  " + scheduleElements.get(i).getElementType() + " Start: " + scheduleElements.get(i).getStartTime() + " End: " + scheduleElements.get(i).getEndTime());
			}
			System.out.println();
			for(int i = 0; i < shipment.getLog().getPlanElements().size(); i++) {
				System.out.println("Logged: " + logElements.get(i).getSolutionElement().getId() + "  " + logElements.get(i).getResourceId() +"  " + logElements.get(i).getElementType() + " Start: " + logElements.get(i).getStartTime() + " End: " + logElements.get(i).getEndTime());
			}
			System.out.println();
		}
		
	
	}
	
	
}
