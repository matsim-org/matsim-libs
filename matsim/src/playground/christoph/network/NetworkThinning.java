package playground.christoph.network;

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
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.Time;

import playground.christoph.knowledge.container.MapKnowledge;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.network.util.SubNetworkCreator;

/*
 * Find links that are not used in an empty Network because they
 * are too expensive. Removing them should speed up creating the
 * List of known Nodes of a Person. The Knowledge should not be
 * influenced by this because it contains only Nodes and not Links.
 */

public class NetworkThinning {

	private static final Logger log = Logger.getLogger(NetworkThinning.class);
	
	private NetworkLayer network;
	private SubNetwork subNetwork;
	
	private Map<Id, Node> transformableNodes;
	private Map<Id, Node> nodesToTransform;
	
	private int numOfThreads = 2;
	
	int idCounter = 0;
	
	public static void main(String[] args)
	{
		// create Config
		Config config = new Config();
		config.addCoreModules();
		config.checkConsistency();
		Gbl.setConfig(config);
		
		// load Network
		//String networkFile = "mysimulations/kt-zurich/input/network.xml";
		String networkFile = "mysimulations/kt-zurich-cut/network.xml";
		NetworkLayer nw = new NetworkLayer();
		new MatsimNetworkReader(nw).readFile(networkFile);
		
		log.info("Network has " + nw.getNodes().size() + " Nodes.");
		log.info("Network has " + nw.getLinks().size() + " Links.");
		
		NetworkThinning ntfd = new NetworkThinning();
		ntfd.setNetwork(nw);
		ntfd.createSubNetwork();
		
		//ntfd.findNodesMultiThread();
	
		for (int i = 0; i < 10; i++)
		{
			log.info("Iteration " + (i + 1));
			ntfd.findTriNodes();
			ntfd.selectTriNodes();
			ntfd.transformTriNodes();
			
			ntfd.transformDuplicatedLinks(ntfd.findDuplicatedLinks());
			
			ntfd.findDuoNodes();
			ntfd.transformDuoNodes();	
		}
	}
	
	public void setNetwork(NetworkLayer network)
	{
		this.network = network;
	}
	
	public NetworkLayer getNetwork()
	{
		return this.network;
	}
	
	public void createSubNetwork()
	{
		SubNetworkCreator snc = new SubNetworkCreator(network);
		
		NodeKnowledge nodeKnowledge = new MapKnowledge();
		nodeKnowledge.setNetwork(network);
		nodeKnowledge.setKnownNodes(network.getNodes());
		
		subNetwork = snc.createSubNetwork(nodeKnowledge);
	}
	
	public void findNodesMultiThread()
	{
		ThinningThread[] thinningThreads = new ThinningThread[numOfThreads];
		
		// split up the Nodes to distribute the workload between the threads
		List<List<NodeImpl>> nodeLists = new ArrayList<List<NodeImpl>>();
		for (int i = 0; i < numOfThreads; i++)
		{
			nodeLists.add(new ArrayList<NodeImpl>());
		}
		
		int i = 0;
		for (NodeImpl node : this.network.getNodes().values())
		{
			nodeLists.get(i % numOfThreads).add(node);
			i++;
		}
		
		// init the Threads
		for (int j = 0; j < thinningThreads.length; j++)
		{
			thinningThreads[j] = new ThinningThread(network, nodeLists.get(j));
			thinningThreads[j].setName("ThinningThread#" + i);
		}
		
		// start the Threads
		for (ThinningThread thinningThread : thinningThreads)
		{
			thinningThread.start();
		}
		
		// wait until the Thread are finished
		try {
			for (ThinningThread thinningThread : thinningThreads) 
			{
				thinningThread.join();
			}
		} 
		catch (InterruptedException e)
		{
			log.error(e.getMessage());
		}
		
		// get Nodes that can be transformed from the Threads
		//transformableNodes = new HashMap<Id, Node>();
		transformableNodes = new TreeMap<Id, Node>(new TriangleNodeComparator(network.getNodes()));
		
		for (ThinningThread thinningThread : thinningThreads) 
		{
			transformableNodes.putAll(thinningThread.getTransformableNodes());
		}
		
		log.info("Network has " + network.getLinks().size() + " Links.");
		log.info("Not used links: " + transformableNodes.size());
	}
	
