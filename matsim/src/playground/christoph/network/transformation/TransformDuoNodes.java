package playground.christoph.network.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
import playground.christoph.network.mapping.ChainMapping;
import playground.christoph.network.mapping.Mapping;
import playground.christoph.network.mapping.MappingInfo;

/*
 * Transforms Nodes that connect only two Links, means that
 * they just split a Link into two.
 * (Basically there are 4 Links involved because a Link can
 * be driven only in one direction)
 * 
 * We assume that there are never multiple Links between the
 * same pair of Nodes (means there is exactly one Link from
 * Node A to Node B and one Link from Node B to Node A).
 */
public class TransformDuoNodes extends TransformationImpl {

	private static final Logger log = Logger.getLogger(TransformDuoNodes.class);
	
	public TransformDuoNodes(SubNetwork subNetwork)
	{
		this.network = subNetwork;
		this.transformableNodes = new HashMap<Id, Node>();
	}
	
	public void findTransformableStructures()
	{		
		// for every Node of the Network
		for (Node node : network.getNodes().values())
		{			
			Map<Id, Node> inNodes = new HashMap<Id, Node>();
			for (Link link : node.getInLinks().values())
			{
				Node fromNode = link.getFromNode();
				inNodes.put(fromNode.getId(), fromNode);
			}
			
			Map<Id, Node> outNodes = new HashMap<Id, Node>();
			for (Link link : node.getOutLinks().values())
			{
				Node toNode = link.getToNode();
				outNodes.put(toNode.getId(), toNode);
			}
			
			/*
			 * If multiple Links connect two Nodes (more than
			 * from -> To and To -> From) -> skip Node
			 */
			if (inNodes.size() != node.getInLinks().size()) continue;
			if (outNodes.size() != node.getOutLinks().size()) continue;
			
			if (inNodes.size() != outNodes.size()) continue;
			if (inNodes.size() != 2) continue;
					
			boolean transformable = true;
			
			// inNodes have to equal the outNodes
			for (Node inNode : inNodes.values())
			{
				if (!outNodes.containsKey(inNode.getId()))
				{
					transformable = false;
					break;
				}
			}
			
			if (transformable)
			{
				getTransformableStructures().put(node.getId(), node);
			}
		}
		
		if (printStatistics) log.info("Transformable Nodes: " + transformableNodes.size());
	}
	
	/*
	 * We have no criteria to filter Links that should not be moved,
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
	
	/*
	 * If there are chains of DuoNodes we transform them by 
	 * using a single Link. To do so, we have to identify the
	 * Chains.
	 *		      |           |
	 * modify  ---x---x---x---x---
	 *            |           |
	 * 
	 *		      |           | 
	 * to      ---x-----------x---
	 *		      |           |
	 */
	private List<LinkedList<Node>> getChains()
	{
		// Find Chain Ends
			
		// Collect all Chains in a List
		List<LinkedList<Node>> chains = new ArrayList<LinkedList<Node>>();
		
		LinkedList<Node> nodes = new LinkedList<Node>();
		nodes.addAll(getTransformableStructures().values());
		
//		while(transformableNodes.size() > 0)
		while(nodes.peek() != null)
		{
			// Define each Chain as LinkedList
			LinkedList<Node> chain = new LinkedList<Node>();
			
			// get next Node from List
			Node node = nodes.poll();
			
			// add a first Node to the Chain
			chain.add(node);
			
			/*
			 * Now check, if the previous and / or next Nodes are
			 * also contained in the nodes List. If it is, add them to 
			 * the chain.
			 */
			// get current Node's In- and OutLinks
			Link[] inLinks = new Link[node.getInLinks().size()];
			node.getInLinks().values().toArray(inLinks);
			
			Link[] outLinks  = new Link[node.getOutLinks().size()]; 
			node.getOutLinks().values().toArray(outLinks);
			
			/*
			 * get previousNode and nextNode
			 * Their Order is irrelevant because we define Head and
			 * Tail of the Chain later by comparing the Ids of the Nodes. 
			 */
			Node previousNode = inLinks[0].getFromNode();
			Node nextNode = inLinks[1].getFromNode();
			
//			Node previous = previousNode;
			while(nodes.contains(previousNode))
			{
				nodes.remove(previousNode);
				chain.addFirst(previousNode);
				
				Link[] in = new Link[2];
				previousNode.getInLinks().values().toArray(in);
				
				/*
				 * We found the Node on the transformable List so the
				 * size of inLinks should be exact 2. If not throw a 
				 * warning.
				 */
				if (previousNode.getInLinks().size() != 2)
				{
					log.warn("Wrong length...");
				}
				if (in[0] == null) log.warn("0 null");
				if (in[1] == null) log.warn("1 null");
				
				// Which of the two inLinks leeds to a new Node?
				if (!chain.contains(in[0].getFromNode())) previousNode = in[0].getFromNode();
				else if (!chain.contains(in[1].getFromNode())) previousNode = in[1].getFromNode();
				
				// else: both are already contained -> seems to be cyclic...
				else previousNode = null;
			}
			
			while(nodes.contains(nextNode))
			{
				nodes.remove(nextNode);
				chain.addLast(nextNode);
				
				Link[] in = new Link[2];
				nextNode.getInLinks().values().toArray(in);
				
				/*
				 * We found the Node on the transformable List so the
				 * size of inLinks should be exact 2. If not throw a 
				 * warning.
				 */
				if (nextNode.getInLinks().size() != 2)
				{
					log.warn("Wrong length...");
				}
				if (in[0] == null) log.warn("0 null");
				if (in[1] == null) log.warn("1 null");

				// Which of the two inLinks leeds to a new Node?
				if (!chain.contains(in[0].getFromNode())) nextNode = in[0].getFromNode();
				else if (!chain.contains(in[1].getFromNode())) nextNode = in[1].getFromNode();
				
				// else: both are already contained -> seems to be cyclic...
				else nextNode = null;
			}
			/*
			 *  Finally add the last previousNode to the Chain. This Node
			 *  is not transformable, so it will become Head or Tail.
			 */
			chain.addFirst(previousNode);
			
			/*
			 *  Finally add the last previousNode to the Chain. This Node
			 *  is not transformable, so it will become Head or Tail.
			 */
			chain.addLast(nextNode);
			
			chains.add(chain);
		}
		return chains;
	}
	
