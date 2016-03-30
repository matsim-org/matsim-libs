package playground.tschlenther.createNetwork;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;


/**
 * @author Tilmann Schlenther
 *
 *
 *				(1)------->(2)---------->(3)
 *				 ^						  '
 *				 '						  '
 * 				 '						  '
 * 				 '						  '
 *  			 '						  '
 * 				 '						  '
 *  			(5)<---------------------(4)
 */
	
public class CircuitNetworkCreator {
	
	private static Id<Link> LINK_ID1 = Id.create("Link1", Link.class);
	private static Id<Link> LINK_ID2 = Id.create("Link2", Link.class);
	private static Id<Link> LINK_ID3 = Id.create("Link3", Link.class);
	private static Id<Link> LINK_ID4 = Id.create("Link4", Link.class);
	private static Id<Link> LINK_ID5 = Id.create("Link5", Link.class);

	protected Scenario scenario;
	
	public CircuitNetworkCreator(Scenario scenario){
		this.scenario = scenario;
	}
	
	public void createNetwork(){
		Network network = this.scenario.getNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node node1 = factory.createNode(Id.createNodeId("1"), new Coord (0, 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), new Coord (0, 100));
		Node node3 = factory.createNode(Id.createNodeId("3"), new Coord (0, 1000));
		Node node4 = factory.createNode(Id.createNodeId("4"), new Coord (1000, 1000));
		Node node5 = factory.createNode(Id.createNodeId("5"), new Coord (1000, 0));

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		Link link1 = factory.createLink((LINK_ID1), node1, node2);
		link1.setCapacity(3600);
		link1.setLength(100);
		link1.setFreespeed(10);
		network.addLink(link1);
		
		Link link2 = factory.createLink((LINK_ID2), node2, node3);		
		link2.setCapacity(3600);
		link2.setLength(900);
		link2.setFreespeed(10);
		network.addLink(link2);	
		
		Link link3 = factory.createLink((LINK_ID3), node3 , node4);
		link3.setCapacity(3600);
		link3.setLength(1000);
		link3.setFreespeed(10);
		network.addLink(link3);
		
		Link link4 = factory.createLink((LINK_ID4), node4, node5);		
		link4.setCapacity(3600);
		link4.setLength(1000);
		link4.setFreespeed(10);
		network.addLink(link4);
		
		Link link5 = factory.createLink((LINK_ID5), node5, node1);		
		link5.setCapacity(3600);
		link5.setLength(1000);
		link5.setFreespeed(10);
		network.addLink(link5);	
	}

}
