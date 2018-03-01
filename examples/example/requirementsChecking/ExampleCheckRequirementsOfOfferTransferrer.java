package example.requirementsChecking;

import static org.junit.Assert.assertTrue;

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

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPPlanDecorator;
import demand.decoratedLSP.LSPPlanWithOfferTransferrer;
import demand.decoratedLSP.LSPWithOffers;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.decoratedLSP.LogisticsSolutionWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjectImpl;
import demand.offer.Offer;
import demand.offer.OfferFactoryImpl;
import demand.offer.OfferTransferrer;
import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.SolutionScheduler;
import lsp.resources.Resource;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;
import lsp.shipment.Requirement;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
import requirementsCheckerTests.BlueInfo;
import requirementsCheckerTests.BlueRequirement;
import requirementsCheckerTests.NonsenseOffer;
import requirementsCheckerTests.RedInfo;
import requirementsCheckerTests.RedRequirement;
import requirementsCheckerTests.RequirementsTransferrer;

public class ExampleCheckRequirementsOfOfferTransferrer {

	public static LSPDecorator createLSPWithProperties(Network network) {
		
		//Create red LogisticsSolution which has the corresponding info
		CollectionCarrierScheduler redScheduler = new CollectionCarrierScheduler();
		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
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
		LogisticsSolutionWithOffers.Builder redOfferSolutionBuilder = LogisticsSolutionWithOffers.Builder.newInstance(redSolutionId);
		redOfferSolutionBuilder.addSolutionElement(redElement);
		LogisticsSolutionDecorator redOfferSolution = redOfferSolutionBuilder.build();
		
		//Add info that shows the world the color of the solution
		redOfferSolution.getInfos().add(new RedInfo());
		
		//Add OfferFactory that gives some nonsense offer, as in this case only the work of the transferrer i.e. the solution from which the offer
		//comes is relevant
		OfferFactoryImpl redOfferFactory = new OfferFactoryImpl(redOfferSolution);
		redOfferFactory.addOffer(new NonsenseOffer());
		redOfferSolution.setOfferFactory(redOfferFactory);
		

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
		
		Id<LogisticsSolutionElement> blueElementId = Id.create("BlueElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder blueElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(blueElementId);
		blueElementBuilder.setResource(blueAdapter);
		LogisticsSolutionElement blueElement = blueElementBuilder.build();
		
		Id<LogisticsSolution> blueSolutionId = Id.create("BlueSolution", LogisticsSolution.class);
		LogisticsSolutionWithOffers.Builder blueOfferSolutionBuilder = LogisticsSolutionWithOffers.Builder.newInstance(blueSolutionId);
		blueOfferSolutionBuilder.addSolutionElement(blueElement);
		LogisticsSolutionDecorator blueOfferSolution = blueOfferSolutionBuilder.build();
		
		//Add info that shows the world the color of the solution
		blueOfferSolution.getInfos().add(new BlueInfo());
		
		//Add OfferFactory that gives some nonsense offer, as in this case only the work of the transferrer i.e. the solution from which the offer
		//comes is relevant
		OfferFactoryImpl blueOfferFactory = new OfferFactoryImpl(blueOfferSolution);
		blueOfferFactory.addOffer(new NonsenseOffer());
		blueOfferSolution.setOfferFactory(blueOfferFactory);
		
		//Create PlanDecorator (i.e. Plan that has an OfferTransferrer) and add solutions
		LSPPlanDecorator plan = new LSPPlanWithOfferTransferrer();
		plan.addSolution(redOfferSolution);
		plan.addSolution(blueOfferSolution);
		
		//Create OfferTransferrer that only gives the offer of the suitable solution with the right color to the outside
		OfferTransferrer transferrer = new RequirementsTransferrer();
		plan.setOfferTransferrer(transferrer);
		
		LSPWithOffers.Builder offerLSPBuilder = LSPWithOffers.Builder.getInstance();
		offerLSPBuilder.setInitialPlan(plan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		offerLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(redAdapter);
		resourcesList.add(blueAdapter);
			
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		offerLSPBuilder.setSolutionScheduler(simpleScheduler);
		return offerLSPBuilder.build();

	}
	
	
	public static Collection<DemandObject> createDemandObjectsWithRequirements(){
		//Create ten demand objects with either a red or blue requirement, i.e. that they only can be transported in a solution with the matching color
		ArrayList<DemandObject> demandObjects = new ArrayList<DemandObject>();
	    
	    Random rand = new Random(1); 
	    
	    for(int i = 1; i < 11; i++) {
        	Id<DemandObject> id = Id.create(("DemandObject_" + Integer.toString(i)), DemandObject.class);
        	DemandObjectImpl.Builder builder = DemandObjectImpl.Builder.newInstance();
        	builder.setId(id);
        	
        	boolean blue = rand.nextBoolean();
        	if (blue == true) {
        		builder.addRequirement(new BlueRequirement());
        	}
        	else {
        		builder.addRequirement(new RedRequirement());
        	}
        	
        	DemandObject demandObject = builder.build();
        	demandObjects.add(demandObject);
	    }	
		
		return demandObjects;
	}
	
	
	public static void main (String[]args) {
		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
		Network network = scenario.getNetwork();
		
		//Create LSP and demand objects
		LSPDecorator lsp = createLSPWithProperties(network);
		Collection<DemandObject> demandObjects =  createDemandObjectsWithRequirements();
		
		for(DemandObject demandObject : demandObjects) {
    		Offer offer = lsp.getOffer(demandObject, "nonsense", null);
    		for(Requirement requirement : demandObject.getRequirements()) {
    			if((requirement instanceof RedRequirement) && (offer.getSolution().getId().toString() == "RedSolution")) {
    				System.out.println(demandObject.getId()  +" is red and gets an offer from a " + offer.getSolution().getId().toString() );
    			}
    			else if((requirement instanceof BlueRequirement) && (offer.getSolution().getId().toString() == "BlueSolution")){
    				System.out.println(demandObject.getId()  +" is blue and gets an offer from a " + offer.getSolution().getId().toString() );
    			}
    			else {
    				System.out.println("Wrong sort of offer for " + demandObject.getId()+ ": " + offer.getSolution().getId().toString() +
    						" was wrong for shipment with " + requirement.getClass());
    			}
    		}
    	}
		        
		        
	}
	
}
