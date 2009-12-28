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

import playground.christoph.network.SubNetwork;
import playground.christoph.network.mapping.DeadEndMapping;
import playground.christoph.network.mapping.Mapping;
import playground.christoph.network.mapping.MappingInfo;

/*
 * Transforms Nodes that are Dead Ends which have just one
 * In- and one OutLink. 
 */
public class TransformDeadEndNodes extends TransformationImpl {

	private static final Logger log = Logger.getLogger(TransformDeadEndNodes.class);
	
	public TransformDeadEndNodes(SubNetwork subNetwork)
	{
		this.network = subNetwork;
		this.transformableNodes = new HashMap<Id, Node>();
	}
	
	public void findTransformableStructures() {
						
		// for every Node of the Network
		for (Node node : network.getNodes().values())
		{
			if (node.getInLinks().size() == 1) getTransformableStructures().put(node.getId(), node);
		}
		
		if (printStatistics) log.info("Transformable Nodes: " + transformableNodes.size());
	}
	
	/*
	 * We have no criteria to filter Node that should not be transformed,
	 * so we move all of them.
	 */
	public void selectTransformableStructures()
	{
		if (printStatistics) log.info("Selected Nodes for Transformation: " + transformableNodes.size());
	}
	
	@SuppressWarnings("unchecked")
	public Map<Id, Node> getTransformableStructures()
	{
		return (Map<Id, Node>) this.transformableNodes;
	}
	
	public void doTransformation()
	{	
		// for all found Nodes
		for (Node node : this.getTransformableStructures().values())
		{
			Node parentNode = node.getInLinks().values().iterator().next().getFromNode();

			Id id = new IdImpl(Mapping.mappingId++);
			List<MappingInfo> input = new ArrayList<MappingInfo>();
			input.add((MappingInfo) node);
			Mapping mapping = new DeadEndMapping(id, input, parentNode);
			((MappingInfo)node).setUpMapping(mapping);
			
			this.network.getNodes().remove(node.getId());
			
			((MappingInfo)node).setUpMapping(null);
			
			for (Link link : node.getInLinks().values())
			{
				this.network.getLinks().remove(link.getId());
				link.getFromNode().getOutLinks().remove(link.getId());
				input.add((MappingInfo) link);
				((MappingInfo)link).setUpMapping(mapping);
			}
			for (Link link : node.getOutLinks().values())
			{
				this.network.getLinks().remove(link.getId());
				link.getToNode().getInLinks().remove(link.getId());
				input.add((MappingInfo) link);
				((MappingInfo)link).setUpMapping(mapping);
			}
			
			node.getInLinks().clear();
			node.getOutLinks().clear();
		}
	
		if (printStatistics) log.info("New NodeCount: " + network.getNodes().size());
		if (printStatistics) log.info("new LinkCount: " + network.getLinks().size());
	}
}
