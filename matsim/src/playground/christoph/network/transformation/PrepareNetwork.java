package playground.christoph.network.transformation;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

import playground.christoph.network.SubNetwork;
import playground.christoph.network.util.MappingNetworkCreator;

/*
 * Some of the Transformation Algorithms assume
 * that each Connection between two Nodes is Bidirectional.
 * So for each Link from A to B there should be a Link from
 * B to A.
 * 
 * If there is no such Link a new one is created with a Length
 * of Double.MAX_VALUE so no agent should ever choose that Link.
 * Doing this allows us to use the Algorithms as they are without
 * concerning about the Network Structure.
 */
public class PrepareNetwork {

	private static final Logger log = Logger.getLogger(TransformLoops.class);
	
	private Network network;
	
	public PrepareNetwork(Network network)
	{
		this.network = network;
	}
	
	/*
	 * Create a 1:1 copy of the Network using MappingLinks
	 * and MappingNodes.
	 */
	public Network createMappedNetwork()
	{
		SubNetwork subNetwork = new MappingNetworkCreator(this.network).createMappingNetwork();
		
		return subNetwork;
	}
	
	public void checkAndAddLinks()
	{
		int idCounter = 0;
		
		NetworkFactory factory = network.getFactory();
		/*
		 * Check and correct Links to ensure that we have only bidirectional
		 * Links!
		 */
		for (Node node : network.getNodes().values()) 
		{
			List<Node> toNodes = new ArrayList<Node>();
			List<Node> fromNodes = new ArrayList<Node>();

			for (Link link : node.getOutLinks().values())
			{
				toNodes.add(link.getToNode());
			}
			for (Link link : node.getInLinks().values())
			{
				fromNodes.add(link.getFromNode());
			}

			for (Link outLink : node.getOutLinks().values())
			{
				Node toNode = outLink.getToNode();
				
				if (!fromNodes.contains(toNode))
				{
					fromNodes.add(outLink.getToNode());
					
					idCounter++;
					Link newLink = factory.createLink(new IdImpl("DummyLink" + idCounter), toNode.getId(), node.getId());
//					newLink.setLength(Double.MAX_VALUE);
					newLink.setLength(outLink.getLength());
					
//					toNode.addOutLink(newLink);
//					node.addInLink(newLink);
					network.addLink(newLink);
//					log.info("adding");
				}
			}
			
			for (Link inLink : node.getInLinks().values())
			{
				Node fromNode = inLink.getFromNode();
				
				if (!toNodes.contains(fromNode))
				{
					toNodes.add(fromNode);
					
					idCounter++;
					Link newLink = factory.createLink(new IdImpl("DummyLink" + idCounter), node.getId(), fromNode.getId());
//					newLink.setLength(Double.MAX_VALUE);
					newLink.setLength(inLink.getLength());
					
//					fromNode.addInLink(newLink);
//					node.addOutLink(newLink);
					network.addLink(newLink);
//					log.info("adding");
				}
			}
			
							
//			for (Iterator<? extends Link> iter = node.getOutLinks().values().iterator(); iter.hasNext();)
//			{
//				Link link = iter.next();
//				if (!fromNodes.contains(link.getToNode()))
//				{
//					link.getToNode().getInLinks().remove(link.getId());
//					network.getLinks().remove(link.getId());
//					iter.remove();
//				}
//			}

//			for (Iterator<? extends Link> iter = node.getInLinks().values().iterator(); iter.hasNext();)
//			{
//				Link link = iter.next();
//				if (!toNodes.contains(link.getFromNode()))
//				{					
//					link.getFromNode().getOutLinks().remove(link.getId());
//					network.getLinks().remove(link.getId());
//					iter.remove();
//				}
//			}

			if (node.getInLinks().size() != node.getOutLinks().size())
			{
				log.warn("Different Link Count!");
			}
		}
	}
}
