package playground.christoph.network.transformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * Transforms Nodes with exactly three In- and OutLinks.
 * The Transformation bases on the so call Star-Triangle-
 * Transformation known from electrotechnics.  
 * Current:
 * 	
 * 		C
 * 	   5|6
 * 		o
 * 	 1/2 3\4
 * 	A		B
 * 
 * 			 C
 * 		   5| |6
 * 			| |
 * 			 O
 * 		 / /  \ \
 * 	   1/ /2  3\ \4
 * 		A		 B
 * 
 * AO = 1
 * OA = 2
 * BO = 3
 * OB = 4
 * CO = 5
 * OC = 6
 * 
 * Mapping:
 * 
 * 	 	 / C \
 *  	/ / \ \
 * 	   / /   \ \
 * 	  / /     \ \
 * 	 / /       \ \
 * 	/ /_________\ \
 * 	A ___________ B
 * 
 * AB = 14 = m1
 * BA = 32 = m2
 * AC = 16 = m3
 * CA = 52 = m4
 * BC = 36 = m5
 * CB = 54 = m6
 */
public class TransformTriNodes extends TransformationImpl{

	private static final Logger log = Logger.getLogger(TransformTriNodes.class);
	
	public TransformTriNodes(SubNetwork subNetwork)
	{
		this.network = subNetwork;
		this.transformableNodes = new TreeMap<Id, Node>(new TriangleNodeComparator(subNetwork.getNodes()));
	}
	
	public void doTransformation()
	{
		for(Node node : getTransformableStructures().values())
		{
			transformTriNode(node);
		}
		
		if (printStatistics) log.info("New NodeCount: " + network.getNodes().size());
		if (printStatistics) log.info("new LinkCount: " + network.getLinks().size());
	}

