/******************************************************************************
 *project: org.matsim.*
 * BlockCreator.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

import playground.rost.graph.block.Block;
import playground.rost.graph.evacarea.EvacArea;
import playground.rost.graph.flatnetwork.FlatNetwork;
import playground.rost.graph.shortestdistances.CostFunction;
import playground.rost.graph.shortestdistances.LengthCostFunction;
import playground.rost.graph.shortestdistances.ShortestDistanceFromSupersource;
import playground.rost.util.PathTracker;

public class BlockCreator {
	
	private static final Logger log = Logger.getLogger(BlockCreator.class);
		
	public NetworkLayer network;
	
	public Map<Node, Integer> demandMap = new HashMap<Node, Integer>();
	
	public Map<Long, Set<Block>> hashBlocks = new HashMap<Long, Set<Block>>();
	
	public Set<Block> blocks = new HashSet<Block>();
	
	public List<Node> borderNodes = new LinkedList<Node>();
	
	public Set<Id> borderLinkIds = new HashSet<Id>();
	
	public Set<Node> noStartSet = new HashSet<Node>();
	
	protected EvacArea evacArea;
	
	protected FlatNetwork flatNetwork = new FlatNetwork();
	
	public double areaBlockSum;
	public double areaBorder;
	
	public BlockCreator(NetworkLayer network)
	{
		PropertyConfigurator.configure(PathTracker.resolve("distriLogger"));
		this.network = network;
		setupNetwork();
		parseBlocks();
		calcAreaStatistics();
	}
	
	protected void setupNetwork()
	{
		//add border information
		addBorderInformation();
		addBorderLinks();
		removeUnreachableNodes();
		this.network = flatNetwork.flatten(network);
		updateBorderInformation();
	}
	
	protected void updateBorderInformation()
	{
		//first: update the border:
		evacArea.evacBorderOrderIds = getNewBorderOrder();
		List<Node> hull = new LinkedList<Node>();
		for(String s : evacArea.evacBorderOrderIds)
		{
			hull.add(network.getNodes().get(new IdImpl(s)));
		}
		
		//update information about newly created nodes
		for(String onBorder : evacArea.evacBorderOrderIds)
		{
			if(evacArea.evacBorderNodeIds.contains(onBorder))
				continue;
			evacArea.evacAreaNodeIds.add(onBorder);
		}
		for(Node node : network.getNodes().values())
		{
			String id = node.getId().toString();
			if(evacArea.evacAreaNodeIds.contains(id))
			{
				continue;
			}
			if(evacArea.evacBorderOrderIds.contains(id) || GraphAlgorithms.pointIsInPolygon(hull, node))
			{
				evacArea.evacAreaNodeIds.add(id);
			}
			else
			{
				evacArea.evacBorderNodeIds.add(id);
			}
		}
		noStartSet.clear();
		//add every node on the border to the noStartSet
		
		//first: add the nodes on the border
		for(String id : evacArea.evacBorderOrderIds)
		{
			noStartSet.add(network.getNodes().get(new IdImpl(id)));
		}
		
		//second: add the nodes outside the border
		for(String id : evacArea.evacBorderNodeIds)
		{
			noStartSet.add(network.getNodes().get(new IdImpl(id)));
		}
	}
	
	protected List<String> getNewBorderOrder()
	{
		refreshBorderLinkIds();
		
		List<String> newBorderOrder = new LinkedList();
		
		String startString = evacArea.evacBorderOrderIds.get(0);
		Node start = network.getNodes().get(new IdImpl(startString));
		newBorderOrder.add(startString);
		
		Node current = start;
		Node next = null;
		
		while(true)
		{
			next = null;
			//search for a link starting with this node
			for(Id id : borderLinkIds)
			{
				Link l = network.getLinks().get(id);
				if(l == null)
					continue;
				Node tmp = l.getToNode();
				if(l.getFromNode().equals(current) && ((!newBorderOrder.contains(tmp.getId().toString()) || (tmp.equals(start) && newBorderOrder.size() > 2))))
				{
					next = tmp;
					break;
				}
			}
			if(next == null)
			{
				throw new RuntimeException("Border could not be reconstructed..");
			}
			if(next.equals(start))
			{
				break;
			}
			else
			{
				String s = next.getId().toString();
				newBorderOrder.add(s);
				current = next;
			}
		}
		return newBorderOrder;
	}

	
	
	
	protected void refreshBorderLinkIds()
	{
		Set<Id> newborderLinkIds = new HashSet<Id>();
		Set<Id> newLinksForOldLink = new HashSet<Id>();
		for(Id oldId : borderLinkIds)
		{
			newLinksForOldLink = flatNetwork.splittedLinks.get(oldId);
			if(newLinksForOldLink == null)
			{
				//add manually, id is the same
				newborderLinkIds.add(oldId);
			}
			else
			{
				for(Id newLinkId : newLinksForOldLink)
				{
					newborderLinkIds.add(newLinkId);
				}
			}	
		}
		this.borderLinkIds = newborderLinkIds;
	}
	
	protected void calcAreaStatistics()
	{
		areaBorder = GraphAlgorithms.getSimplePolygonArea(borderNodes);
		areaBlockSum = 0;
		for(Block b : blocks)
		{
			areaBlockSum += GraphAlgorithms.getSimplePolygonArea(b.border);
		}
		log.debug("AREA STATISTICS!");
		log.debug("totalArea (according to bounding box of border): " + areaBorder);
		log.debug("blockSum: " + areaBlockSum);
	}	
	
	protected void removeUnreachableNodes()
	{
		log.debug("Removing unreachable nodes..");
		CostFunction cf = new LengthCostFunction();
		ShortestDistanceFromSupersource sd = new ShortestDistanceFromSupersource(cf, network, evacArea);
		sd.calcShortestDistances();
		Set<Node> toRemove = sd.getNodes(Double.MAX_VALUE);
		for(Node n : toRemove)
		{
			network.removeNode(network.getNodes().get(n.getId()));
		}
		
	}
	
	protected void addBorderInformation()
	{
		try {
			evacArea = EvacArea.readXMLFile(PathTracker.resolve("evacArea"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(evacArea != null)
		{
			borderNodes.clear();
			for(String s : evacArea.evacBorderOrderIds)
			{
				Node current = network.getNodes().get(new IdImpl(s));
				if(current != null)
				{
					borderNodes.add(current);
				}
				else
				{
					throw new RuntimeException("border node is not contained in the network!");
				}
			}
		}
	}
	
	protected void addBorderLinks()
	{

		Long maxLinkId = Long.MIN_VALUE;
		Long tmp;
		for(LinkImpl l : network.getLinks().values())
		{
			tmp = Long.parseLong(l.getId().toString());
			if(tmp > maxLinkId)
				maxLinkId = tmp;
		}
		
		boolean linkTo = false, linkFrom = false;
		//add links for Borders
		for(int i = 0; i < borderNodes.size(); ++i)
		{
			int found = 0;
			NodeImpl current = (NodeImpl)borderNodes.get(i);
			NodeImpl next = (NodeImpl)borderNodes.get((i + 1) % borderNodes.size());
			//check if link already exists:
			for(Link l : current.getOutLinks().values())
			{
				if(l.getToNode().equals(next))
				{
					borderLinkIds.add(l.getId());
					linkTo = true;
					++found;
				}
			}
			for(Link l : next.getOutLinks().values())
			{
				if(l.getToNode().equals(current))
				{
					borderLinkIds.add(l.getId());
					linkFrom = true;
					++found;
				}
			}
			log.debug("found " + found + "links for one border link");
			
			if(!linkTo)
			{
				Id id = new IdImpl("" + (++maxLinkId));
				network.createAndAddLink(id, 
						current, 
						next, 
						GraphAlgorithms.getDistanceMeter(current, next),
						0, //freespeed
						0, //capacity
						1); //numLanes)
				
				borderLinkIds.add(id);
			}
			if(!linkFrom)
			{
				Id id = new IdImpl("" + (++maxLinkId));
				network.createAndAddLink(id, 
						next, 
						current, 
						GraphAlgorithms.getDistanceMeter(current, next),
						0, //freespeed
						0, //capacity
						1); //numLanes)
				borderLinkIds.add(id);
			}
		}
	}
	
	public int getDemand(Node node)
	{
		return demandMap.get(node);
	}
	
	public void parseBlocks()
	{
		for(Node node : network.getNodes().values())
		{
			if(noStartSet.contains(node))
				continue;
			//node = network.getNode("254308571");
//			//konstruiere weg, der immer nach rechts fuehrt.
//			constructBlocks(node);
//			node = network.getNode("103902985");
//			//konstruiere weg, der immer nach rechts fuehrt.
//			constructBlocks(node);
//			node = network.getNode("103906854");
//			//konstruiere weg, der immer nach rechts fuehrt.
//			constructBlocks(node);
//			node = network.getNode("27785560");
//			//konstruiere weg, der immer nach rechts fuehrt.
			constructBlocks(node);
			//break;
		}
		//fuege alle Blocks in ein set zusammen
		for(Set<Block> set : hashBlocks.values())
		{
			for(Block b : set)
				blocks.add(b);
		}
		hashBlocks = null;
		//debug 
		for(Block b : blocks)
		{
			//log.debug("new Block, " + b.id +  "area: " + b.area_size);
			String nodes = "";
			log.debug("Block: " + b.id);
			for(Node n : b.border)
			{
				nodes += n.getId() + "-->";
				if(b.border.lastIndexOf(n) != b.border.indexOf(n))
				{
					throw new RuntimeException("invalid border!");
				}
			}
			log.debug(nodes);

		}
	}
	
	protected void constructBlock(List<Node> path, Node current)
	{
		
	}
	
	protected boolean linkPossible (Link l, Set<Link> usedLinks, List<Node> result, Node previousNode, Node currentNode, Node startNode)
	{
		if(l.getFromNode().equals(l.getToNode()))
			return false;
		if(usedLinks.contains(l))
			return false;
		if(currentNode == startNode)
			return true;
		if(!result.contains(l.getToNode()))
			return true;
		int linkCount = currentNode.getOutLinks().values().size();
//		if(linkCount <= 2)
//			return true;
		//sofern es eine Kante gibt, die zurueck fuehrt
		for(Link link : currentNode.getOutLinks().values())
		{
			if(usedLinks.contains(link) || link.getToNode().equals(previousNode))
				--linkCount;
		}
		if(linkCount == 0 && l.getToNode().equals(previousNode))
			return true;
		if(linkCount > 0 && !l.getToNode().equals(previousNode))
			return true;
		return false;
	}
	
	protected void constructBlocks(Node startNode)
	{
		Set<Block> bResult = new HashSet<Block>();
		for(Link start : startNode.getOutLinks().values())
		{
			if(borderLinkIds.contains(start.getId()))
				continue;
			log.debug("Constucting Block.." + startNode.getId().toString());
			List<Node> result = new LinkedList<Node>();
			Set<Link> usedLinks = new HashSet<Link>();
			usedLinks.add(start);
			Node previousNode = startNode;
			result.add(startNode);
			boolean error = false;
			Node currentNode = start.getToNode();
			do
			{
				if(currentNode.getId().toString().equals("27212444"))
				{
					log.debug("halte!");
				}
				log.debug("search for new node..");
				Link bestLink = null;
				double bestAngle = Double.MAX_VALUE;
				for(Link l : currentNode.getOutLinks().values())
				{
					if(linkPossible(l, usedLinks, result, previousNode, currentNode, startNode))
					{
						double angle = GraphAlgorithms.calcAngle(previousNode, currentNode, l.getToNode());
						if(	angle < bestAngle )
						{
							bestAngle = angle;
							bestLink = l;
						}
					}
				}
				if(bestLink == null)
				{
					error = true;
					break;
				}
				else
				{
					usedLinks.add(bestLink);
				}
				previousNode = currentNode;
				result.add(currentNode);
				currentNode = bestLink.getToNode();
				log.debug("add node " + currentNode.getId().toString() + " to block..");
			}while(currentNode != startNode);
			if(!error && result.size() > 2)
			{
				log.debug("Success??!");
				Set<Block> minimalBlocks = Block.extractMinimalBlocks(result);
				for(Block resultBlock : minimalBlocks)
				{
					if(containsBlock(resultBlock))
					{
						continue;
					}
					else
					{
						log.debug(resultBlock.id);
						if(resultBlock.id == 1158L)
						{
							String bar = "";
							bar += "10";
						}
						String foo = "";
						for(Node n : resultBlock.border)
						{
							foo += n.getId().toString() + ", ";
						}
						log.debug(foo);
						//suche das richtige Set heraus
						Long hash = getHash(resultBlock);
						Set<Block> set = hashBlocks.get(hash);
						if(set == null)
						{
							set = new HashSet<Block>();
							set.add(resultBlock);
							hashBlocks.put(hash, set);
						}
						else
						{
							set.add(resultBlock);
						}
					}
				}
			}
			result = new LinkedList<Node>();
		}
	}
	
	
	protected boolean containsBlock(Block b)
	{
		Set<Block> existing = hashBlocks.get(getHash(b));
		if(existing != null)
		{
			Node start = b.border.get(0);
			for(Block block : existing)
			{
				if(block.border.size() == b.border.size())
				{
					if(isSameSequence(block, b))
						return true;
					else
						return false;
				}
			}
		}
		return false;
	}
	
	protected boolean isSameSequence(Block first, Block second)
	{
		boolean result = false;
		Node start = first.border.get(0);
		for(int i = 0; i < second.border.size() && !result; ++i)
		{
			if(start.equals(second.border.get(i)))
			{
				result |= isSameSequence(first, second, 0, i);
			}
		}
		return result;
	}
	
	protected boolean isSameSequence(Block first, Block second, int indexFirst, int indexSecond)
	{
		boolean result = true;
		//start forward
		int size = first.border.size();
		int counter = size;
		int i = indexFirst;
		int j = indexSecond;
		while(counter > 0)
		{
			if(!first.border.get(i).equals(second.border.get(j)))
			{
				result = false;
				break;
			}
			i = (i+1) % size;
			j = (j+1) % size;
			--counter;
		}
		if(result)
			return result;
		result = true;
		counter = size;
		i = indexFirst;
		j = indexSecond;
		while(counter > 0)
		{
			if(!first.border.get(i).equals(second.border.get(j)))
			{
				result = false;
				break;
			}
			i = (i-1) % size;
			j = (j-1) % size;
			if(i < 0)
				i += size;
			if(j < 0)
				j += size;
			--counter;
		}
		return result;
	}

	protected Long getHash(Block b)
	{
		BigInteger hash = new BigInteger("1");
		for(Node n : b.border)
		{
			hash = hash.multiply(BigInteger.valueOf(Long.parseLong(n.getId().toString())));
		}
		return hash.longValue();
	}	
}
