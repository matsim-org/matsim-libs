/**
 * 
 */
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
 *				(1)------->(2)---------->(3)
 *				 ^	.				      '
 *				 '		.		    	  '
 * 				 '		   .     		  '
 * 				 '	        	.		  '
 *  			 '	     			.	  '
 * 				 '    					. '
 *  			(5)<---------------------(4)
 */
	
public class Nikolaus extends CircuitNetworkCreator {

	private static Id<Link> LINK_ID6 = Id.create("Link6", Link.class);
	private static Id<Link> LINK_ID7 = Id.create("Link7", Link.class);
	
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
		Node node4 = network.getNodes().get(Id.createNodeId("4"));
		
		Link link6 = factory.createLink((LINK_ID6), node1, node4);		
		link6.setCapacity(3600);
		link6.setLength(Math.sqrt(2000));
		link6.setFreespeed(1);
		network.addLink(link6);	
	}

}