	public void findDuoNodes()
	{
		transformableNodes = new TreeMap<Id, Node>();
		
		int nodeCount = 0;
		
		// for every Node of the Network
		for (Node node : subNetwork.getNodes().values())
		{
			nodeCount++;
			if (nodeCount % 1000 == 0)
			{
				log.info("NodeCount: " + nodeCount + ", transformable Nodes: " + transformableNodes.size());
			}
			
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
				transformableNodes.put(node.getId(), node);
			}
		}
		
		log.info("Transformable Nodes: " + transformableNodes.size());
	}
	
	/*
	 * Find Links with the given Number of In- and OutLinks
	 * A pair of In- and Outlink to the same Node is counted as single Link! 
	 */
	public void findTriNodes()
	{
		transformableNodes = new TreeMap<Id, Node>(new TriangleNodeComparator(subNetwork.getNodes()));
		
		int nodeCount = 0;
		
		// for every Node of the Network
		for (Node node : subNetwork.getNodes().values())
		{
			nodeCount++;
			if (nodeCount % 1000 == 0)
			{
				log.info("NodeCount: " + nodeCount + ", transformable Nodes: " + transformableNodes.size());
			}
			
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
				transformableNodes.put(node.getId(), node);
			}
		}
		
		log.info("Transformable Nodes: " + transformableNodes.size());
	}
	
	/*
	 * Searches for Nodes that are directly connected by more than
	 * one Link. This may occur in given input networks or after
	 * transforming the network.
	 */
	//[TODO]
	
	public List<List<Link>> findDuplicatedLinks()
	{
		int nodeCount = 0;
		List<List<Link>> duplicatedLinks = new ArrayList<List<Link>>();
		
		for (Node node : subNetwork.getNodes().values())
		{
			nodeCount++;
			if (nodeCount % 1000 == 0)
			{
				log.info("NodeCount: " + nodeCount + ", duplicated Links: " + duplicatedLinks.size());
			}
			
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
			 * Now add the Lists with multiple Entry to the List
			 * with the duplicated Links.
			 */
			for (List<Link> linkList : map.values())
			{
				if(linkList.size() > 1) duplicatedLinks.add(linkList);
			}
			
//			List<Node> toNodes = new ArrayList<Node>();
//			for (Link link : node.getOutLinks().values())
//			{
//				Node toNode = link.getToNode();
//				if (toNodes.contains(toNode))
//				{
//					duplicatedLinks++;
//					duplicated.add(link);
//				}
//				else toNodes.add(toNode);
//			}
		}
		
//		for (Link link : duplicated)
//		{
//			subNetwork.getLinks().remove(link.getId());
//			link.getFromNode().getOutLinks().remove(link.getId());
//			link.getToNode().getInLinks().remove(link.getId());
//		}
		
		log.info("Duplicated Links: " + duplicatedLinks.size());
		
		return duplicatedLinks;
	}
	
	public void transformDuplicatedLinks(List<List<Link>> duplicatedLinks)
	{
		for (List<Link> duplicates : duplicatedLinks)
		{
			Node fromNode = duplicates.get(0).getFromNode();
			Node toNode = duplicates.get(0).getToNode();
						
			MappingLink ml1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), fromNode, toNode, duplicates);
			
			fromNode.addOutLink(ml1);
			toNode.addInLink(ml1);
			subNetwork.addSubLink(ml1);
			
			for (Link link : duplicates)
			{
				subNetwork.getLinks().remove(link.getId());
				link.getFromNode().getOutLinks().remove(link.getId());
				link.getToNode().getInLinks().remove(link.getId());
			}
		}
		
		log.info("New NodeCount: " + subNetwork.getNodes().size());
		log.info("new LinkCount: " + subNetwork.getLinks().size());
	}
	
	/*
	 * Select those Duo-Nodes that are really gonna be transformed
	 * Currently we don't have criteria to filter - we transform all of them
	 */
