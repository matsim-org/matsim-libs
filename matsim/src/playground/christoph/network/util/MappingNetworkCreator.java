package playground.christoph.network.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.christoph.network.MappingLink;
import playground.christoph.network.MappingLinkImpl;
import playground.christoph.network.MappingNode;
import playground.christoph.network.MappingNodeImpl;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.mapping.Mapping;
import playground.christoph.network.mapping.MappingInfo;
import playground.christoph.network.mapping.SingleLinkMapping;
import playground.christoph.network.mapping.SingleNodeMapping;

public class MappingNetworkCreator {

	private Network network;
	
	private Map<Id, Link[]> inLinks;	// <NodeId, LinkImpl[]>
	private Map<Id, Link[]> outLinks;	// <NodeId, LinkImpl[]>
	
	public MappingNetworkCreator(Network network)
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
	
	public SubNetwork createMappingNetwork()
	{		
		return createSubNetwork(network);
	}
	
	public SubNetwork createSubNetwork(Network network)
	{	
		SubNetwork subNetwork = new SubNetwork(network);
		
		Map<Id,? extends Node> knownNodes = network.getNodes();
		
		Map<Id, MappingNode> nodesMapping = new HashMap<Id, MappingNode>((int)(knownNodes.size() * 1.1), 0.95f);
		
		subNetwork.initialize(knownNodes.size());
		
		// create all MappingNodes
		for (Node node : knownNodes.values())
		{
			MappingNode mappingNode = new MappingNodeImpl(node.getId(), node.getCoord());
			Mapping mapping = new SingleNodeMapping(node.getId(), node, mappingNode);
			mappingNode.setDownMapping(mapping);
			
			if (node instanceof MappingInfo) ((MappingInfo)node).setUpMapping(mapping); 
			
			subNetwork.addNode(mappingNode);
			
			nodesMapping.put(node.getId(), mappingNode);
		}
		
		// create all MappingLinks
		for (MappingNode mappingNode : nodesMapping.values())
		{
			// Get all known Links from and To the Node
			Link[] in = inLinks.get(mappingNode.getId());
			for (Link link : in)
			{
				// Check only the Node and not the Link - should be twice as fast. 
				//if (nodeKnowledge.knowsNode(link.getFromNode()))
				MappingNode fromNode = nodesMapping.get(link.getFromNode().getId());
				if (fromNode != null)
				{
					Id fromId = link.getFromNode().getId();
					Id toId = link.getToNode().getId();
					
					List<Link> links = new ArrayList<Link>();
					links.add(link);
					MappingLink mappingLink = new MappingLinkImpl(link.getId(), nodesMapping.get(fromId), nodesMapping.get(toId));
					Mapping mapping = new SingleLinkMapping(link.getId(), link, mappingLink);
					mappingLink.setDownMapping(mapping);
					
					if (link instanceof MappingInfo) ((MappingInfo)link).setUpMapping(mapping); 
						
					mappingNode.addInLink(mappingLink);
					fromNode.addOutLink(mappingLink);

					subNetwork.addLink(mappingLink);
				}
			}		
		}
				
		subNetwork.setInitialized();
		
		return subNetwork;
	}	
}
