package playground.christoph.network.util;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.SubNode;

public class SubNetworkCreator {

	private NodeKnowledge nodeKnowledge;
	private NetworkLayer network;
	
//	private Map<Id, NodeImpl[]> inNodes;
//	private Map<Id, NodeImpl[]> outNodes;
	private Map<Id, LinkImpl[]> inLinks;	// <NodeId, LinkImpl[]>
	private Map<Id, LinkImpl[]> outLinks;	// <NodeId, LinkImpl[]>
	
	public SubNetworkCreator(NetworkLayer network)
	{
		this.network = network;
		
		createLookupTables();
	}
	
	private void createLookupTables()
	{
		inLinks = new TreeMap<Id, LinkImpl[]>();
		outLinks = new TreeMap<Id, LinkImpl[]>();
		
		for(NodeImpl node : this.network.getNodes().values())
		{			
			LinkImpl[] in = node.getInLinks().values().toArray(new LinkImpl[node.getInLinks().size()]);
			LinkImpl[] out = node.getOutLinks().values().toArray(new LinkImpl[node.getOutLinks().size()]);
		
			Id id = node.getId();
			
			inLinks.put(id, in);
			outLinks.put(id, out);
		}
	}
	
	public SubNetwork createSubNetwork()
	{
		SubNetwork subNetwork = new SubNetwork(network);
		
		return createSubNetwork(subNetwork);
	}
	
	public SubNetwork createSubNetwork(SubNetwork subNetwork)
	{
		for (NodeImpl node : nodeKnowledge.getKnownNodes().values())
		{
			SubNode subNode = new SubNode(node);
			
			LinkImpl[] in = inLinks.get(node.getId());
			//NodeImpl[] in = inNodes.get(node.getId());
			
			for (LinkImpl link : in)
			{
				// Check only the Node and not the Link - should be twice as fast. 
				if (nodeKnowledge.knowsNode(link.getFromNode()))
				{
					subNode.addInLink(link);
					subNetwork.addLink(link);
				}
			}
						
			LinkImpl[] out = outLinks.get(node.getId());
			//NodeImpl[] out = outNodes.get(node.getId());
			
			for (LinkImpl link : out)
			{
				// Check only the Node and not the Link - should be twice as fast.
				if (nodeKnowledge.knowsNode(link.getToNode()))
				{
					subNode.addOutLink(link);
					subNetwork.addLink(link);
				}
			}	
			subNetwork.addSubNode(subNode);
		}
		
		subNetwork.setInitialized();
		
		return subNetwork;
	}	
}
