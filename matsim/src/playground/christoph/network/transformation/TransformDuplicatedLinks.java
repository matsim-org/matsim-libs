package playground.christoph.network.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

import playground.christoph.network.MappingLink;
import playground.christoph.network.MappingLinkImpl;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.mapping.Mapping;
import playground.christoph.network.mapping.MappingInfo;
import playground.christoph.network.mapping.ParallelLinkMapping;

/*
 * Transforms parallel Links that connect the same Nodes.
 */
public class TransformDuplicatedLinks extends TransformationImpl {

	private static final Logger log = Logger.getLogger(TransformDuplicatedLinks.class);
	
	public TransformDuplicatedLinks(SubNetwork subNetwork)
	{
		this.network = subNetwork;

		// Map<FromNode Id, Map<ToNodeId, List<Duplicated Links to ToNode>>>
		this.transformableLinks = new HashMap<Id, Map<Id, List<Link>>>();
	}
	
	public void doTransformation()
	{
		for(Map<Id, List<Link>> map : this.getTransformableStructures().values())
		{
			for (List<Link> duplicates : map.values())
			{
				Node fromNode = duplicates.get(0).getFromNode();
				Node toNode = duplicates.get(0).getToNode();
				
				MappingLink ml1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), fromNode, toNode);
				
				// create Mapping Information and add it to the MappingLinks
				Mapping m1 = new ParallelLinkMapping(new IdImpl("mapped" + idCounter++), duplicates, ml1);
				ml1.setDownMapping(m1);
				for (Link link : duplicates) ((MappingInfo)link).setUpMapping(m1);
				
				fromNode.addOutLink(ml1);
				toNode.addInLink(ml1);
				network.addLink(ml1);
				
				for (Link link : duplicates)
				{
					network.getLinks().remove(link.getId());
					link.getFromNode().getOutLinks().remove(link.getId());
					link.getToNode().getInLinks().remove(link.getId());
				}
			}	
		}
				
		if (printStatistics) log.info("New NodeCount: " + network.getNodes().size());
		if (printStatistics) log.info("new LinkCount: " + network.getLinks().size());
	}
	
	/*
	 * Searches for Pairs of Nodes that are directly connected by more
	 * than one Link. This may occur in given input networks or after
	 * transforming the network.
	 */
	public void findTransformableStructures() 
	{
		
		for (Node node : network.getNodes().values())
		{		
			Map<Node, List<Link>> map = new HashMap<Node, List<Link>>();
			
			/*
			 * We create a Map with the ToNode as Id. If multiple
			 * Link lead to the same ToNode, there will be only one
			 * entry in the Map.
			 */
			for (Link link : node.getOutLinks().values())
			{
				map.put(link.getToNode(), new ArrayList<Link>());
			}
			
			for (Link link : node.getOutLinks().values())
			{
				List<Link> linkList = map.get(link.getToNode());
				linkList.add(link);
			}
			
			/*
			 * Now add the Lists with multiple Entries to the List
			 * with the duplicated Links.
			 */
			for (List<Link> linkList : map.values())
			{
				if(linkList.size() > 1)
				{
					Map<Id, List<Link>> map2 = this.getTransformableStructures().get(node.getId()); 
					
					if (map2 == null)
					{
						map2 = new HashMap<Id, List<Link>>();
						this.getTransformableStructures().put(node.getId(), map2);
					}
					// Id of the ToNode, Duplicated Links to the ToNode
					map2.put(linkList.get(0).getToNode().getId(), linkList);
				}
			}
		}
		
		if (printStatistics) log.info("Duplicated Links: " + getTransformableStructures().size());
	}

	@SuppressWarnings("unchecked")
	public Map<Id, Map<Id, List<Link>>> getTransformableStructures() 
	{
		return (Map<Id, Map<Id, List<Link>>>) this.transformableLinks;
	}

	/*
	 * We have no criteria to filter Links that should not be combined,
	 * so we combine all of them.
	 */
	public void selectTransformableStructures()
	{
		if (printStatistics) log.info("Transformable Links: " + this.transformableLinks.size());
	}

}
