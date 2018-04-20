package testMutualReplanning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

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
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import demand.controler.MutualModule;
import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPPlanDecorator;
import demand.decoratedLSP.LSPPlanWithOfferTransferrer;
import demand.decoratedLSP.LSPWithOffers;
import demand.decoratedLSP.LSPDecorators;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.decoratedLSP.LogisticsSolutionWithOffers;
import demand.demandAgent.DemandAgent;
import demand.demandAgent.DemandAgentImpl;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjectImpl;
import demand.demandObject.DemandObjects;
import demand.demandObject.DemandPlanImpl;
import demand.demandObject.ShipperShipment;
import demand.demandObject.ShipperShipmentImpl;
import demand.mutualReplanning.DemandPlanStrategyImpl;
import demand.mutualReplanning.DemandReplannerImpl;
import demand.mutualReplanning.MutualReplanningModule;
import demand.mutualReplanning.MutualReplanningModuleImpl;
import demand.mutualReplanning.OfferReplanningStrategyModuleImpl;
import demand.offer.OfferFactoryImpl;
import demand.offer.OfferTransferrer;
import demand.scoring.MutualScoringModule;
import demand.scoring.MutualScoringModuleImpl;
import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.SolutionScheduler;
import lsp.events.EventUtils;
import lsp.resources.Resource;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
import testLSPWithCostTracker.CollectionServiceHandler;
import testLSPWithCostTracker.DistanceAndTimeHandler;
import testLSPWithCostTracker.LinearCostTracker;
import testLSPWithCostTracker.TourStartHandler;

public class MutualReplanningTest {
	
	private LSPDecorator lsp;
	private Collection<DemandObject> demandObjects;
	
	
	@Before
	public void initialize(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
		Network network = scenario.getNetwork();
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Random random = new Random(1);
		
		CollectionCarrierScheduler collectionScheduler = new CollectionCarrierScheduler();
		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(20);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType collectionType = vehicleTypeBuilder.build();
				
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId);
		collectionVehicle.setVehicleType(collectionType);
				
		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarrierImpl.newInstance(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);
						
		Id<Resource> collectionAdapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder collectionAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(collectionAdapterId, network);
		collectionAdapterBuilder.setCollectionScheduler(collectionScheduler);
		collectionAdapterBuilder.setCarrier(collectionCarrier);
		collectionAdapterBuilder.setLocationLinkId(collectionLinkId);
		Resource collectionAdapter = collectionAdapterBuilder.build();
				
		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(collectionElementId);
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();
				
		Id<LogisticsSolution> solutionId = Id.create("Solution", LogisticsSolution.class);
		LogisticsSolutionWithOffers.Builder solutionBuilder = LogisticsSolutionWithOffers.Builder.newInstance(solutionId);
		solutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolutionDecorator solution = solutionBuilder.build();

		LinearCostTracker tracker = new LinearCostTracker(0.2);
		tracker.getEventHandlers().add(new TourStartHandler());
		tracker.getEventHandlers().add(new CollectionServiceHandler());
		tracker.getEventHandlers().add(new DistanceAndTimeHandler(network));	
		solution.addSimulationTracker(tracker);
		
		
		OfferFactoryImpl offerFactory = new OfferFactoryImpl(solution);
		offerFactory.addOffer(new LinearOffer(solution));
		solution.setOfferFactory(offerFactory);
				
		LSPPlanDecorator plan = new LSPPlanWithOfferTransferrer();
		plan.addSolution(solution);
					
		OfferTransferrer transferrer = new SimpleOfferTransferrer();
		plan.setOfferTransferrer(transferrer);
				
