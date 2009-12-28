package playground.christoph.network.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

import playground.christoph.network.MappingLink;
import playground.christoph.network.MappingLinkImpl;
import playground.christoph.network.MappingNode;
import playground.christoph.network.MappingNodeImpl;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.mapping.Mapping;
import playground.christoph.network.mapping.MappingInfo;
import playground.christoph.network.mapping.ShortLinkMapping;
import playground.christoph.network.mapping.SingleLinkMapping;

/*
 * Find Links that are shorter than a given Length l and
 * transform their From- and ToNode to a new Node.
 * 
 * This allows an easy remapping because a MappingNode may
 * have a TravelTime.
 */
public class TransformShortLinks extends TransformationImpl{

	private static final Logger log = Logger.getLogger(TransformShortLinks.class);
	
	private double length = 0.0;
	
	public TransformShortLinks(SubNetwork subNetwork)
	{
		this.network = subNetwork;
		this.transformableLinks = new HashMap<Id, Link[]>();
	}
	
	public void setLength(double length)
	{
		this.length = length;
	}
	
	public void doTransformation()
	{
		for (Link[] links : this.getTransformableStructures().values())
		{
			Node fromNode = links[0].getFromNode();
			Node toNode = links[0].getToNode();
			
			Node[] nodes = new Node[]{fromNode, toNode};
			
			MappingNode mappingNode = new MappingNodeImpl(new IdImpl("mapped" + idCounter++), links[0].getCoord());	
			
			/*
			 * Remove all Links on the Loop and add the
			 * remaining In- and OutLinks to the mapped Node
			 */
			for (Node node : nodes)
			{
				for (Link inLink : node.getInLinks().values())
				{
					// If it is part of the Loop remove it from the Network
					if (links[0] == inLink || links[1] == inLink)
					{
						this.network.getLinks().remove(inLink.getId());
					}
					// Otherwise add it to the new MappedNode
					else
					{
						MappingLink mappingLink = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), inLink.getFromNode(), mappingNode);
						
						Mapping mapping = new SingleLinkMapping(new IdImpl("mapped" + idCounter++), inLink, mappingLink);
						mappingLink.setDownMapping(mapping);
						
						((MappingInfo)inLink).setUpMapping(mapping);
						
						mappingNode.addInLink(mappingLink);
						
						// replace inLink with mappedLink
						Node fromNode2 = inLink.getFromNode();
						fromNode2.getOutLinks().remove(inLink.getId());
						fromNode2.addOutLink(mappingLink);
						
//						this.network.getLinks().put(mappingLink.getId(), mappingLink);
						this.network.addLink(mappingLink);
						this.network.getLinks().remove(inLink.getId());				
					}
				}
				for (Link outLink : node.getOutLinks().values())
				{
					// If it is part of the Loop remove it from the Network
					if (links[0] == outLink || links[1] == outLink)
					{
						this.network.getLinks().remove(outLink.getId());
					}
					// Otherwise add it to the new MappedNode
					else
					{
						MappingLink mappingLink = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), mappingNode, outLink.getToNode());
						
						Mapping mapping = new SingleLinkMapping(new IdImpl("mapped" + idCounter++), outLink, mappingLink);
						mappingLink.setDownMapping(mapping);
						
						((MappingInfo)outLink).setUpMapping(mapping);
						
						mappingNode.addOutLink(mappingLink);
						
						// replace outLink with mappedLink
						Node toNode2 = outLink.getToNode();
						toNode2.getInLinks().remove(outLink.getId());
						toNode2.addInLink(mappingLink);
						
//						this.network.getLinks().put(mappingLink.getId(), mappingLink);
						this.network.addLink(mappingLink);
						this.network.getLinks().remove(outLink.getId());					
					}
				}
				
				node.getInLinks().clear();
				node.getOutLinks().clear();
				this.network.getNodes().remove(node.getId());
			}
			
			this.network.addNode(mappingNode);

/*
			fromNode.getOutLinks().remove(links[0].getId());
			fromNode.getInLinks().remove(links[1].getId());
			toNode.getInLinks().remove(links[0].getId());
			toNode.getOutLinks().remove(links[1].getId());
			
//			for (Link link : fromNode.getInLinks().values()) mappingNode.addInLink(link);
//			for (Link link : fromNode.getOutLinks().values()) mappingNode.addOutLink(link);
//			for (Link link : toNode.getInLinks().values()) mappingNode.addInLink(link);
//			for (Link link : toNode.getOutLinks().values()) mappingNode.addOutLink(link);
			
			for (Link link : fromNode.getInLinks().values()) link.setToNode(mappingNode);
			for (Link link : fromNode.getOutLinks().values()) link.setFromNode(mappingNode);
			for (Link link : toNode.getInLinks().values()) link.setToNode(mappingNode);
			for (Link link : toNode.getOutLinks().values()) link.setFromNode(mappingNode);
			
//			fromNode.getInLinks().clear();
//			fromNode.getOutLinks().clear();
//			toNode.getInLinks().clear();
//			toNode.getOutLinks().clear();
			
			this.network.addNode(mappingNode);
			this.network.getNodes().remove(fromNode.getId());
			this.network.getNodes().remove(toNode.getId());
			this.network.getLinks().remove(links[0].getId());
			this.network.getLinks().remove(links[1].getId());
*/		
			Mapping mapping = new ShortLinkMapping(new IdImpl("mapped" + idCounter++), links, mappingNode);
			mappingNode.setDownMapping(mapping);
			((MappingInfo) links[0]).setUpMapping(mapping);
			((MappingInfo) links[1]).setUpMapping(mapping);
		}
		
		if (printStatistics) log.info("New NodeCount: " + network.getNodes().size());
		if (printStatistics) log.info("new LinkCount: " + network.getLinks().size());
	}

	public void findTransformableStructures() 
	{
		for (Node node : network.getNodes().values())
		{
			for (Link outLink : node.getOutLinks().values())
			{
				double ll = outLink.getLength();
				if (ll < length)
				{
					Node toNode = outLink.getToNode();
					
					for (Link inLink : node.getInLinks().values())
					{
						if (inLink.getFromNode().equals(toNode))
						{
							if (inLink.getLength() == ll)
							{
								Link[] links = new Link[2];
								links[0] = outLink;
								links[1] = inLink;
								
								getTransformableStructures().put(outLink.getId(), links);
							}
						}
					}
				}
			}
		}
		
		if (printStatistics) log.info("Transformable Links: " + transformableLinks.size());
		
//		for (Link link : network.getLinks().values())
//		{
//			if (link.getLength() < length)
//			{
//				getTransformableStructures().put(link.getId(), link);
//			}
//		}		
	}

	@SuppressWarnings("unchecked")
	public Map<Id, Link[]> getTransformableStructures()
	{	
		return (Map<Id, Link[]>) this.transformableLinks;
	}

	/*
	 * We can transform all found Links - even if they are direct
	 * Neighours. Transforming them one after the other will keep
	 * it deterministic.
	 */
	public void selectTransformableStructures()
	{
		List<Node> transformedNodes = new ArrayList<Node>();
		
		Iterator<Link[]> iter = this.getTransformableStructures().values().iterator();
		while (iter.hasNext())
		{	
			Link[] link = iter.next();
			Node fromNode = link[0].getFromNode();
			Node toNode = link[0].getToNode();
			if (!(transformedNodes.contains(fromNode) || transformedNodes.contains(toNode)))
			{
				transformedNodes.add(fromNode);
				transformedNodes.add(toNode);
			}
			else iter.remove();
		}
		if (printStatistics) log.info("Selected Links for Transformation: " + transformableLinks.size());
	}

}
