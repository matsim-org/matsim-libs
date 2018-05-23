package example.requirementsChecking;

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
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.LSP;
import lsp.LSPImpl;
import lsp.LSPPlan;
import lsp.LSPPlanImpl;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.LogisticsSolutionImpl;
import lsp.ShipmentAssigner;
import lsp.SolutionScheduler;
import lsp.resources.Resource;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
import requirementsCheckerTests.BlueInfo;
import requirementsCheckerTests.BlueRequirement;
import requirementsCheckerTests.RedInfo;
import requirementsCheckerTests.RedRequirement;
import requirementsCheckerTests.RequirementsAssigner;

public class ExampleCheckRequirementsOfAssigner {

	public static LSP createLSPWithProperties(Network network) {
		
		//Create red LogisticsSolution which has the corresponding info
		CollectionCarrierScheduler redScheduler = new CollectionCarrierScheduler();
		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("RedCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
		CarrierVehicle redVehicle = CarrierVehicle.newInstance(redVehicleId, collectionLinkId);
		redVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder redCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		redCapabilitiesBuilder.addType(collectionType);
		redCapabilitiesBuilder.addVehicle(redVehicle);
		redCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities redCapabilities = redCapabilitiesBuilder.build();
		Carrier redCarrier = CarrierImpl.newInstance(redCarrierId);
		redCarrier.setCarrierCapabilities(redCapabilities);
				
		Id<Resource> redAdapterId = Id.create("RedCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder redAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(redAdapterId, network);
		redAdapterBuilder.setCollectionScheduler(redScheduler);
		redAdapterBuilder.setCarrier(redCarrier);
		redAdapterBuilder.setLocationLinkId(collectionLinkId);
		Resource redAdapter = redAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> redElementId = Id.create("RedElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder redElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(redElementId);
		redElementBuilder.setResource(redAdapter);
		LogisticsSolutionElement redElement = redElementBuilder.build();
		
		Id<LogisticsSolution> redSolutionId = Id.create("RedSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder redSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(redSolutionId);
		redSolutionBuilder.addSolutionElement(redElement);
		LogisticsSolution redSolution = redSolutionBuilder.build();
		
		//Add info that shows the world the color of the solution
		redSolution.getInfos().add(new RedInfo());
		
		
		//Create blue LogisticsSolution which has the corresponding info
		CollectionCarrierScheduler blueScheduler = new CollectionCarrierScheduler();
		Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
		Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId);
		blueVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder blueCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		blueCapabilitiesBuilder.addType(collectionType);
		blueCapabilitiesBuilder.addVehicle(blueVehicle);
		blueCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities blueCapabilities = blueCapabilitiesBuilder.build();
		Carrier blueCarrier = CarrierImpl.newInstance(blueCarrierId);
		blueCarrier.setCarrierCapabilities(blueCapabilities);
				
		Id<Resource> blueAdapterId = Id.create("BlueCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder blueAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(blueAdapterId, network);
		blueAdapterBuilder.setCollectionScheduler(blueScheduler);
		blueAdapterBuilder.setCarrier(blueCarrier);
		blueAdapterBuilder.setLocationLinkId(collectionLinkId);
		Resource blueAdapter = blueAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> blueElementId = Id.create("BlueCElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder blueElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(blueElementId);
		blueElementBuilder.setResource(blueAdapter);
		LogisticsSolutionElement blueElement = blueElementBuilder.build();
		
		Id<LogisticsSolution> blueSolutionId = Id.create("BlueSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder blueSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(blueSolutionId);
		blueSolutionBuilder.addSolutionElement(blueElement);
		LogisticsSolution blueSolution = blueSolutionBuilder.build();
		
		//Add info that shows the world the color of the solution
		blueSolution.getInfos().add(new BlueInfo());
				
		//Create the initial plan, add assigner that checks requirements of the shipments when assigning and add both solutions (red and blue) to the 
		//plan.
		LSPPlan plan = new LSPPlanImpl();
		ShipmentAssigner assigner = new RequirementsAssigner();
		plan.setAssigner(assigner);
		plan.addSolution(redSolution);
		plan.addSolution(blueSolution);
		
		LSPImpl.Builder lspBuilder = LSPImpl.Builder.getInstance();
		lspBuilder.setInitialPlan(plan);
		Id<LSP> lspId = Id.create("CollectionLSP", LSP.class);
		lspBuilder.setId(lspId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(redAdapter);
		resourcesList.add(blueAdapter);
			
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		lspBuilder.setSolutionScheduler(simpleScheduler);
		return lspBuilder.build();
	}
	
	public static Collection<LSPShipment> createShipmentsWithRequirements(Network network){
		//Create ten shipments with either a red or blue requirement, i.e. that they only can be transported in a solution with the matching color
		ArrayList<LSPShipment> shipmentList = new ArrayList<LSPShipment>();
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Link> toLinkId = collectionLinkId;
	
	    Random rand = new Random(1); 
	    
	    for(int i = 1; i < 11; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
        	int capacityDemand = rand.nextInt(10);
        	builder.setCapacityDemand(capacityDemand);
        	
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
        	
        	builder.setToLinkId(toLinkId);
        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setEndTimeWindow(endTimeWindow);
        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setStartTimeWindow(startTimeWindow);
        	builder.setServiceTime(capacityDemand * 60);
        	boolean blue = rand.nextBoolean();
        	if (blue == true) {
        		builder.addRequirement(new BlueRequirement());
        	}
        	else {
        		builder.addRequirement(new RedRequirement());
        	}
        	
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
        LSP lsp = createLSPWithProperties(network);
        Collection<LSPShipment> shipments =  createShipmentsWithRequirements(network);
	
        //assign the shipments to the LSP
        for(LSPShipment shipment : shipments) {
        	lsp.assignShipmentToLSP(shipment);
        }
        
        for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
        	if(solution.getId().toString() == "RedSolution") {
        		for(LSPShipment shipment : solution.getShipments()) {
        			if(!(shipment.getRequirements().iterator().next() instanceof RedRequirement)) {
        				break;
        			}
        		}
        		System.out.println("All shipments in " + solution.getId() + " are red");
        	}
        	if(solution.getId().toString() == "BlueSolution") {
        		for(LSPShipment shipment : solution.getShipments()) {
        			if(!(shipment.getRequirements().iterator().next() instanceof BlueRequirement)) {
        				break;
        			}
        		}
        		System.out.println("All shipments in " + solution.getId() + " are blue");
        	}
        }
       
	}
	
}