	private void transformTriNode(Node node)
	{	
		// get current Links
		Link[] inLinks = new Link[node.getInLinks().size()];
		node.getInLinks().values().toArray(inLinks);
		
		Link[] outLinks  = new Link[node.getOutLinks().size()]; 
		node.getOutLinks().values().toArray(outLinks);
		
		// get Nodes
		Node A = outLinks[0].getToNode();
		Node B = outLinks[1].getToNode();
		Node C = outLinks[2].getToNode();

		// get Links
		Link l1 = null;
		Link l2 = null;
		Link l3 = null;
		Link l4 = null;
		Link l5 = null;
		Link l6 = null;

		for(Link link : inLinks)
		{
			if (link.getFromNode().equals(A)) l1 = link;
			else if (link.getFromNode().equals(B)) l3 = link;
			else l5 = link;
		}
		for (Link link : outLinks)
		{
			if (link.getToNode().equals(A)) l2 = link;
			else if (link.getToNode().equals(B)) l4 = link;
			else l6 = link;
		}
		
		// create new Links
		MappingLink m1 = null;
		MappingLink m2 = null;
		MappingLink m3 = null;
		MappingLink m4 = null;
		MappingLink m5 = null;
		MappingLink m6 = null;

//		m1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), A, B, l1, l4);
//		m2 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), B, A, l3, l2);
//		m3 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), A, C, l1, l6);
//		m4 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), C, A, l5, l2);
//		m5 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), B, C, l3, l6);
//		m6 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), C, B, l5, l4);

		m1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), A, B);
		m2 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), B, A);
		m3 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), A, C);
		m4 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), C, A);
		m5 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), B, C);
		m6 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), C, B);

		// create and set Mappings
		List<MappingInfo> list1 = new ArrayList<MappingInfo>();
		List<MappingInfo> list2 = new ArrayList<MappingInfo>();
		List<MappingInfo> list3 = new ArrayList<MappingInfo>();
		List<MappingInfo> list4 = new ArrayList<MappingInfo>();
		List<MappingInfo> list5 = new ArrayList<MappingInfo>();
		List<MappingInfo> list6 = new ArrayList<MappingInfo>();
		
		list1.add((MappingInfo)l1); list1.add((MappingInfo)node); list1.add((MappingInfo)l4);
		list2.add((MappingInfo)l3); list2.add((MappingInfo)node); list2.add((MappingInfo)l2);
		list3.add((MappingInfo)l1); list3.add((MappingInfo)node); list3.add((MappingInfo)l6);
		list4.add((MappingInfo)l5); list4.add((MappingInfo)node); list4.add((MappingInfo)l2);
		list5.add((MappingInfo)l3); list5.add((MappingInfo)node); list5.add((MappingInfo)l6);
		list6.add((MappingInfo)l5); list6.add((MappingInfo)node); list6.add((MappingInfo)l4);
		
		Mapping mapping1 = new ChainMapping(new IdImpl("mapped" + idCounter++), list1 , m1);
		Mapping mapping2 = new ChainMapping(new IdImpl("mapped" + idCounter++), list2 , m2);
		Mapping mapping3 = new ChainMapping(new IdImpl("mapped" + idCounter++), list3 , m3);
		Mapping mapping4 = new ChainMapping(new IdImpl("mapped" + idCounter++), list4 , m4);
		Mapping mapping5 = new ChainMapping(new IdImpl("mapped" + idCounter++), list5 , m5);
		Mapping mapping6 = new ChainMapping(new IdImpl("mapped" + idCounter++), list6 , m6);
		
		m1.setDownMapping(mapping1);
		m2.setDownMapping(mapping2);
		m3.setDownMapping(mapping3);
		m4.setDownMapping(mapping4);
		m5.setDownMapping(mapping5);
		m6.setDownMapping(mapping6);

		// no further explicit upMapping possible
		((MappingInfo)l1).setUpMapping(null);
		((MappingInfo)l2).setUpMapping(null);
		((MappingInfo)l3).setUpMapping(null);
		((MappingInfo)l4).setUpMapping(null);
		((MappingInfo)l5).setUpMapping(null);
		((MappingInfo)l6).setUpMapping(null);
		((MappingInfo)node).setUpMapping(null);
		
		network.addLink(m1);
		network.addLink(m2);
		network.addLink(m3);
		network.addLink(m4);
		network.addLink(m5);
		network.addLink(m6);
		
		// connect new Links to Nodes
		A.addInLink(m2);
		A.addInLink(m4);
		B.addInLink(m1);
		B.addInLink(m6);
		C.addInLink(m3);
		C.addInLink(m5);

		A.addOutLink(m1);
		A.addOutLink(m3);
		B.addOutLink(m2);
		B.addOutLink(m5);
		C.addOutLink(m4);
		C.addOutLink(m6);

		// remove old Links
		A.getOutLinks().remove(l1.getId());
		A.getInLinks().remove(l2.getId());
		B.getOutLinks().remove(l3.getId());
		B.getInLinks().remove(l4.getId());
		C.getOutLinks().remove(l5.getId());
		C.getInLinks().remove(l6.getId());
				
		network.getLinks().remove(l1.getId());
		network.getLinks().remove(l2.getId());
		network.getLinks().remove(l3.getId());
		network.getLinks().remove(l4.getId());
		network.getLinks().remove(l5.getId());
		network.getLinks().remove(l6.getId());
		
		network.getNodes().remove(node.getId());
	}
	
	public void findTransformableStructures() 
	{		
//		int nodeCount = 0;
		
		// for every Node of the Network
		for (Node node : network.getNodes().values())
		{
//			nodeCount++;
//			if (nodeCount % 1000 == 0)
//			{
//				log.info("NodeCount: " + nodeCount + ", transformable Nodes: " + transformableNodes.size());
//			}
			
			Map<Id, Node> inNodes = new TreeMap<Id, Node>();
			for (Link link : node.getInLinks().values())
			{
				Node fromNode = link.getFromNode();
				inNodes.put(fromNode.getId(), fromNode);
			}
			
			Map<Id, Node> outNodes = new TreeMap<Id, Node>();
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
			if (inNodes.size() != 3) continue;

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
			
//			// check whether the point is outside the triangle defined by the other three nodes
//			Node[] angleNodes = inNodes.values().toArray(new Node[3]);
//			int inTriangle = calcInTriangle(angleNodes[0], angleNodes[1], angleNodes[2], node);
//			if (inTriangle == -1) transformable = false;
//
//			if (subNodesConnected(angleNodes[0], angleNodes[1], angleNodes[2])) transformable = false;			
						
			if (transformable)
			{
				getTransformableStructures().put(node.getId(), node);
			}
		}
		
		if (printStatistics) log.info("Transformable Nodes: " + transformableNodes.size());
	}
	
	@SuppressWarnings("unchecked")
	public Map<Id, Node> getTransformableStructures()
	{
		return (Map<Id, Node>) this.transformableNodes;
	}

	/*
	 * Try to transform the Nodes by following their order given
	 * by the TriangleNodeComparator.
	 */
	public void selectTransformableStructures() 
	{
		// Nodes that will be removed.
		List<Node> nodesToRemove = new ArrayList<Node>();
		
		Iterator<Node> iter = getTransformableStructures().values().iterator();
		
		while(iter.hasNext())
		{
			Node node = iter.next();
			
			/*
			 * Remove the Node if it can't be processed anymore.
			 * Otherwise mark its surrounding Nodes as not processable.
			 */
			if (nodesToRemove.contains(node))
			{
				iter.remove();
			}
			else
			{
				for (Link link : node.getOutLinks().values())
				{
					Node toNode = link.getToNode();
					nodesToRemove.add(toNode);
				}
			}
		}
				
		if (printStatistics) log.info("Selected Nodes for Transformation: " + transformableNodes.size());
	}
	
	/*
	 * Order the Nodes by the following criteria:
	 * 1) Shortest Link. The shorter the earlier.
	 * otherwise:
	 * 2) Sum of the Length of the Links. The shorter the earlier.
	 * otherwise:
	 * 3) Ids of the Nodes.
	 * 
	 * These criteria guarantee a deterministic behavior.
	 */
	private class TriangleNodeComparator implements Comparator<Id>, Serializable {

		private static final long serialVersionUID = 1L;
		private Map<Id, ? extends Node> nodes;
		
		public TriangleNodeComparator(Map<Id, ? extends Node> nodes)
		{
			this.nodes = nodes;
		}
	
		public int compare(final Id Id1, final Id Id2) 
		{
			Node n1 = nodes.get(Id1);
			Node n2 = nodes.get(Id2);
				
			double n1Length = 0;
			double n1Min = Double.MAX_VALUE;
			for (Link link : n1.getInLinks().values())
			{
				double length = link.getLength();
				
				if (length < n1Min) n1Min = link.getLength();
				
				n1Length = n1Length + length;
			}

			double n2Length = 0;
			double n2Min = Double.MAX_VALUE;
			for (Link link : n2.getInLinks().values())
			{
				double length = link.getLength();
				
				if (length < n2Min) n2Min = link.getLength();
				
				n2Length = n2Length + length;
			}
			
			// compare shortest Links
			if (n1Min < n2Min) return -1;
			else if (n1Min > n2Min) return 1;
			else 
			{
				// compare summarized Link Length
				if (n1Length < n2Length) return -1;
				else if (n1Length > n2Length) return 1;
				else return Id1.compareTo(Id2);
			}	
		}
	}
}