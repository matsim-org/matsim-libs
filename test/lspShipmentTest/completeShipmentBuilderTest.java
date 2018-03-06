package lspShipmentTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import demand.demandAgent.DemandAgent;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public class completeShipmentBuilderTest {
	
	private Network network;
	private ArrayList<LSPShipment> shipments;

	@Before
	public void initialize(){
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
        this.network = scenario.getNetwork();
        ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
        this.shipments = new ArrayList<LSPShipment>();
        
        for(int i = 1; i < 11; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
        	int capacityDemand = new Random().nextInt(10);
        	builder.setCapacityDemand(capacityDemand);
        	
        	while(true) {
        		Collections.shuffle(linkList);
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
        	
        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setEndTimeWindow(endTimeWindow);
        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setStartTimeWindow(startTimeWindow);
        	builder.setServiceTime(capacityDemand * 60);
        	shipments.add(builder.build());
        }
	}
	
		
	@Test
	public void testShipments() {
		assertTrue(shipments.size() == 10);
		for(LSPShipment shipment : shipments) {
			assertNotNull(shipment.getId());
			assertNotNull(shipment.getCapacityDemand());
			assertNotNull(shipment.getEndTimeWindow());
			assertNotNull(shipment.getFromLinkId());
			assertNotNull(shipment.getServiceTime());
			assertNotNull(shipment.getToLinkId());
			assertNotNull(shipment.getStartTimeWindow());
			assertNotNull(shipment.getSchedule());
			assertNotNull(shipment.getLog());
			assertNotNull(shipment.getEventHandlers());
			
			assertTrue(shipment.getEventHandlers().isEmpty());
			assertEquals(shipment.getLog().getShipment(), shipment);
			assertTrue(shipment.getLog().getPlanElements().isEmpty());
			
			assertEquals(shipment.getSchedule().getShipment(), shipment);
			assertTrue(shipment.getSchedule().getPlanElements().isEmpty());
			Link link = network.getLinks().get(shipment.getToLinkId());
			assertTrue(link.getFromNode().getCoord().getX() <= 18000);
			assertTrue(link.getFromNode().getCoord().getX() >= 14000);
			assertTrue(link.getToNode().getCoord().getX() <= 18000);
			assertTrue(link.getToNode().getCoord().getX() >= 14000);
			
			link = network.getLinks().get(shipment.getFromLinkId());
			assertTrue(link.getFromNode().getCoord().getX() <= 4000);
			assertTrue(link.getFromNode().getCoord().getY() <= 4000);
		}
	}
}
