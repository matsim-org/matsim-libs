/**
 * 
 */
package playground.vsp.createTestNetworks;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;

/**
 * @author Tilmann Schlenther
 *
 *
 *							  (4)
 *							 ´   `  
 *						III ´	  ` IV
 *						   ´	   `
 *			 I		II	  ´			`	VII
 *		(1)-----(2)-----(3)			(6)-----(7)
 *						  `		    ´			
 *						   `	   ´
 *						  V `	  ´ VI
 *						     `   ´
 *							  (5)
 *
 */
public class ForkNetworkCreator {

	
	private static Id<Link> LINK_ID1 = Id.create("Link1", Link.class);
	private static Id<Link> LINK_ID2 = Id.create("Link2", Link.class);
	private static Id<Link> LINK_ID3 = Id.create("Link3", Link.class);
	private static Id<Link> LINK_ID4 = Id.create("Link4", Link.class);
	private static Id<Link> LINK_ID5 = Id.create("Link5", Link.class);
	private static Id<Link> LINK_ID6 = Id.create("Link6", Link.class);
	private static Id<Link> LINK_ID7 = Id.create("Link7", Link.class);

	protected Scenario scenario;
	protected boolean UseSignals = false;
	protected boolean UseLanes = false;
	
	public ForkNetworkCreator(Scenario scenario, boolean lanes, boolean signals){
		this.scenario = scenario;
		this.UseSignals = signals;
		this.UseLanes = lanes;
	}
	
	
	public void createNetwork(){
		Network network = this.scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node node1 = factory.createNode(Id.createNodeId("1"), new Coord((double) 0, (double) 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), new Coord((double) 0, (double) 1000));
		Node node3 = factory.createNode(Id.createNodeId("3"), new Coord((double) 0, (double) 2000));
		Node node4 = factory.createNode(Id.createNodeId("4"), new Coord((double) 500, (double) 2500));
		double x = -500;
		Node node5 = factory.createNode(Id.createNodeId("5"), new Coord(x, (double) 2500));
		Node node6 = factory.createNode(Id.createNodeId("6"), new Coord((double) 0, (double) 3000));
		Node node7 = factory.createNode(Id.createNodeId("7"), new Coord((double) 0, (double) 4000));

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);
		network.addNode(node7);

		Link link1 = factory.createLink((LINK_ID1), node1, node2);
		link1.setCapacity(3600);
		link1.setLength(1000);
		link1.setFreespeed(201);
		network.addLink(link1);
		
		Link link2 = factory.createLink((LINK_ID2), node2, node3);		
		link2.setCapacity(3600);
		link2.setLength(1000);
		link2.setFreespeed(201);
		network.addLink(link2);	

//		//use signals
//		if(this.UseSignals){	
//			setSignals();
//		}
		
		//use Lanes
		if(this.UseLanes){
			scenario.getConfig().qsim().setUseLanes(true);
			setLanes();
		}
		
		
		Link link3 = factory.createLink((LINK_ID3), node3 , node4);
		link3.setCapacity(1800);
		link3.setLength(1000);
		link3.setFreespeed(10);
		network.addLink(link3);
		
		Link link4 = factory.createLink((LINK_ID4), node4, node6);		
		link4.setCapacity(1800);
		link4.setLength(1000);
		link4.setFreespeed(10);
		network.addLink(link4);
		
		Link link5 = factory.createLink((LINK_ID5), node3, node5);		
		link5.setCapacity(1800);
		link5.setLength(1000);
		link5.setFreespeed(10);
		network.addLink(link5);	
	
		Link link6 = factory.createLink((LINK_ID6), node5, node6);		
		link6.setCapacity(1800);
		link6.setLength(1000);
		link6.setFreespeed(10);
		network.addLink(link6);	
	
		Link link7 = factory.createLink((LINK_ID7), node6, node7);		
		link7.setCapacity(3600);
		link7.setLength(1000);
		link7.setFreespeed(10);
		network.addLink(link7);	
	}


	private void setLanes() {
		Lanes lanes = scenario.getLanes();
		LanesFactory lfactory = lanes.getFactory();
		Id<Lane> olId = Id.create("2.ol", Lane.class);
		Id<Lane> secondLaneId = Id.create("2to3", Lane.class);
		Id<Lane> thirdLaneId = Id.create("2to5", Lane.class);
		LanesToLinkAssignment assignment = lfactory.createLanesToLinkAssignment(LINK_ID2);
		
		Lane lane = lfactory.createLane(olId);
		lane.setNumberOfRepresentedLanes(1);
		lane.setStartsAtMeterFromLinkEnd(1000.0);
		lane.setCapacityVehiclesPerHour(scenario.getNetwork().getLinks().get(LINK_ID2).getCapacity());
		lane.addToLaneId(secondLaneId);
		lane.addToLaneId(thirdLaneId);
		assignment.addLane(lane);
		
		lane = lfactory.createLane(secondLaneId);
		lane.setNumberOfRepresentedLanes(1);
		lane.setStartsAtMeterFromLinkEnd(100.0);
		lane.setCapacityVehiclesPerHour(scenario.getNetwork().getLinks().get(LINK_ID2).getCapacity());
		lane.setAlignment(-1);
		lane.addToLinkId(LINK_ID3);
		assignment.addLane(lane);
		
		lane = lfactory.createLane(thirdLaneId);
		lane.setNumberOfRepresentedLanes(1);
		lane.setStartsAtMeterFromLinkEnd(100.0);
		lane.setCapacityVehiclesPerHour(scenario.getNetwork().getLinks().get(LINK_ID2).getCapacity());
		lane.setAlignment(1);
		lane.addToLinkId(LINK_ID5);
		assignment.addLane(lane);
		
		lanes.addLanesToLinkAssignment(assignment);
		
	}

//	private void setSignals() {
//		Link2LinkTestSignalsCreator signalsCreator = new Link2LinkTestSignalsCreator(this.scenario, this.UseLanes);
//		signalsCreator.createSignals();
//	}
	
}
