package example.lspAndDemand.requirementsChecking;

import lsp.*;
import lsp.resources.LSPResource;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

class ExampleCheckRequirementsOfAssigner {

	public static LSP createLSPWithProperties(Network network) {
		
		//Create red LogisticsSolution which has the corresponding info
		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("RedCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
		CarrierVehicle redVehicle = CarrierVehicle.newInstance(redVehicleId, collectionLinkId);
		redVehicle.setType( collectionType );

		CarrierCapabilities.Builder redCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		redCapabilitiesBuilder.addType(collectionType);
		redCapabilitiesBuilder.addVehicle(redVehicle);
		redCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities redCapabilities = redCapabilitiesBuilder.build();
		Carrier redCarrier = CarrierUtils.createCarrier( redCarrierId );
		redCarrier.setCarrierCapabilities(redCapabilities);
				
		Id<LSPResource> redAdapterId = Id.create("RedCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder redAdapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(redAdapterId, network);
		redAdapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		redAdapterBuilder.setCarrier(redCarrier);
		redAdapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource redAdapter = redAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> redElementId = Id.create("RedElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder redElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(redElementId );
		redElementBuilder.setResource(redAdapter);
		LogisticsSolutionElement redElement = redElementBuilder.build();
		
		Id<LogisticsSolution> redSolutionId = Id.create("RedSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder redSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(redSolutionId );
		redSolutionBuilder.addSolutionElement(redElement);
		LogisticsSolution redSolution = redSolutionBuilder.build();
		
		//Add info that shows the world the color of the solution
		redSolution.getInfos().add(new RedInfo());
		
		
		//Create blue LogisticsSolution which has the corresponding info
		Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
		Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId);
		blueVehicle.setType( collectionType );

		CarrierCapabilities.Builder blueCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		blueCapabilitiesBuilder.addType(collectionType);
		blueCapabilitiesBuilder.addVehicle(blueVehicle);
		blueCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities blueCapabilities = blueCapabilitiesBuilder.build();
		Carrier blueCarrier = CarrierUtils.createCarrier( blueCarrierId );
		blueCarrier.setCarrierCapabilities(blueCapabilities);
				
		Id<LSPResource> blueAdapterId = Id.create("BlueCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder blueAdapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(blueAdapterId, network);
		blueAdapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		blueAdapterBuilder.setCarrier(blueCarrier);
		blueAdapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource blueAdapter = blueAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> blueElementId = Id.create("BlueCElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder blueElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(blueElementId );
		blueElementBuilder.setResource(blueAdapter);
		LogisticsSolutionElement blueElement = blueElementBuilder.build();
		
		Id<LogisticsSolution> blueSolutionId = Id.create("BlueSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder blueSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(blueSolutionId );
		blueSolutionBuilder.addSolutionElement(blueElement);
		LogisticsSolution blueSolution = blueSolutionBuilder.build();
		
		//Add info that shows the world the color of the solution
		blueSolution.getInfos().add(new BlueInfo());
				
		//Create the initial plan, add assigner that checks requirements of the shipments when assigning and add both solutions (red and blue) to the 
		//plan.
		LSPPlan plan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = new RequirementsAssigner();
		plan.setAssigner(assigner);
		plan.addSolution(redSolution);
		plan.addSolution(blueSolution);
		
		LSPUtils.LSPBuilder lspBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		lspBuilder.setInitialPlan(plan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(redAdapter);
		resourcesList.add(blueAdapter);
			
		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		lspBuilder.setSolutionScheduler(simpleScheduler);
		return lspBuilder.build();
	}
	
	public static Collection<LSPShipment> createShipmentsWithRequirements(Network network){
		//Create ten shipments with either a red or blue requirement, i.e. that they only can be transported in a solution with the matching color
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());

		Random rand = new Random(1);
	    
	    for(int i = 1; i < 11; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
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
        	
        	builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setEndTimeWindow(endTimeWindow);
        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setStartTimeWindow(startTimeWindow);
        	builder.setDeliveryServiceTime(capacityDemand * 60 );
        	boolean blue = rand.nextBoolean();
        	if (blue) {
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
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();
		
		//Create LSP and shipments
        LSP lsp = createLSPWithProperties(network);
        Collection<LSPShipment> shipments =  createShipmentsWithRequirements(network);
	
        //assign the shipments to the LSP
        for(LSPShipment shipment : shipments) {
        	lsp.assignShipmentToLSP(shipment);
        }
        
        for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
        	if(solution.getId().toString().equals("RedSolution")) {
        		for(LSPShipment shipment : solution.getShipments()) {
        			if(!(shipment.getRequirements().iterator().next() instanceof RedRequirement)) {
        				break;
        			}
        		}
        		System.out.println("All shipments in " + solution.getId() + " are red");
        	}
        	if(solution.getId().toString().equals("BlueSolution")) {
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