//	public void selectDuoNodes()
//	{
//		nodesToTransform = new HashMap<Id, Node>();
//		
//		for (Node node : transformableNodes.values())
//		{
//			for (Link link : node.getOutLinks().values())
//			{
//				Node toNode = link.getToNode();
//			}
//		}
//		Entry<Id, Node> entry;
//		while((entry = ((TreeMap<Id, Node>)transformableNodes).pollFirstEntry()) != null)
//		{
//			nodesToTransform.put(entry.getKey(), entry.getValue());
//			
//			Node node = entry.getValue();
//			
//			for (Link link : node.getOutLinks().values())
//			{
//				Node toNode = link.getToNode();
//				transformableNodes.remove(toNode.getId());
//			}
//		}
//		
//		log.info("Nodes to Transform: " + nodesToTransform.size());
//	}
	
	public void transformDuoNodes()
	{		
/*		
		Entry<Id, Node> entry;
		while((entry = ((TreeMap<Id, Node>)transformableNodes).pollFirstEntry()) != null)
		{
			LinkedList<Node> nodesList = new LinkedList<Node>();
			
			Node node = entry.getValue();
		
			nodesList.add(node);
			
			transformableNodes.remove(node.getId());
			
			// get current Links
			Link[] inLinks = new Link[node.getInLinks().size()];
			node.getInLinks().values().toArray(inLinks);
			
			Link[] outLinks  = new Link[node.getOutLinks().size()]; 
			node.getOutLinks().values().toArray(outLinks);
			
			Node previousNode = inLinks[0].getFromNode();
			Node nextNode = inLinks[1].getFromNode();
				
			Node previous = previousNode;
			while(transformableNodes.containsKey(previous.getId()))
			{
				transformableNodes.remove(previous.getId());
				nodesList.addFirst(previous);
				
				Link[] in = new Link[2];
				previous.getInLinks().values().toArray(in);
				
				if (previous.getInLinks().size() != 2)
				{
					log.warn("Wrong length...");
				}
				if (in[0] == null) log.warn("0 null");
				if (in[1] == null) log.warn("1 null");
				
				// Which of the two inLinks leeds to a new Node?
				if (!nodesList.contains(in[0].getFromNode())) previous = in[0].getFromNode();
				else if (!nodesList.contains(in[1].getFromNode())) previous = in[1].getFromNode();
				
				// else: both are already contained -> seems to be cyclic...
				else previous = null;
			}
			// previous should point to the FromNode of the replacing Link
			previousNode = previous;
			
			Node next = nextNode;
			while(transformableNodes.containsKey(next.getId()))
			{
				transformableNodes.remove(next.getId());
				nodesList.addLast(next);
				
				Link[] in = new Link[2];
				next.getInLinks().values().toArray(in);
				
				if (next.getInLinks().size() != 2)
				{
					log.warn("Wrong length...");
				}
				if (in[0] == null) log.warn("0 null");
				if (in[1] == null) log.warn("1 null");

				in[0].getFromNode();
				nodesList.contains(in[0].getFromNode());
				// Which of the two inLinks leeds to a new Node?
				if (!nodesList.contains(in[0].getFromNode())) next = in[0].getFromNode();
				else if (!nodesList.contains(in[1].getFromNode())) next = in[1].getFromNode();
				
				// else: both are already contained -> seems to be cyclic...
				else next = null;
			}
			// next should point to the ToNode of the replacing Link
			nextNode = next;
			
			if (previousNode != null && nextNode != null)
			{
				// if it is no loop
				if (previousNode != nextNode)
				{
					List<Link> forward = new ArrayList<Link>();
					List<Link> backward = new ArrayList<Link>();
					// [TODO] fill Lists for Reverse Mapping
					
					MappingLink ml1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), previousNode, nextNode, forward);
					MappingLink ml2 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), nextNode, previousNode, backward);			
					
					// Adding new Links to the Nodes and the Network
					previousNode.addInLink(ml2);
					previousNode.addOutLink(ml1);
					nextNode.addInLink(ml1);
					nextNode.addOutLink(ml2);
					
					subNetwork.addSubLink(ml1);
					subNetwork.addSubLink(ml2);
					
					// remove replaced Nodes
					for(Node n : nodesList)
					{
						subNetwork.getNodes().remove(n.getId());
						
						for(Link link : n.getInLinks().values()) subNetwork.getLinks().remove(link.getId());
						for(Link link : n.getOutLinks().values()) subNetwork.getLinks().remove(link.getId());
						n.getInLinks().values().clear();
						n.getOutLinks().values().clear();
					}
					
					// remove replaced In- and OutLinks from previousNode and nextNode
					for (Iterator<? extends Link> iterator = previousNode.getOutLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getToNode())) iterator.remove();
					}

					for (Iterator<? extends Link> iterator = previousNode.getInLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getFromNode())) iterator.remove();
					}

					for (Iterator<? extends Link> iterator = nextNode.getOutLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getToNode())) iterator.remove();
					}

					for (Iterator<? extends Link> iterator = nextNode.getInLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getFromNode())) iterator.remove();
					}
				}
				//[TODO] How to handle a loop?
				// At the moment: remove them...
				else
				{
//					log.warn("Loop - do nothing...");
					// remove replaced Nodes
					for(Node n : nodesList)
					{
						subNetwork.getNodes().remove(n.getId());
						
						for(Link link : n.getInLinks().values()) subNetwork.getLinks().remove(link.getId());
						for(Link link : n.getOutLinks().values()) subNetwork.getLinks().remove(link.getId());
						n.getInLinks().values().clear();
						n.getOutLinks().values().clear();
					}
					
					// remove replaced In- and OutLinks from previousNode and nextNode
					for (Iterator<? extends Link> iterator = previousNode.getOutLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getToNode())) iterator.remove();
					}

					for (Iterator<? extends Link> iterator = previousNode.getInLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getFromNode())) iterator.remove();
					}

					for (Iterator<? extends Link> iterator = nextNode.getOutLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getToNode())) iterator.remove();
					}

					for (Iterator<? extends Link> iterator = nextNode.getInLinks().values().iterator(); iterator.hasNext();)
					{
						Link link = iterator.next();
						if (nodesList.contains(link.getFromNode())) iterator.remove();
					}

				}
			}
			else
			{
				log.error("Something seems to be wrong here...");
			}
		}
		
//		log.info("Nodes to Transform: " + nodesToTransform.size());
*/
		log.info("New NodeCount: " + subNetwork.getNodes().size());
		log.info("new LinkCount: " + subNetwork.getLinks().size());
	}
	
	/*
	 * Select those Tri-Nodes that are really gonna be transformed
	 */
	public void selectTriNodes()
	{
		nodesToTransform = new HashMap<Id, Node>();
/*				
		Entry<Id, Node> entry;
		while((entry = ((TreeMap<Id, Node>)transformableNodes).pollFirstEntry()) != null)
		{
			nodesToTransform.put(entry.getKey(), entry.getValue());
			
			Node node = entry.getValue();
			
			for (Link link : node.getOutLinks().values())
			{
				Node toNode = link.getToNode();
				transformableNodes.remove(toNode.getId());
			}
		}
*/		
		log.info("Nodes to Transform: " + nodesToTransform.size());
	}
	
	public void transformTriNodes()
	{
//		int i = 0;
		for(Node node : nodesToTransform.values())
		{
			transformTriNode(node);
			
//			i++;
//			if (i > 1000) break;
//			Node nodeToTransform = subNetwork.getNodes().get(node.getId());
//			transformTriNode(nodeToTransform);
		}
		
		log.info("New NodeCount: " + subNetwork.getNodes().size());
		log.info("new LinkCount: " + subNetwork.getLinks().size());
	}
	
	/* 	
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
	 * AB = 14 = m1
	 * BA = 32 = m2
	 * AC = 16 = m3
	 * CA = 52 = m4
	 * BC = 36 = m5
	 * CB = 54 = m6
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
	 */
	//private void transformTriNode(Node node, Network network)
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

		m1 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), A, B, l1, l4);
		m2 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), B, A, l3, l2);
		m3 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), A, C, l1, l6);
		m4 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), C, A, l5, l2);
		m5 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), B, C, l3, l6);
		m6 = new MappingLinkImpl(new IdImpl("mapped" + idCounter++), C, B, l5, l4);
		
		subNetwork.addSubLink(m1);
		subNetwork.addSubLink(m2);
		subNetwork.addSubLink(m3);
		subNetwork.addSubLink(m4);
		subNetwork.addSubLink(m5);
		subNetwork.addSubLink(m6);
		//Map<Id, ? extends Link> networkLinks = network.getLinks();
		//networkLinks.put(m1.getId(), (MappingLinkImpl) m1);
		
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
				
		subNetwork.getLinks().remove(l1.getId());
		subNetwork.getLinks().remove(l2.getId());
		subNetwork.getLinks().remove(l3.getId());
		subNetwork.getLinks().remove(l4.getId());
		subNetwork.getLinks().remove(l5.getId());
		subNetwork.getLinks().remove(l6.getId());
		
		subNetwork.getNodes().remove(node.getId());
	}
	
