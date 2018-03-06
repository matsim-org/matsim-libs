package demandObjectTests;

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
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

import demand.decoratedLSP.LSPDecorator;
import demand.demandAgent.DemandAgent;
import demand.demandAgent.DemandAgentImpl;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjectImpl;
import demand.demandObject.DemandPlan;
import demand.demandObject.DemandPlanImpl;
import demand.demandObject.OfferRequester;
import demand.demandObject.ShipperShipment;
import demand.demandObject.ShipperShipmentImpl;
import demand.mutualReplanning.DemandPlanStrategyImpl;
import demand.mutualReplanning.DemandReplannerImpl;
import demand.mutualReplanning.OfferReplanningStrategyModule;
import demand.mutualReplanning.OfferReplanningStrategyModuleImpl;
import demand.offer.Offer;

public class DemandObjectBuilderTest {

	private Network network;
	private ArrayList<DemandObject> demandObjects;
	private OfferRequester offerRequester;
	private LSPDecorator lsp;
	private DemandReplannerImpl replanner;
	private DemandPlanStrategyImpl planStrategy;
	private OfferReplanningStrategyModule offerModule ;
	
	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
        this.network = scenario.getNetwork();
        ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
        this.demandObjects = new ArrayList<DemandObject>();
        Random random = new Random(1);
        lsp = new InitialLSPGenerator().createInitialLSP();
        
        
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
        	
        	while(true) {
        		Collections.shuffle(linkList, random);
        		Link pendingToLink = linkList.get(0);
        		if((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
        			pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
        			pendingToLink.getFromNode().getCoord().getX() >= 14000 &&       			
        			pendingToLink.getToNode().getCoord().getX() <= 18000 &&
        			pendingToLink.getToNode().getCoord().getY() <= 4000  &&
        			pendingToLink.getToNode().getCoord().getX() >= 14000	)) {
        		   builder.setToLinkId(pendingToLink.getId());
        		   break;	
        		}
        	}
        	
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
        	replanner = new DemandReplannerImpl();
        	planStrategy = new DemandPlanStrategyImpl(new BestPlanSelector());
        	offerModule = new OfferReplanningStrategyModuleImpl();
        	planStrategy.addStrategyModule(offerModule);
        	replanner.addStrategy(planStrategy);
        	builder.setReplanner(replanner);
        	builder.setOfferRequester(new AllOffersRequester());
        	builder.setDemandPlanGenerator(new HalfLotSizeDemandPlanGenerator());
        	demandObjects.add(builder.build());
        }
        
	}	
	
	@Test
	public void testDemandObjectBuilding() {
		for(DemandObject demandObject : demandObjects) {
			Offer offer = lsp.getOffer(demandObject, "linear", lsp.getSelectedPlan().getSolutions().iterator().next().getId());
			Collection<Offer>offerList= new ArrayList<>();
			offerList.add(offer);
			DemandPlan newPlan = demandObject.getDemandPlanGenerator().createDemandPlan(offerList); 
			DemandPlan oldPlan = demandObject.getSelectedPlan();
			assertTrue(newPlan.getLsp() == oldPlan.getLsp());
			assertTrue(newPlan.getSolutionId()  == oldPlan.getSolutionId());
			assertTrue(newPlan.getShipment().getShipmentSize() == oldPlan.getShipment().getShipmentSize()/2);
			assertNotNull(demandObject.getOfferRequester());
			assertNotNull(demandObject.getReplanner());
			
		}
	}
	
}
