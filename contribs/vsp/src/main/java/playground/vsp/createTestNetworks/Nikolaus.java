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

/**
 * @author Tilmann Schlenther
 * 
 * 
 * 
 * 
 * 			    (8)<------>(7)<--->(9)
 * 				 ' 	^		.	  ´	 '
 * 				 '	  `		.	´	 '
 *				 '		 `	. ´ 	 ' 
 * 				 '		  >(6)<		 '
 *				 '		´  		.	 '
 *				 '	 ´			   . '
 *				 '<					 >
 *				(1)------->(2)----->(3)
 *				 ^	.			     '
 *				 '		.		 	  '
 * 				 '		   .    	  '
 * 				 '	          .		  '
 *  			 '	     		.	  '
 * 				 '    				. '
 *  			(5)<-----------------(4)
 */
	
public class Nikolaus extends CircuitNetworkCreator {

	private static Id<Link> LINK_ID6 = Id.create("Link6", Link.class);
	private static Id<Link> LINK_ID7 = Id.create("Link7", Link.class);
	private static Id<Link> LINK_ID8 = Id.create("Link8", Link.class);
	private static Id<Link> LINK_ID9 = Id.create("Link9", Link.class);
	private static Id<Link> LINK_ID10 = Id.create("Link10", Link.class);
	private static Id<Link> LINK_ID11 = Id.create("Link11", Link.class);
	private static Id<Link> LINK_ID12 = Id.create("Link12", Link.class);
	private static Id<Link> LINK_ID13 = Id.create("Link13", Link.class);
	private static Id<Link> LINK_ID14 = Id.create("Link14", Link.class);
	private static Id<Link> LINK_ID15 = Id.create("Link15", Link.class);
	private static Id<Link> LINK_ID16 = Id.create("Link16", Link.class);
	private static Id<Link> LINK_ID17 = Id.create("Link17", Link.class);
	
	/**
	 * @param scenario
	 */
	public Nikolaus(Scenario scenario) {
		super(scenario);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void createNetwork(){
		super.createNetwork();
		
		Network network = this.scenario.getNetwork();
		NetworkFactory factory = network.getFactory();
//		Node node6 = factory.createNode(Id.createNodeId("4"), new Coord (1000, 1000));
//		network.addNode(node6);
		Node node1 = network.getNodes().get(Id.createNodeId("1"));
		Node node3 = network.getNodes().get(Id.createNodeId("3"));
		Node node4 = network.getNodes().get(Id.createNodeId("4"));
		
		Node node6 = factory.createNode(Id.createNodeId("6"), new Coord (-500, 500));
		Node node7 = factory.createNode(Id.createNodeId("7"), new Coord (-1000, 500));
		Node node8 = factory.createNode(Id.createNodeId("8"), new Coord (-1000, 0));
		Node node9 = factory.createNode(Id.createNodeId("9"), new Coord (-1000, 1000));
		
		network.addNode(node6);
		network.addNode(node7);
		network.addNode(node8);
		network.addNode(node9);
		
		Link link6 = factory.createLink((LINK_ID6), node4, node1);		
		link6.setCapacity(3600);
		link6.setLength(1414);
		link6.setFreespeed(10);
		network.addLink(link6);	
		
		Link link7 = factory.createLink((LINK_ID7), node1, node6 );		
		link7.setCapacity(3600);
		link7.setLength(707);
		link7.setFreespeed(10);
		network.addLink(link7);
		
		Link link8 = factory.createLink((LINK_ID8), node6, node1 );		
		link8.setCapacity(3600);
		link8.setLength(707);
		link8.setFreespeed(10);
		network.addLink(link8);
		
		Link link9 = factory.createLink((LINK_ID9), node6, node3 );		
		link9.setCapacity(3600);
		link9.setLength(707);
		link9.setFreespeed(10);
		network.addLink(link9);
		
		Link link10 = factory.createLink((LINK_ID10), node3, node6 );		
		link10.setCapacity(3600);
		link10.setLength(707);
		link10.setFreespeed(10);
		network.addLink(link10);
		
		Link link11 = factory.createLink((LINK_ID11), node6, node9 );		
		link11.setCapacity(3600);
		link11.setLength(707);
		link11.setFreespeed(10);
		network.addLink(link11);
		
		Link link12 = factory.createLink((LINK_ID12), node9, node3 );		
		link12.setCapacity(3600);
		link12.setLength(1000);
		link12.setFreespeed(10);
		network.addLink(link12);
		
		Link link13 = factory.createLink((LINK_ID13), node9, node7 );		
		link13.setCapacity(3600);
		link13.setLength(500);
		link13.setFreespeed(10);
		network.addLink(link13);
		
		Link link14 = factory.createLink((LINK_ID14), node7, node6 );		
		link14.setCapacity(3600);
		link14.setLength(500);
		link14.setFreespeed(10);
		network.addLink(link14);
		
		Link link15 = factory.createLink((LINK_ID15), node7, node8 );		
		link15.setCapacity(3600);
		link15.setLength(500);
		link15.setFreespeed(10);
		network.addLink(link15);
		
		Link link16 = factory.createLink((LINK_ID16), node8, node6 );		
		link16.setCapacity(3600);
		link16.setLength(707);
		link16.setFreespeed(10);
		network.addLink(link16);
		
		Link link17 = factory.createLink((LINK_ID17), node8, node1 );		
		link17.setCapacity(3600);
		link17.setLength(1000);
		link17.setFreespeed(10);
		network.addLink(link17);
	}

}
