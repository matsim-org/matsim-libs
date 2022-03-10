package example.lspAndDemand.requirementsChecking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import demand.decoratedLSP.*;
import lsp.*;
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

import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjectImpl;
import demand.offer.Offer;
import demand.offer.OfferFactoryImpl;
import demand.offer.OfferTransferrer;
import lsp.resources.LSPResource;
import lsp.shipment.Requirement;

class ExampleCheckRequirementsOfOfferTransferrer {

	public static LSPDecorator createLSPWithProperties(Network network) {
		
		//Create red LogisticsSolution which has the corresponding info
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
		DecoratedLSPUtils.LogisticsSolutionDecoratorImpl_wOffersBuilder redOfferSolutionBuilder = DecoratedLSPUtils.LogisticsSolutionDecoratorImpl_wOffersBuilder.newInstance(redSolutionId);
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
		
		Id<LogisticsSolutionElement> blueElementId = Id.create("BlueElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder blueElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(blueElementId );
		blueElementBuilder.setResource(blueAdapter);
		LogisticsSolutionElement blueElement = blueElementBuilder.build();
		
		Id<LogisticsSolution> blueSolutionId = Id.create("BlueSolution", LogisticsSolution.class);
		DecoratedLSPUtils.LogisticsSolutionDecoratorImpl_wOffersBuilder blueOfferSolutionBuilder = DecoratedLSPUtils.LogisticsSolutionDecoratorImpl_wOffersBuilder.newInstance(blueSolutionId);
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
		
		LSPWithOffers.Builder offerLSPBuilder = LSPWithOffers.Builder.newInstance();
		offerLSPBuilder.setInitialPlan(plan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		offerLSPBuilder.setId(collectionLSPId);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(redAdapter);
		resourcesList.add(blueAdapter);
			
		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		offerLSPBuilder.setSolutionScheduler(simpleScheduler);
		return offerLSPBuilder.build();

	}
	
	
	public static Collection<DemandObject> createDemandObjectsWithRequirements(){
		//Create ten demand objects with either a red or blue requirement, i.e. that they only can be transported in a solution with the matching color
		ArrayList<DemandObject> demandObjects = new ArrayList<>();
	    
	    Random rand = new Random(1); 
	    
	    for(int i = 1; i < 11; i++) {
			DemandObjectImpl.Builder builder = DemandObjectImpl.Builder.newInstance(Id.create(("DemandObject_" + i), DemandObject.class));
        	
        	boolean blue = rand.nextBoolean();
        	if (blue) {
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
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();
		
		//Create LSP and demand objects
		LSPDecorator lsp = createLSPWithProperties(network);
		Collection<DemandObject> demandObjects =  createDemandObjectsWithRequirements();
		
		for(DemandObject demandObject : demandObjects) {
    		Offer offer = lsp.getOffer(demandObject, "nonsense", null);
    		for(Requirement requirement : demandObject.getRequirements()) {
    			if((requirement instanceof RedRequirement) && (offer.getSolution().getId().toString().equals("RedSolution"))) {
    				System.out.println(demandObject.getId()  +" is red and gets an offer from a " + offer.getSolution().getId().toString() );
    			}
    			else if((requirement instanceof BlueRequirement) && (offer.getSolution().getId().toString().equals("BlueSolution"))){
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
