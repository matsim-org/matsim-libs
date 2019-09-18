package requirementsCheckerTests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import lsp.*;
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
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.resources.Resource;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;

public class AssignerRequirementsTest {
	
	private Network network;
	private LogisticsSolution blueSolution;
	private LogisticsSolution redSolution;
	private ShipmentAssigner assigner;
	private LSPPlan collectionPlan;
	private LSP collectionLSP;	
	
	@Before
	public void initialize() {
		
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();
		
		CollectionCarrierScheduler redScheduler = new CollectionCarrierScheduler();
		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
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
		Resource redCollectionAdapter = redAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> redElementId = Id.create("RedCollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder redCollectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(redElementId);
		redCollectionElementBuilder.setResource(redCollectionAdapter);
		LogisticsSolutionElement redCollectionElement = redCollectionElementBuilder.build();
		
		Id<LogisticsSolution> redCollectionSolutionId = Id.create("RedCollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder redCollectionSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(redCollectionSolutionId);
		redCollectionSolutionBuilder.addSolutionElement(redCollectionElement);
		redSolution = redCollectionSolutionBuilder.build();
		redSolution.getInfos().add(new RedInfo());
		
		assigner = new RequirementsAssigner();
		collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(redSolution);
	
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
		Resource blueCollectionAdapter = blueAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> blueElementId = Id.create("BlueCollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder blueCollectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(blueElementId);
		blueCollectionElementBuilder.setResource(blueCollectionAdapter);
		LogisticsSolutionElement blueCollectionElement = blueCollectionElementBuilder.build();
		
		Id<LogisticsSolution> blueCollectionSolutionId = Id.create("BlueCollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder blueCollectionSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(blueCollectionSolutionId);
		blueCollectionSolutionBuilder.addSolutionElement(blueCollectionElement);
		blueSolution = blueCollectionSolutionBuilder.build();
		blueSolution.getInfos().add(new BlueInfo());
		collectionPlan.addSolution(blueSolution);
		
		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(redCollectionAdapter);
		resourcesList.add(blueCollectionAdapter);
			
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
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
        	
        	LSPShipment shipment = builder.build();
        	collectionLSP.assignShipmentToLSP(shipment);
	    }	
	}
	
	@Test
	public void testAssignerRequirements() {
		for(LSPShipment shipment : blueSolution.getShipments()) {
			assertTrue(shipment.getRequirements().iterator().next() instanceof BlueRequirement);
		}
		for(LSPShipment shipment : redSolution.getShipments()) {
			assertTrue(shipment.getRequirements().iterator().next() instanceof RedRequirement);
		}
	}
	
}