//	/*
//	 * Calculates the Angle between the vectors ab and ac.
//	 */
//	private double calcAngle(NodeImpl a, NodeImpl b, NodeImpl c)
//	{
//		double ABx = b.getCoord().getX() - a.getCoord().getX();
//		double ABy = b.getCoord().getY() - a.getCoord().getY();	
//
//		double ACx = c.getCoord().getX() - a.getCoord().getX();
//		double ACy = c.getCoord().getY() - a.getCoord().getY();
//		
//		double cosAlpha = (ABx * ACx + ABy * ACy) / (Math.sqrt(ABx*ABx + ABy*ABy) * Math.sqrt(ACx*ACx + ACy*ACy));
//		
//		return Math.acos(cosAlpha);
//	}
	
	private boolean subNodesConnected(Node a, Node b, Node c)
	{
		//for(NodeImpl node : a.getOutNodes().values())
		for (Link link : a.getOutLinks().values())
		{
			Node node = link.getToNode();
			if (node.equals(b) || node.equals(c)) return true;
		}

		//for(NodeImpl node : b.getOutNodes().values())
		for (Link link : b.getOutLinks().values())
		{
			Node node = link.getToNode();
			if (node.equals(a) || node.equals(c)) return true;
		}
		
		//for(NodeImpl node : c.getOutNodes().values())
		for (Link link : c.getOutLinks().values())
		{
			Node node = link.getToNode();
			if (node.equals(a) || node.equals(b)) return true;
		}
		
		return false;
	}
	
	/*
	 * returns 1 if the point is inside the triangle
	 * returns 0 if the point is on one border of the triangle
	 * returns -1 if the point is outside the triangle
	 */
	private int calcInTriangle(Node a, Node b, Node c, Node point)
	{
		double det1 = calcDeterminant(a, b, point);
		double det2 = calcDeterminant(b, c, point);
		double det3 = calcDeterminant(c, a, point);
		
		if (det1 == 0.0 || det2 == 0.0 || det3 == 0.0) return 0;
		
		if (det1 > 0 && det2 > 0 && det3 > 0) return 1;
		if (det1 < 0 && det2 < 0 && det3 < 0) return 1;
		
		return -1;
	}
	
	/*
	 * used by calcInTriangle(...)
	 */
	private double calcDeterminant(Node start, Node end, Node point)
	{
		double[] row1 = new double[]{start.getCoord().getX(), start.getCoord().getY(), 1};
		double[] row2 = new double[]{end.getCoord().getX(),   end.getCoord().getY(),   1};
		double[] row3 = new double[]{point.getCoord().getX(), point.getCoord().getY(), 1};
		
		double det = row1[0]*row2[1]*row3[2] + row1[1]*row2[2]*row3[0] + row1[2]*row2[0]*row3[1] -
					 row1[2]*row2[1]*row3[0] - row1[1]*row2[0]*row3[2] - row1[0]*row2[2]*row3[1];
		                                                                                      
		return det;
	}
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class ThinningThread extends Thread
	{
		private NetworkLayer network;
		
		private Map<Id, NodeImpl> nodesToTransform;
		private List<NodeImpl> nodes;
		
		private double time = Time.UNDEFINED_TIME;
		private int thread;
		
		private static int threadCounter = 0;
		
		public ThinningThread(NetworkLayer network, List<NodeImpl> nodes)
		{
			this.network = network;
			this.nodes = nodes;
			this.thread = threadCounter++;
		}

		@Override
		public void run()
		{
			findNodes();	
		}
		
		public Map<Id, NodeImpl> getTransformableNodes()
		{
			return this.nodesToTransform;
		}
		
		private void findNodes()
		{
			nodesToTransform = new HashMap<Id, NodeImpl>();
			
			int nodeCount = 0;
			
			// for every Node of the given List
			for (NodeImpl node : nodes)
			{
				List<NodeImpl> outNodes = new ArrayList<NodeImpl>();
				
				for (LinkImpl outLink : node.getOutLinks().values())
				{
					outNodes.add(outLink.getToNode());
				}
								
				nodeCount++;
				if (nodeCount % 1000 == 0)
				{
					log.info("Thread: " + thread + ", NodeCount: " + nodeCount + ", not used Links: " + nodesToTransform.size());
				}
			}
		}
	}
	
	public class TriangleNodeComparator implements Comparator<Id>, Serializable {

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
				else return n1.getId().compareTo(n2.getId());
			}	
		}

	}
}
