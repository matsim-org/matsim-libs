package playground.christoph.network.transformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.christoph.network.MappingLink;
import playground.christoph.network.MappingLinkImpl;
import playground.christoph.network.MappingNode;
import playground.christoph.network.MappingNodeImpl;
import playground.christoph.network.SubNetwork;
import playground.christoph.network.mapping.LoopMapping;
import playground.christoph.network.mapping.Mapping;
import playground.christoph.network.mapping.MappingInfo;
import playground.christoph.network.mapping.SingleLinkMapping;

/*
 * Find Loops that are shorter than a given value.
 * Only the shortest Loop per Node is transformed.
 * 
 * The used Loop Lists contains all Nodes of a Loop.
 * The last Node is connected to the first Node again,
 * no Node is contained twice in a List.
 */
public class TransformLoops extends TransformationImpl{

	private static final Logger log = Logger.getLogger(TransformLoops.class);
	
	/*
	 * We will do a maximum of 10 flooding Iterations
	 * per Node.
	 */
	private int maxIterations = 10;
	
	/*
	 * The maximum allowed costs that a Loop may have.
	 * We use the Length of the Loop to calculate the costs.
	 */
	private double maxCosts = 5000.0;
	
	private Map<Id, LoopData> loopDatas;
	
	public TransformLoops(SubNetwork subNetwork)
	{
		this.network = subNetwork;
		this.loopDatas = new HashMap<Id, LoopData>();		
		this.transformableNodes = new TreeMap<Id, List<Node>>(new LoopComparator(loopDatas));
	}
	
	public void setMaxCosts(double costs)
	{
		this.maxCosts = costs;
	}
	