		LSPWithOffers.Builder offerLSPBuilder = LSPWithOffers.Builder.getInstance();
		offerLSPBuilder.setInitialPlan(plan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		offerLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
						
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		offerLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = offerLSPBuilder.build();
		
		demandObjects = new ArrayList<>();
		for(int i = 1; i < 11 ; i++) {
        	DemandObjectImpl.Builder builder = DemandObjectImpl.Builder.newInstance();
        	builder.setId(Id.create("DemandObject_" + i, DemandObject.class));
        	DemandAgentImpl.Builder shipperBuilder = DemandAgentImpl.Builder.newInstance();
        	shipperBuilder.setId(Id.create("DemandObject_" + i+ "_Shipper", DemandAgent.class));
        	builder.setShipper(shipperBuilder.build());
        	DemandAgentImpl.Builder recipientBuilder = DemandAgentImpl.Builder.newInstance();
        	recipientBuilder.setId(Id.create("DemandObject_" + i+ "_Recipient", DemandAgent.class));
        	builder.setRecipient(recipientBuilder.build());
        	double shipmentSize= 5 + random.nextDouble()*5;
        	builder.setStrengthOfFlow(shipmentSize);
        	builder.setToLinkId(collectionLinkId);
        	   	
        	
        	while(true) {
        		Collections.shuffle(linkList, random);
        		Link pendingFromLink = linkList.get(0);
        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
        		   pendingFromLink.getToNode().getCoord().getY() <= 4000    ) {
        		   builder.setFromLinkId(pendingFromLink.getId());
        		   break;	
        		}
        	}
        	
        	DemandPlanImpl.Builder planBuilder = DemandPlanImpl.Builder.newInstance();
        	ShipperShipmentImpl.Builder shipmentBuilder = ShipperShipmentImpl.Builder.newInstance();
        	shipmentBuilder.setId(Id.create("DemandObject_" + i+ "_Shipment", ShipperShipment.class));
        	shipmentBuilder.setShipmentSize(shipmentSize);
        	shipmentBuilder.setServiceTime(shipmentSize * 60);
        	planBuilder.setShipperShipment(shipmentBuilder.build());
        	planBuilder.setLsp(lsp);
        	planBuilder.setLogisticsSolutionId(lsp.getSelectedPlan().getSolutions().iterator().next().getId());
        	builder.setInitialPlan(planBuilder.build());
        	builder.setScorer(new FortyTwoDemandScorer());
        	DemandReplannerImpl replanner = new DemandReplannerImpl();
        	DemandPlanStrategyImpl planStrategy = new DemandPlanStrategyImpl(new BestPlanSelector());
        	planStrategy.addStrategyModule(new OfferReplanningStrategyModuleImpl());
        	replanner.addStrategy(planStrategy);
        	builder.setReplanner(replanner);
        	builder.setOfferRequester(new AllOffersRequester());
        	builder.setDemandPlanGenerator(new HalfLotSizeDemandPlanGenerator());
        	DemandObject demandObject = builder.build();
           	demandObjects.add(demandObject);
        	
		}
		
		
		Collection<LSPDecorator>lsps = new ArrayList<>();
		lsps.add(lsp);
		MutualScoringModule mutScoreModule = new MutualScoringModuleImpl(demandObjects, lsps);
		
		MutualReplanningModule mutReplanModule = new MutualReplanningModuleImpl( lsps, demandObjects);
			
		MutualModule.Builder moduleBuilder = MutualModule.Builder.newInstance();
		moduleBuilder.setDemandObjects(new DemandObjects(demandObjects));
		moduleBuilder.setLsps(new LSPDecorators(lsps));
		moduleBuilder.setMutualReplanningModule(mutReplanModule);
		moduleBuilder.setMutualScoringModule(mutScoreModule);
		moduleBuilder.setEventCreators(EventUtils.getStandardEventCreators());
		MutualModule mutualModule = moduleBuilder.build();
		
		Controler controler = new Controler(config);
		controler.addOverridingModule(mutualModule);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("input\\lsp\\network\\2regions.xml");
		controler.run();
	}
	
	@Test	
	public void testReplanning() {
		for(DemandObject demandObject : demandObjects) {
			assertEquals((demandObject.getStrengthOfFlow()/2), demandObject.getSelectedPlan().getShipment().getShipmentSize(), 0.1);
		}
		
	}
	
}
