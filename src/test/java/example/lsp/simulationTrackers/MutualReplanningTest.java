package example.lsp.simulationTrackers;

import demand.DemandAgent;
import demand.DemandUtils;
import demand.controler.DemandControlerUtils;
import demand.controler.MutualModule;
import demand.decoratedLSP.*;
import demand.demandObject.*;
import demand.mutualReplanning.*;
import demand.offer.OfferFactoryImpl;
import demand.offer.OfferTransferrer;
import demand.scoring.MutualScoringModule;
import demand.scoring.MutualScoringModuleImpl;
import lsp.*;
import lsp.resources.LSPResource;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreatorUtils;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import testLSPWithCostTracker.CollectionServiceHandler;
import testLSPWithCostTracker.DistanceAndTimeHandler;
import testLSPWithCostTracker.TourStartHandler;
import testMutualReplanning.LinearOffer;
import testMutualReplanning.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class MutualReplanningTest {

	private Collection<DemandObject> demandObjects;
	
	
	@Before
	public void initialize(){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());
		Random random = new Random(1);

		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(20);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId);
		collectionVehicle.setType( collectionType );

		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarrierUtils.createCarrier( collectionCarrierId );
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);
		
		Id<LSPResource> collectionAdapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder collectionAdapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(collectionAdapterId, network);
		collectionAdapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		collectionAdapterBuilder.setCarrier(collectionCarrier);
		collectionAdapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource collectionAdapter = collectionAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(collectionElementId );
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();
		
		Id<LogisticsSolution> solutionId = Id.create("Solution", LogisticsSolution.class);
		DecoratedLSPUtils.LogisticsSolutionDecoratorImpl_wOffersBuilder solutionBuilder = DecoratedLSPUtils.LogisticsSolutionDecoratorImpl_wOffersBuilder.newInstance(solutionId);
		solutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolutionDecorator solution = solutionBuilder.build();
		
		LinearCostTracker tracker = new LinearCostTracker(0.2);
		tracker.getEventHandlers().add(new TourStartHandler());
		tracker.getEventHandlers().add(new CollectionServiceHandler());
		tracker.getEventHandlers().add(new DistanceAndTimeHandler(network));
		solution.addSimulationTracker(tracker);
		
		
		OfferFactoryImpl offerFactory = new OfferFactoryImpl(solution);
		offerFactory.addOffer(new LinearOffer(solution) );
		solution.setOfferFactory(offerFactory);
		
		LSPPlanDecorator plan = new LSPPlanWithOfferTransferrer();
		plan.addSolution(solution);
		
		OfferTransferrer transferrer = new SimpleOfferTransferrer();
		plan.setOfferTransferrer(transferrer);
		
		LSPWithOffers.Builder offerLSPBuilder = LSPWithOffers.Builder.newInstance();
		offerLSPBuilder.setInitialPlan(plan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		offerLSPBuilder.setId(collectionLSPId);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionAdapter);
		
		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		offerLSPBuilder.setSolutionScheduler(simpleScheduler);
		LSPDecorator lsp = offerLSPBuilder.build();
		
		demandObjects = new ArrayList<>();
		for(int i = 1; i < 11 ; i++) {
			DemandObjectImpl.Builder builder = DemandObjectImpl.Builder.newInstance(Id.create("DemandObject_" + i, DemandObject.class));
			DemandUtils.DemandAgentImplBuilder shipperBuilder = DemandUtils.DemandAgentImplBuilder.newInstance();
			shipperBuilder.setId(Id.create("DemandObject_" + i+ "_Shipper", DemandAgent.class));
			builder.setShipper(shipperBuilder.build());
			DemandUtils.DemandAgentImplBuilder recipientBuilder = DemandUtils.DemandAgentImplBuilder.newInstance();
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
			builder.setScorer(new FortyTwoDemandScorer() );
			DemandReplannerImpl replanner = new DemandReplannerImpl();
			DemandPlanStrategyImpl planStrategy = new DemandPlanStrategyImpl(new BestPlanSelector());
			planStrategy.addStrategyModule(new OfferReplanningStrategyModuleImpl());
			replanner.addStrategy(planStrategy);
			builder.setReplanner(replanner);
			builder.setOfferRequester(new AllOffersRequester() );
			builder.setDemandPlanGenerator(new HalfLotSizeDemandPlanGenerator() );
			DemandObject demandObject = builder.build();
			demandObjects.add(demandObject);
			
		}
		
		
		Collection<LSPDecorator>lsps = new ArrayList<>();
		lsps.add(lsp);
		MutualScoringModule mutScoreModule = new MutualScoringModuleImpl(demandObjects, lsps);
		
		MutualReplanningModule mutReplanModule = new MutualReplanningModuleImpl( lsps, demandObjects);
		
		MutualModule.Builder moduleBuilder = MutualModule.Builder.newInstance();
		moduleBuilder.setDemandObjects(new DemandObjects(demandObjects));
		moduleBuilder.setLsps(DemandControlerUtils.createLSPDecorators(lsps));
		moduleBuilder.setMutualReplanningModule(mutReplanModule);
		moduleBuilder.setMutualScoringModule(mutScoreModule);
		moduleBuilder.setEventCreators(LSPEventCreatorUtils.getStandardEventCreators());
		MutualModule mutualModule = moduleBuilder.build();
		
		Controler controler = new Controler(config);
		controler.addOverridingModule(mutualModule);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		controler.run();
	}
	
	@Test
	public void testReplanning() {
		for(DemandObject demandObject : demandObjects) {
			assertEquals((demandObject.getStrengthOfFlow()/2), demandObject.getSelectedPlan().getShipment().getShipmentSize(), 0.1);
		}
		
	}
	
}
