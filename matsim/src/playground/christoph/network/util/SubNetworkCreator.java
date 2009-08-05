package playground.christoph.network.util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.SubNode;

public class SubNetworkCreator {

//	private NodeKnowledge nodeKnowledge;
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
		inLinks = new HashMap<Id, LinkImpl[]>((int)(this.network.getNodes().size() * 1.1), 0.95f);
		outLinks = new HashMap<Id, LinkImpl[]>((int)(this.network.getNodes().size() * 1.1), 0.95f);
		
		for(NodeImpl node : this.network.getNodes().values())
		{			
			LinkImpl[] in = node.getInLinks().values().toArray(new LinkImpl[node.getInLinks().size()]);
			LinkImpl[] out = node.getOutLinks().values().toArray(new LinkImpl[node.getOutLinks().size()]);
		
			Id id = node.getId();
			
			inLinks.put(id, in);
			outLinks.put(id, out);
		}
	}
	
	public SubNetwork createSubNetwork(NodeKnowledge nodeKnowledge)
	{
		SubNetwork subNetwork = new SubNetwork(network);
		
		return createSubNetwork(nodeKnowledge, subNetwork);
	}
	
	public SubNetwork createSubNetwork(NodeKnowledge nodeKnowledge, SubNetwork subNetwork)
	{
		subNetwork.initialize(nodeKnowledge.getKnownNodes().size());
		
		int a = 0;
		int b = 0;
		
		for (NodeImpl node : nodeKnowledge.getKnownNodes().values())
		{
			//SubNode subNode = new SubNode(node);
			
			LinkImpl[] in = inLinks.get(node.getId());
			//NodeImpl[] in = inNodes.get(node.getId());
			
			boolean knowsAllLinks = true;
			
			boolean[] knowsInLinks = new boolean[in.length];
			int i = 0;
			for (LinkImpl link : in)
			{
				// Check only the Node and not the Link - should be twice as fast. 
				if (nodeKnowledge.knowsNode(link.getFromNode()))
				{
					knowsInLinks[i] = true;
//					subNode.addInLinkNoCheck(link);
//					subNetwork.addLink(link);
				}
				else
				{
					knowsInLinks[i] = false;
					knowsAllLinks = false;
				}
				i++;
			}
			
			LinkImpl[] out = outLinks.get(node.getId());
			//NodeImpl[] out = outNodes.get(node.getId());
			
			boolean[] knowsOutLinks = new boolean[out.length];
			i = 0;
			for (LinkImpl link : out)
			{
				// Check only the Node and not the Link - should be twice as fast.
				if (nodeKnowledge.knowsNode(link.getToNode()))
				{
					knowsOutLinks[i] = true;
//					subNode.addOutLinkNoCheck(link);
//					subNetwork.addLink(link);
				}
				else
				{
					knowsOutLinks[i] = false;
					knowsAllLinks = false;
				}
				i++;
			}
			
			//SubNode subNode;
			Node subNode;
			
			// if all In- and OutLinks are known, we can use the Data from the Parent Node
			if (knowsAllLinks)
			{
				a++;
				//subNode = new SubNode(node, true);
				subNode = node;
			}
			else
			{
				b++;
				subNode = new SubNode(node);
				
				i = 0;
				for (boolean knowsLink : knowsInLinks)
				{
					if (knowsLink)
					{
						((SubNode)subNode).addInLinkNoCheck(in[i]);
						subNetwork.addLink(in[i]);
					}
					i++;
				}
				
				i = 0;
				for (boolean knowsLink : knowsOutLinks)
				{
					if (knowsLink)
					{
						((SubNode)subNode).addInLinkNoCheck(out[i]);
						subNetwork.addLink(out[i]);
					}
					i++;
				}
			}
			
//			if (subNode != null) 
			subNetwork.addSubNode(subNode);
			
			knowsInLinks = null;
			knowsOutLinks = null;
			in = null;
			out = null;
		}
				
		subNetwork.setInitialized();
		
//		System.out.println("existing: " + a + ", new: " + b);
		
		return subNetwork;
	}	
}
