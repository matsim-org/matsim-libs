/**
 * 
 */
package scenarios.braess.createInput;

import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.lanes.utils.LanesUtils;

/**
 * Class to create network and lanes for the breass scenario.
 * 
 * You may choose between simulating inflow capacity or not. Whereby simulating
 * inflow capacity means that one additional short link is added in front of
 * link 2-3 and 4-5.
 * 
 * @author tthunig
 * 
 */
public class TtCreateBraessNetworkAndLanes {

	private Scenario scenario;
	
	private boolean simulateInflowCap = false;
	
	// capacity at the links that all agents have to use
	private long CAP_FIRST_LAST = 3600; // [veh/h]
	// capacity at all other links
	private long CAP_MAIN = 1800; // [veh/h]
	// link length for the inflow links
	private double INFLOW_LINK_LENGTH = 7.5; // [m]
	// link length for all other links
	private long LINK_LENGTH = 200; // [m]
	
	public TtCreateBraessNetworkAndLanes(Scenario scenario) {		
		this.scenario = scenario;
	}

	/**
	 * Creates the network for the Breass scenario.
	 */
	public void createNetwork(){
		
		Network net = this.scenario.getNetwork();
		NetworkFactory fac = net.getFactory();

		// create nodes
		net.addNode(fac.createNode(Id.createNodeId(0),
				scenario.createCoord(-200, 200)));
		net.addNode(fac.createNode(Id.createNodeId(1),
				scenario.createCoord(0, 200)));
		net.addNode(fac.createNode(Id.createNodeId(2),
				scenario.createCoord(200, 200)));
		net.addNode(fac.createNode(Id.createNodeId(3),
				scenario.createCoord(400, 400)));
		net.addNode(fac.createNode(Id.createNodeId(4),
				scenario.createCoord(400, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5),
				scenario.createCoord(600, 200)));
		net.addNode(fac.createNode(Id.createNodeId(6),
				scenario.createCoord(800, 200)));
		
		if (simulateInflowCap){
			net.addNode(fac.createNode(Id.createNodeId(7),
					scenario.createCoord(250, 250)));
			net.addNode(fac.createNode(Id.createNodeId(9), 
					scenario.createCoord(450, 50)));
		}
		
		// create links
		Link l = fac.createLink(Id.createLinkId("0_1"),
				net.getNodes().get(Id.createNodeId(0)),
				net.getNodes().get(Id.createNodeId(1)));
		l.setCapacity(CAP_FIRST_LAST);
		l.setLength(LINK_LENGTH);
		double linkTT = 1;
		l.setFreespeed(l.getLength() / linkTT);
		net.addLink(l);
		
		l = fac.createLink(Id.createLinkId("1_2"),
				net.getNodes().get(Id.createNodeId(1)),
				net.getNodes().get(Id.createNodeId(2)));
		l.setCapacity(CAP_FIRST_LAST);
		l.setLength(LINK_LENGTH);
		linkTT = 1;
		l.setFreespeed(l.getLength() / linkTT);
		net.addLink(l);
		
		if (simulateInflowCap){
			l = fac.createLink(Id.createLinkId("2_7"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(7)));
			l.setCapacity(CAP_MAIN);
			l.setLength(INFLOW_LINK_LENGTH);
			linkTT = 1;
			l.setFreespeed(l.getLength() / linkTT);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("7_3"),
					net.getNodes().get(Id.createNodeId(7)),
					net.getNodes().get(Id.createNodeId(3)));
			l.setCapacity(CAP_MAIN);
			l.setLength(LINK_LENGTH);
			linkTT = 8; // 10 - link travel time of inflow link - 1 additional matsim link second
			l.setFreespeed(l.getLength() / linkTT);
			net.addLink(l);
		}
		else{
			l = fac.createLink(Id.createLinkId("2_3"),
					net.getNodes().get(Id.createNodeId(2)),
					net.getNodes().get(Id.createNodeId(3)));
			l.setCapacity(CAP_MAIN);
			l.setLength(LINK_LENGTH);
			linkTT = 10;
			l.setFreespeed(l.getLength() / linkTT);
			net.addLink(l);
		}
		
		l = fac.createLink(Id.createLinkId("2_4"),
				net.getNodes().get(Id.createNodeId(2)),
				net.getNodes().get(Id.createNodeId(4)));
		l.setCapacity(CAP_MAIN);
		l.setLength(LINK_LENGTH);
		linkTT = 20;
		l.setFreespeed(l.getLength() / linkTT);
		net.addLink(l);
		
		l = fac.createLink(Id.createLinkId("3_4"),
				net.getNodes().get(Id.createNodeId(3)),
				net.getNodes().get(Id.createNodeId(4)));
		l.setCapacity(CAP_MAIN);
		l.setLength(LINK_LENGTH);
		linkTT = 1;
		l.setFreespeed(l.getLength() / linkTT);
		net.addLink(l);
		
		l = fac.createLink(Id.createLinkId("3_5"),
				net.getNodes().get(Id.createNodeId(3)),
				net.getNodes().get(Id.createNodeId(5)));
		l.setCapacity(CAP_MAIN);
		l.setLength(LINK_LENGTH);
		linkTT = 20;
		l.setFreespeed(l.getLength() / linkTT);
		net.addLink(l);
		