	private void transformChain(LinkedList<Node> chain)
	{
		Node firstNode = chain.getFirst();
		Node lastNode = chain.getLast();
		
		/*
		 * If one or both of them are null then it seems
		 * as the chain is a ring that is not connected to
		 * the network.
		 */
		if (firstNode != null && lastNode != null)
		{
			Node nextNode;
			Node previousNode;
			
			/*
			 * Define the Head in a deterministic way by 
			 * comparing the Ids of the Nodes.
			 */
			boolean firstIsHead;
			if(firstNode.getId().compareTo(lastNode.getId()) < 0)
			{
				firstIsHead = true;
				previousNode = firstNode;
				nextNode = lastNode;
			}
			else
			{
				firstIsHead = false;
				previousNode = lastNode;
				nextNode = firstNode;
			}
			
			if (firstNode != lastNode)
			{
				/*
				 * Create the Lists that are used to document
				 * the mapping.
				 * forward: from PreviousNode to NextNode
				 * backward: from NextNode to PreviousNode
				 */
				LinkedList<Link> list1 = new LinkedList<Link>();
				LinkedList<Link> list2 = new LinkedList<Link>();
				LinkedList<MappingInfo> mappingList1 = new LinkedList<MappingInfo>();
				LinkedList<MappingInfo> mappingList2 = new LinkedList<MappingInfo>();
				Iterator<Node> iter;
				Node n1;
				Node n2;
				
				iter = chain.iterator();
				n1 = iter.next();
				
				while (iter.hasNext())
				{
					n2 = iter.next();
					
					for (Link link : n1.getOutLinks().values())
					{
						if (link.getToNode().equals(n2))
						{
							list1.addLast(link);
							mappingList1.addLast((MappingInfo)link);
							mappingList1.addLast((MappingInfo)n2);
							break;
						}
					}
					/*
					 *  Remove Last Element because because we want 
					 *  a List of Link - Node - ... - Node - Link 
					 */
					mappingList1.removeLast();
					
					for (Link link : n1.getInLinks().values())
					{
						if (link.getFromNode().equals(n2))
						{
							list2.addFirst(link);
							mappingList2.addFirst((MappingInfo)link);
							mappingList2.addFirst((MappingInfo)n2);
							break;
						}
					}
					/*
					 *  Remove First Element because because we want 
					 *  a List of Link - Node - ... - Node - Link 
					 */
					mappingList1.removeFirst();
					
					n1 = n2;
				}			
				
				List<Link> forward = null;
				List<Link> backward = null;
				List<MappingInfo> forwardMapping = null;
				List<MappingInfo> backwardMapping = null;
				if (firstIsHead)
				{
					forward = list1;
					backward = list2;
					forwardMapping = mappingList1;
					backwardMapping = mappingList2;
				}
				else
				{
					forward = list2;
					backward = list1;
					forwardMapping = mappingList2;
					backwardMapping = mappingList1;
				}
				
				MappingLink ml1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), previousNode, nextNode);
				MappingLink ml2 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), nextNode, previousNode);			
				
				// create Mapping Information and add it to the MappingLinks
				Mapping m1 = new ChainMapping(new IdImpl("mapped" + idCounter++), forwardMapping, ml1);
				Mapping m2 = new ChainMapping(new IdImpl("mapped" + idCounter++), backwardMapping, ml2);
				ml1.setDownMapping(m1);
				ml2.setDownMapping(m2);
				for (MappingInfo mappingInfo : forwardMapping) mappingInfo.setUpMapping(m1);
				for (MappingInfo mappingInfo : backwardMapping) mappingInfo.setUpMapping(m2);
				
				// Adding new Links to the Nodes and the Network
				previousNode.addInLink(ml2);
				previousNode.addOutLink(ml1);
				nextNode.addInLink(ml1);
				nextNode.addOutLink(ml2);
				
				network.addLink(ml1);
				network.addLink(ml2);
				
				removeFromSubNetwork(chain, previousNode, nextNode);
			}
			/*
			 * If they are identically then it seems that
			 * the chain is a ring that is connected at this
			 * identical Node to the Network.
			 */
			else
			{	
//				log.warn("Loop..." + chain.size());
				removeFromSubNetwork(chain, previousNode, nextNode);
			}
		}
		else
		{
			log.warn("Seems like we have a ring that is not connected to the Network - what to do?");
			log.warn("ChainSize: " + chain.size());
			
			/*
			 *  We don't want the not replaced Head- and
			 *  TailNodes of the Chain to be removed from
			 *  the SubNetwork so we remove them from the Chain.
			 */
			chain.remove(chain.getFirst());
			chain.remove(chain.getLast());
			
			for (Node node : chain)
			{
				if (node != null) network.getNodes().remove(node.getId());
			}
		}
	}
	
	private void removeFromSubNetwork(LinkedList<Node> chain, Node previousNode, Node nextNode)
	{
		/*
		 *  We don't want the not replaced Head- and
		 *  TailNodes of the Chain to be removed from
		 *  the SubNetwork so we remove them from the Chain.
		 */
		chain.remove(chain.getFirst());
		chain.remove(chain.getLast());

		// remove replaced Nodes
		for(Node n : chain)
		{
			network.getNodes().remove(n.getId());
			
			for(Link link : n.getInLinks().values()) network.getLinks().remove(link.getId());
			for(Link link : n.getOutLinks().values()) network.getLinks().remove(link.getId());
			n.getInLinks().values().clear();
			n.getOutLinks().values().clear();
		}
		
		// remove replaced In- and OutLinks from previousNode and nextNode
		for (Iterator<? extends Link> iterator = previousNode.getOutLinks().values().iterator(); iterator.hasNext();)
		{
			Link link = iterator.next();
			if (chain.contains(link.getToNode())) iterator.remove();
		}

		for (Iterator<? extends Link> iterator = previousNode.getInLinks().values().iterator(); iterator.hasNext();)
		{
			Link link = iterator.next();
			if (chain.contains(link.getFromNode())) iterator.remove();
		}

		for (Iterator<? extends Link> iterator = nextNode.getOutLinks().values().iterator(); iterator.hasNext();)
		{
			Link link = iterator.next();
			if (chain.contains(link.getToNode())) iterator.remove();
		}

		for (Iterator<? extends Link> iterator = nextNode.getInLinks().values().iterator(); iterator.hasNext();)
		{
			Link link = iterator.next();
			if (chain.contains(link.getFromNode())) iterator.remove();
		}

	}
	
	/*
	 * Use Chains. Define Head and Tail by comparing their Ids to ensure determinism!
	 */
	public void doTransformation()
	{	
		List<LinkedList<Node>> chains = getChains();
		
		// for all found Chains
		for (LinkedList<Node> chain : chains)
		{
			transformChain(chain);
		}
	
		if (printStatistics) log.info("New NodeCount: " + network.getNodes().size());
		if (printStatistics) log.info("new LinkCount: " + network.getLinks().size());
	}
}
