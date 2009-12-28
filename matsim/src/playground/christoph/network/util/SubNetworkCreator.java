package playground.christoph.network.util;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.network.SubLink;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.SubNode;

public class SubNetworkCreator {

//	private NodeKnowledge nodeKnowledge;
	private Network network;
	
//	private Map<Id, NodeImpl[]> inNodes;
//	private Map<Id, NodeImpl[]> outNodes;
	private Map<Id, Link[]> inLinks;	// <NodeId, LinkImpl[]>
	private Map<Id, Link[]> outLinks;	// <NodeId, LinkImpl[]>
	
	public SubNetworkCreator(Network network)
	{
		this.network = network;
		
		createLookupTables();
	}
	
	private void createLookupTables()
	{
		inLinks = new HashMap<Id, Link[]>((int)(this.network.getNodes().size() * 1.1), 0.95f);
		outLinks = new HashMap<Id, Link[]>((int)(this.network.getNodes().size() * 1.1), 0.95f);
		
		for(Node node : this.network.getNodes().values())
		{			
			Link[] in = node.getInLinks().values().toArray(new Link[node.getInLinks().size()]);
			Link[] out = node.getOutLinks().values().toArray(new Link[node.getOutLinks().size()]);
		
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
		synchronized(subNetwork)
		{
			synchronized(nodeKnowledge)
			{	
				Map<Id, Node> knownNodes = nodeKnowledge.getKnownNodes();
			
				Map<Id, SubNode> nodesMapping = new HashMap<Id, SubNode>((int)(knownNodes.size() * 1.1), 0.95f);//new TreeMap<Id, SubNode>();
				
				subNetwork.initialize(knownNodes.size());
				
				// create all SubNodes
				for (Node node : knownNodes.values())
				{
					SubNode subNode = new SubNode(node);
	
					subNetwork.addNode(subNode);
					
					nodesMapping.put(node.getId(), subNode);
				}
				
				// create all SubLinks
				for (SubNode subNode : nodesMapping.values())
				{
					// Get all known Links from and To the Node
					Link[] in = inLinks.get(subNode.getId());
					for (Link link : in)
					{
						// Check only the Node and not the Link - should be twice as fast. 
						//if (nodeKnowledge.knowsNode(link.getFromNode()))
						SubNode fromNode = nodesMapping.get(link.getFromNode().getId());
						if (fromNode != null)
						{
							Id fromId = link.getFromNode().getId();
							Id toId = link.getToNode().getId();
							SubLink subLink = new SubLink(this.network, nodesMapping.get(fromId), nodesMapping.get(toId), link);
							
							subNode.addInLinkNoCheck(subLink);
							fromNode.addOutLinkNoCheck(subLink);
	
							subNetwork.addLink(subLink);
						}
					}		
				}
						
				subNetwork.setInitialized();
				
				return subNetwork;
			}
		}
	}	
	
	public SubNetwork createSubNetworkOld(NodeKnowledge nodeKnowledge, SubNetwork subNetwork)
	{
		subNetwork.initialize(nodeKnowledge.getKnownNodes().size());
		
		int a = 0;
		int b = 0;
		
		for (Node node : nodeKnowledge.getKnownNodes().values())
		{
			//SubNode subNode = new SubNode(node);
			
			Link[] in = inLinks.get(node.getId());
			//NodeImpl[] in = inNodes.get(node.getId());
			
			boolean knowsAllLinks = true;
			
			boolean[] knowsInLinks = new boolean[in.length];
			int i = 0;
			for (Link link : in)
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
			
			Link[] out = outLinks.get(node.getId());
			//NodeImpl[] out = outNodes.get(node.getId());
			
			boolean[] knowsOutLinks = new boolean[out.length];
			i = 0;
			for (Link link : out)
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
			subNetwork.addNode(subNode);
			
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