	public void doTransformation()
	{	
		for (List<Node> loop : this.getTransformableStructures().values())
		{
			Coord newCentre = calculateCenterCoord(loop);
			
			MappingNode mappingNode = new MappingNodeImpl(new IdImpl("mapped" + idCounter++), newCentre);
			
			List<MappingInfo> list = new ArrayList<MappingInfo>();
			for (Node node : loop)
			{
				list.add((MappingInfo) node);
			}
			Mapping nodeMapping = new LoopMapping(new IdImpl("mapped" + idCounter++), list, mappingNode);
			mappingNode.setDownMapping(nodeMapping);
			
			/*
			 * Remove all Links on the Loop and add the
			 * remaining In- and OutLinks to the mapped Node
			 */
			for (Node node : loop)
			{
				((MappingInfo)node).setUpMapping(nodeMapping);
				
				for (Link inLink : node.getInLinks().values())
				{
					// If it is part of the Loop remove it from the Network
					if (loop.contains(inLink.getFromNode()))
					{
						this.network.getLinks().remove(inLink.getId());
						((MappingInfo)inLink).setUpMapping(nodeMapping);
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
						Node fromNode = inLink.getFromNode();
						fromNode.getOutLinks().remove(inLink.getId());
						fromNode.addOutLink(mappingLink);
						
//						this.network.getLinks().put(mappingLink.getId(), mappingLink);
						this.network.addLink(mappingLink);
						this.network.getLinks().remove(inLink.getId());				
					}
				}
				for (Link outLink : node.getOutLinks().values())
				{
					// If it is part of the Loop remove it from the Network
					if (loop.contains(outLink.getToNode()))
					{
						this.network.getLinks().remove(outLink.getId());
						((MappingInfo)outLink).setUpMapping(nodeMapping);
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
						Node toNode = outLink.getToNode();
						toNode.getInLinks().remove(outLink.getId());
						toNode.addInLink(mappingLink);
						
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
		}
		
		if (printStatistics) log.info("New NodeCount: " + network.getNodes().size());
		if (printStatistics) log.info("new LinkCount: " + network.getLinks().size());
	}

	public void findTransformableStructures()
	{			
		for (Node node : network.getNodes().values())
		{	
			this.floodNode(node);
		}

		if (printStatistics) log.info("Transformable Loops: " + transformableNodes.size());
	}
	
	private void floodNode(Node node)
	{
//		log.info(node.getId() + " " + this.getTransformableStructures().size());
		double foundMinCost = Double.MAX_VALUE;
		FloodNode[] foundLoop = new FloodNode[2];
		
		// A Node and its corresponding FloodNode
		Map<Node, FloodNode> currentIteration = new HashMap<Node, FloodNode>();
		
		// A Node and its corresponding FloodNode
		Map<Node, FloodNode> lastIteration = new HashMap<Node, FloodNode>();
		
		// Map of FloodNodes that still have to be processed
		/*
		 * [TODO] We have to sort the Map - otherwise the Results are not
		 * deterministic. That should not be necessary - so find out why.
		 */
//		Map<Node, FloodNode> nodesToProcess = new HashMap<Node, FloodNode>();
		Map<Node, FloodNode> nodesToProcess = new TreeMap<Node, FloodNode>(new Comparator<Node>() {
		    public int compare(Node n1, Node n2) 
		    {
		       return n1.getId().compareTo(n2.getId());
		    }
		});
		
		/*
		 * Do initial Iteration to fill the nodesToProcess List.
		 */		
		FloodNode currentNode = new FloodNode(node, null, 0.0);
		for (Link outLink : node.getOutLinks().values())
		{
			Node outNode = outLink.getToNode();
			FloodNode floodNode = new FloodNode(outNode, currentNode, outLink.getLength());
			currentIteration.put(node, currentNode);
			
			if (floodNode.getCosts() < maxCosts) nodesToProcess.put(outNode, floodNode);
		}
		
		int iteration = 1;
		while (iteration < maxIterations && nodesToProcess.size() > 0)
		{
			lastIteration.clear();
			lastIteration.putAll(currentIteration);
			currentIteration.clear();
			
			Map<Node, FloodNode> nodesForNextIteration = new HashMap<Node, FloodNode>();
			
			Iterator<FloodNode> iter = nodesToProcess.values().iterator();
			while(iter.hasNext())
			{
				FloodNode nodeToProcess = iter.next();
				Node node2 = nodeToProcess.getCurrentNode();
		
				currentIteration.put(node2, nodeToProcess);
				
				for (Link outLink : node2.getOutLinks().values())
				{
					Node outNode = outLink.getToNode();		
					
					/*
					 * If we found a Loop:
					 * - don't process that Node further
					 * - check if it is cheaper than a previous found loop
					 * - check if it is a previous Node
					 */
					if (lastIteration.containsKey(outNode))
					{
						/*
						 * We skip the outNode if it is the same as the previousNode 
						 * of the currently processed Node
						 */
						if (outNode.equals(nodeToProcess.getPreviousNode().getCurrentNode()))
						{
							/*
							 * Skip the outNode because it is the currents
							 * Node parent.
							 */
//							log.info("skipping");
						}
						/*
						 * The Node is contained in the lastIteration Map
						 * but it is not the parent of the currently processed
						 * Node so we found a Loop!
						 */
						else
						{
							// Calculate the costs to get to the OutNode
							double costs = nodeToProcess.costs + outLink.getLength();

							// Check whether the new loop is really cheaper than an existing one
							double totalCosts =  costs + lastIteration.get(outNode).getCosts();
							
							if (totalCosts < foundMinCost && totalCosts < maxCosts)
							{
								FloodNode floodNode = new FloodNode(outNode, nodeToProcess, costs);
								
								foundMinCost = totalCosts;
								foundLoop[0] = floodNode;
								foundLoop[1] = lastIteration.get(outNode);
							}
						}
					}
					else if (currentIteration.containsKey(outNode))
					{
						double costs = nodeToProcess.costs + outLink.getLength();

						// check if the new loop is really cheaper
						double totalCosts =  costs + currentIteration.get(outNode).getCosts();
						if (totalCosts < foundMinCost && totalCosts < maxCosts)
						{
							foundMinCost = totalCosts;
							foundLoop[0] = nodeToProcess;
							foundLoop[1] = currentIteration.get(outNode);
//							log.info("Found Loop!");
						}
					}
					else if (nodesToProcess.containsKey(outNode))
					{
						/*
						 *  Nothing to do here. The loop will be found
						 *  when the Node is processed because then
						 *  the current Node is contained in the
						 *  currentIteration Map.
						 */
					}
					// no loop found - continue searching
					else
					{
						// Calculate Costs to get to the OutNode
						double costs = nodeToProcess.costs + outLink.getLength();
						
						/*
						 * Don't process Node if it is already to expensive
						 */
						if (costs < maxCosts)
						{
							FloodNode floodNode = new FloodNode(outNode, nodeToProcess, costs);
							
							nodesForNextIteration.put(outNode, floodNode);
//							log.info("Adding... " + iteration + " " + costs);
						}
					}
					
				}	// for all outLinks
				
				iter.remove();
			}
			
			nodesToProcess.putAll(nodesForNextIteration);
			
			iteration++;
		}	// while iterations

		/*
		 * Check whether we found a valid loop and if we
		 * did, add it to the transformableNodes.
		 */
		if (foundMinCost < Double.MAX_VALUE)
		{
			LinkedList<Node> loop = new LinkedList<Node>();
			FloodNode floodNode;
			
			floodNode = foundLoop[0];
			loop.addFirst(floodNode.getCurrentNode());
			while((floodNode = floodNode.getPreviousNode()) != null)
			{
				loop.addFirst(floodNode.getCurrentNode());
			}
			
			floodNode = foundLoop[1];
			loop.addLast(floodNode.getCurrentNode());
			while((floodNode = floodNode.getPreviousNode()) != null)
			{
				loop.addLast(floodNode.getCurrentNode());
			}
			
			/*
			 *  We added the first Node twice to the List so we
			 *  remove it once again. This should be faster than
			 *  always checking if its the last one.
			 */
			if (loop.getFirst() != loop.getLast()) log.error("Ooops. That should not happen...");
			loop.removeLast();
							
			// create LoopData Object that is needed by the TreeMap's Comparator			
			LoopData loopData = new LoopData(foundMinCost, loop.size());
			
			this.loopDatas.put(node.getId(), loopData);
			
			this.getTransformableStructures().put(node.getId(), loop);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<Id, List<Node>> getTransformableStructures() 
	{
		return (Map<Id, List<Node>>) this.transformableNodes;
	}

	/*
	 * How to select Transformable Loops?
	 * 
	 * Order Loops by Costs.
	 * 
	 * Find all Nodes of the current Loop that are
	 * also part of other Loops.
	 */
	public void selectTransformableStructures()
	{
		// A List of Nodes that have been transformed
		List<Node> transformedNodes = new ArrayList<Node>();
		
		// for all transformable Loops
		Iterator <Entry<Id, List<Node>>> iter = this.getTransformableStructures().entrySet().iterator();
		while(iter.hasNext())
		{
			Entry<Id, List<Node>> entry = iter.next();
			List<Node> loop = entry.getValue();

			boolean transformable = true;
			// for all Nodes of the List
			for (Node node : loop)
			{
				if (transformedNodes.contains(node))
				{
					transformable = false;
					break;
				}
			}
			
			// if all Nodes are transformable
			if (transformable)
			{
				transformedNodes.addAll(loop);
			}
			// not transformable so remove from Map
			else
			{
				loopDatas.remove(entry.getKey());
				iter.remove();
			}		
		}
		
		if (printStatistics) log.info("Selected Loops for Transformation: " + transformableNodes.size());
		if (printStatistics) log.info("Selected Nodes for Transformation: " + transformedNodes.size());
	}
	
	private Coord calculateCenterCoord(List<Node> nodes)
	{
		double x = 0.0;
		double y = 0.0;
		
		for (Node node : nodes)
		{
			x = x + node.getCoord().getX();
			y = y + node.getCoord().getY();
		}
		
		x = x / nodes.size();
		y = y / nodes.size();
		
		Coord coord = new CoordImpl(x, y);
		
		return coord;
	}
	
	private class FloodNode
	{
		private Node currentNode;
		private FloodNode previousNode;
		private double costs;
				
		public FloodNode(Node currentNode, FloodNode previousNode, double costs)
		{
			this.currentNode = currentNode;
			this.previousNode = previousNode;
			this.costs = costs;
		}
		
		public Node getCurrentNode()
		{
			return this.currentNode;
		}
		
		public FloodNode getPreviousNode()
		{
			return this.previousNode;
		}
		
		public double getCosts()
		{
			return this.costs;
		}
	}
	
	/*
	 * Contains some information on the found Loops
	 * that are used by the Comparator to sort the Loops.
	 */
	private class LoopData
	{
		private double costs;
		private int nodeCount;
		
		public LoopData(double costs, int nodeCount)
		{
			this.costs = costs;
			this.nodeCount = nodeCount;
		}
		
		public double getCosts()
		{
			return this.costs;
		}
		
		public int getNodeCount()
		{
			return this.nodeCount;
		}
	}
	
	/*
	 * Order the Nodes by the following criteria:
	 * 1) Sum of the Length of the Links. The shorter the earlier.
	 * otherwise:
	 * 2) NodesCount. The less Nodes the earlier.
	 * otherwise:
	 * 3) Ids of the first Nodes in the List.
	 * 
	 * These criteria guarantee a deterministic behavior.
	 */
	private class LoopComparator implements Comparator<Id>, Serializable {

		private static final long serialVersionUID = 1L;
		private Map<Id, LoopData> loopsData;
		
		public LoopComparator(Map<Id, LoopData> loopsData)
		{
			this.loopsData = loopsData;
		}
	
		public int compare(final Id Id1, final Id Id2) 
		{			
			LoopData loopData1 = loopsData.get(Id1);
			LoopData loopData2 = loopsData.get(Id2);
			
			double costs1 = loopData1.getCosts();
			double costs2 = loopData2.getCosts();

			int nodeCount1 = loopData1.getNodeCount();
			int nodeCount2 = loopData2.getNodeCount();
			
			// compare Loop Lengths
			if (costs1 < costs2) return -1;
			else if (costs1 > costs2) return 1;
			else 
			{
				// compare Node Count
				if (nodeCount1 < nodeCount2) return -1;
				else if (nodeCount1 > nodeCount2) return 1;
				else return Id1.compareTo(Id2);
			}	
		}
	}
}