		if (simulateInflowCap){
			l = fac.createLink(Id.createLinkId("4_9"),
					net.getNodes().get(Id.createNodeId(4)),
					net.getNodes().get(Id.createNodeId(9)));
			l.setCapacity(CAP_MAIN);
			l.setLength(INFLOW_LINK_LENGTH);
			linkTT = 1;
			l.setFreespeed(l.getLength() / linkTT);
			net.addLink(l);
			
			l = fac.createLink(Id.createLinkId("9_5"),
					net.getNodes().get(Id.createNodeId(9)),
					net.getNodes().get(Id.createNodeId(5)));
			l.setCapacity(CAP_MAIN);
			l.setLength(LINK_LENGTH);
			linkTT = 8; // 10 - link travel time of inflow link - 1 additional matsim link second
			l.setFreespeed(l.getLength() / linkTT);
			net.addLink(l);
		}
		else{
			l = fac.createLink(Id.createLinkId("4_5"),
					net.getNodes().get(Id.createNodeId(4)),
					net.getNodes().get(Id.createNodeId(5)));
			l.setCapacity(CAP_MAIN);
			l.setLength(LINK_LENGTH);
			linkTT = 10;
			l.setFreespeed(l.getLength() / linkTT);
			net.addLink(l);
		}
		
		l = fac.createLink(Id.createLinkId("5_6"),
				net.getNodes().get(Id.createNodeId(5)),
				net.getNodes().get(Id.createNodeId(6)));
		l.setCapacity(CAP_MAIN);
		l.setLength(LINK_LENGTH);
		linkTT = 1;
		l.setFreespeed(l.getLength() / linkTT);
		net.addLink(l);
	}

	public void createLanes() {
		
		LaneDefinitions20 laneDef20 = (LaneDefinitions20) this.scenario
				.getScenarioElement(LaneDefinitions20.ELEMENT_NAME);
		LaneDefinitionsFactory20 fac = laneDef20.getFactory();

		// create link assignment of link 1_2
		LanesToLinkAssignment20 linkAssignment = fac
				.createLanesToLinkAssignment(Id.createLinkId("1_2"));

		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("1_2.ol", Lane.class), CAP_FIRST_LAST,
				LINK_LENGTH, 0, 1, null, 
				Arrays.asList(Id.create("1_2.1", Lane.class),
				Id.create("1_2.2", Lane.class)));
		
		if (simulateInflowCap) {
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.1", Lane.class), CAP_FIRST_LAST,
					LINK_LENGTH / 2, -1, 1, 
					Arrays.asList(Id.createLinkId("2_7")),
					null);
		} else {
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("1_2.1", Lane.class), CAP_FIRST_LAST,
					LINK_LENGTH / 2, -1, 1, 
					Arrays.asList(Id.createLinkId("2_3")),
					null);
		}
		
		LanesUtils.createAndAddLane20(linkAssignment, fac,
				Id.create("1_2.2", Lane.class), CAP_FIRST_LAST,
				LINK_LENGTH / 2, 1, 1,  
				Arrays.asList(Id.createLinkId("2_4")), null);
		
		laneDef20.addLanesToLinkAssignment(linkAssignment);
		
		// create link assignment of link 2_3 (or 7_3 if inflow capacity is simulated)
		if (simulateInflowCap){
			linkAssignment = fac.createLanesToLinkAssignment(Id
					.createLinkId("7_3"));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("7_3.ol", Lane.class), CAP_MAIN,
					LINK_LENGTH, 0, 1, null, 
					Arrays.asList(Id.create("7_3.1", Lane.class),
					Id.create("7_3.2", Lane.class)));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("7_3.1", Lane.class), CAP_MAIN,
					LINK_LENGTH / 2, 0, 1, 
					Arrays.asList(Id.createLinkId("3_5")),
					null);
			
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("7_3.2", Lane.class), CAP_MAIN,
					LINK_LENGTH / 2, 1, 1, 
					Arrays.asList(Id.createLinkId("3_4")),
					null);

			laneDef20.addLanesToLinkAssignment(linkAssignment);
		}
		else{
			linkAssignment = fac.createLanesToLinkAssignment(
					Id.createLinkId("2_3"));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("2_3.ol", Lane.class), CAP_MAIN,
					LINK_LENGTH, 0, 1, null, 
					Arrays.asList(Id.create("2_3.1", Lane.class),
					Id.create("2_3.2", Lane.class)));

			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("2_3.1", Lane.class), CAP_MAIN,
					LINK_LENGTH / 2, 0, 1, 
					Arrays.asList(Id.createLinkId("3_5")),
					null);
			
			LanesUtils.createAndAddLane20(linkAssignment, fac,
					Id.create("2_3.2", Lane.class), CAP_MAIN,
					LINK_LENGTH / 2, 1, 1, 
					Arrays.asList(Id.createLinkId("3_4")),
					null);

			laneDef20.addLanesToLinkAssignment(linkAssignment);
		}
	}

	/**
	 * Sets the flag for simulating inflow capacity.
	 * 
	 * If true, link 2_3 and 4_5 are divided into 2 links: a small one at the
	 * beginning that simulates an inflow capacity at the link and a longer one
	 * that preserves the other properties of the link
	 * 
	 * @param simulateInflowCap
	 */
	public void setSimulateInflowCap(boolean simulateInflowCap) {
		this.simulateInflowCap = simulateInflowCap;
	}

}
