/******************************************************************************
 *project: org.matsim.*
 * FlatNetwork.java
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


package playground.rost.graph.flatnetwork;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.rost.graph.GraphAlgorithms;
import playground.rost.graph.Point;

public class FlatNetwork {
	private static final Logger log = Logger.getLogger(FlatNetwork.class);
	
	protected NetworkLayer input;
	protected Map<Long, Set<FlatLink>> mapLinks = new HashMap<Long, Set<FlatLink>>();
	protected Set<FlatLink> links = new HashSet<FlatLink>();
	protected Map<Point, Set<FlatLink>> intersections = new HashMap<Point, Set<FlatLink>>();

	
	protected Set<NodeImpl> newNodes = new HashSet<NodeImpl>();
	
	public Map<Id, Set<Id>> splittedLinks = new HashMap<Id, Set<Id>>();
	public Map<Id, Set<Id>> newlyCreatedNodes = new HashMap<Id, Set<Id>>();
	
	protected void parseNetwork()
	{
		Set<FlatLink> linkSet;
		Long hash;
		FlatLink fL;
		for(Link l : input.getLinks().values())
		{
			hash = getHash(l);
			log.debug("hash: " + hash);
			linkSet = mapLinks.get(hash);
			if(linkSet != null)
			{
				fL = getReverseLink(linkSet, l);
				if(fL != null)
				{
					fL.setReverseId(l.getId());
				}
				else
				{
					fL = new FlatLink(l);
					linkSet.add(fL);
				}
			}
			else
			{
				linkSet = new HashSet<FlatLink>();
				fL = new FlatLink(l);
				linkSet.add(fL);
				mapLinks.put(hash, linkSet);
			}
		}
	}
	
	protected FlatLink getReverseLink(Set<FlatLink> set, Link l)
	{
		for(FlatLink fL : set)
		{
			if(l.getFromNode().equals(fL.toNode) && l.getToNode().equals(fL.fromNode))
			{
				return fL;
			}
			if(l.getFromNode().equals(fL.fromNode) && l.getToNode().equals(fL.toNode))
			{
				//throw new RuntimeException("FUCK YOU!");
			}
		}
		return null;
	}
	
	protected void calcIntersections()
	{
		
		links.clear();
		for(Set<FlatLink> set : mapLinks.values())
		{
			for(FlatLink fL : set)
			{
				links.add(fL);
			}
		}
		mapLinks.clear();
		//every link is contained in the "links"-Set
		Set<FlatLink> intersectionSet;
		for(FlatLink fL : links)
		{
			for(FlatLink fL2 : links)
			{
				if(fL != fL2 && 
					(fL.fromNode != fL2.fromNode && 
						fL.toNode != fL2.toNode && 
						fL.toNode != fL2.fromNode && 
						fL.fromNode != fL2.toNode))
				{
					Point intersection = new Point();
					if(getIntersection(fL, fL2, intersection))
					{
						log.debug("" + intersection.x + ", " + intersection.y);
						log.debug("intersection: " + fL.link.getId().toString() + " - " + fL2.link.getId().toString());
						intersectionSet = intersections.get(intersection);
						if(intersectionSet != null)
						{
							intersectionSet.add(fL);
							intersectionSet.add(fL2);
							log.debug(intersectionSet.size());
						}
						else
						{
							log.debug("new set..");
							intersectionSet = new HashSet<FlatLink>();
							intersectionSet.add(fL);
							intersectionSet.add(fL2);
							intersections.put(intersection, intersectionSet);
							log.debug(intersectionSet.size());
						}
					}
				}
			}
		}
	}
	
	protected boolean getIntersection(FlatLink l1, FlatLink l2, Point p) {
		

		double 	x1 = l1.fromX,
				y1 = l1.fromY, 
				x2 = l1.toX, 
				y2 = l1.toY,
				x3 = l2.fromX,
				y3 = l2.fromY,
				x4 = l2.toX,
				y4 = l2.toY;

		double uA = ((x4-x3)*(y1-y3)-(y4-y3)*(x1-x3))/( (y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
		double uB = ((x2-x1)*(y1-y3)-(y2-y1)*(x1-x3))/( (y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
		if(uA > 0 && uA < 1 && uB > 0 && uB < 1)
		{
			p.x = x1+ uA * (x2-x1);
			p.y = y1 + uA * (y2-y1);
			return true;
		}

		return false;
	}
	
	protected void createNewNodesAndLinks()
	{
		log.debug("createNewNodesAndLinks!");
		//calc max Ids for new Links / nodes..
		Long maxNodeId = Long.MIN_VALUE;
		
		Long tmp;
		
		for(NodeImpl n : input.getNodes().values())
		{
			tmp = Long.parseLong(n.getId().toString());
			if(tmp > maxNodeId)
				maxNodeId = tmp;
		}
		
		
		Set<FlatLink> intersectingLinks;
		Set<Point> pointCollection = intersections.keySet();
		Object[] pointArray = pointCollection.toArray();
		for(int i = 0; i < pointArray.length; ++i)
		{
			Point p = (Point)pointArray[i];
			log.debug("Point p: " + p.x + ", " + p.y);
			intersectingLinks = intersections.get(p);
			log.debug(intersectingLinks.size());
			if(intersectingLinks.size() < 2)
				throw new RuntimeException("algo wrong");
			Id id = new IdImpl(++maxNodeId);
			Coord coord = new CoordImpl(p.x, p.y);
			NodeImpl intersectionNode = new NodeImpl(id);
			
			intersectionNode.setCoord(coord);
			
			newNodes.add(intersectionNode);
			for(FlatLink fL : intersectingLinks )
			{
				splitLink(fL, p, intersectionNode);
				//update information, that this node was created due to this link
				Set<Id> idSet = newlyCreatedNodes.get(fL.link);
				if(idSet != null)
				{
					idSet.add(id);
				}
				else
				{
					idSet = new HashSet<Id>();
					idSet.add(id);
					newlyCreatedNodes.put(fL.link.getId(), idSet);
				}
			}
			intersectingLinks.clear();
		}
	}
	
	protected void splitLink(FlatLink toRemove, Point currentPoint, Node n)
	{
		FlatLink fL1 = new FlatLink(toRemove.link, toRemove.fromNode, n);
		FlatLink fL2 = new FlatLink(toRemove.link, n, toRemove.toNode);
		
		double quo = GraphAlgorithms.getDistance(toRemove.fromNode, n) / GraphAlgorithms.getDistance(toRemove.fromNode, toRemove.toNode);
		fL1.length = toRemove.length * quo;
		fL2.length = toRemove.length * (1 - quo);
		if(toRemove.hasReverseLink)
		{
			fL1.setReverseLinkTrue();
			fL2.setReverseLinkTrue();
		}
		else
		{
			log.debug("new link has no reverse link!");
		}
		fL1.isNewLink = true;
		fL2.isNewLink = true;
		//remove the "old" link from the other intersection sets
		Set<FlatLink> oldSet;
		for(Point p : intersections.keySet())
		{
			oldSet = intersections.get(p);
			if(p.equals(currentPoint))
			{
				continue;
			}
			if(oldSet.contains(toRemove))
			{
				oldSet.remove(toRemove);
				double distFL1 = Math.max(
						GraphAlgorithms.getDistanceMeter(fL1.fromX, fL1.fromY, p.x, p.y),
						GraphAlgorithms.getDistanceMeter(fL1.toX, fL1.toY, p.x, p.y)) / GraphAlgorithms.getDistanceMeter(fL1.fromX, fL1.fromY, fL1.toX, fL1.toY);
						
				double distFL2 = Math.max(
						GraphAlgorithms.getDistanceMeter(fL2.fromX, fL2.fromY, p.x, p.y),
						GraphAlgorithms.getDistanceMeter(fL2.toX, fL2.toY, p.x, p.y)) / GraphAlgorithms.getDistanceMeter(fL2.fromX, fL2.fromY, fL2.toX, fL2.toY);
								
				if(distFL1 > 1 && distFL2 > 1)
					log.debug("problem calculating which link of the intersection map shall be replaced: " + distFL1 + " : " + distFL2 + " (chosing the smaller one!)");
				if(distFL1 < distFL2)
				{
					oldSet.add(fL1);
				}
				else
				{
					oldSet.add(fL2);
				}
			}
		}
		links.remove(toRemove);
		links.add(fL1);
		links.add(fL2);
	}
	
	protected NetworkLayer composeOutputNetwork()
	{
		NetworkLayer output = new NetworkLayer();
		
		NodeImpl newNode;
		CoordImpl newCoord;
		LinkImpl newLink;
		Id newId;
		for(Node node : input.getNodes().values())
		{
			newId = new IdImpl(node.getId().toString());
			newCoord = new CoordImpl(node.getCoord().getX(), node.getCoord().getY());
			output.createAndAddNode(newId, newCoord);
		}
		int counter = 0;
		for(Node node : newNodes)
		{
			++counter;
			newId = new IdImpl(node.getId().toString());
			newCoord = new CoordImpl(node.getCoord().getX(), node.getCoord().getY());
			output.createAndAddNode(newId, newCoord);
		}
		log.debug("created " + counter + " new nodes!");
		
		//createLinks
		
		Long maxLinkId = Long.MIN_VALUE;
		Long tmp;
		for(LinkImpl l : input.getLinks().values())
		{
			tmp = Long.parseLong(l.getId().toString());
			if(tmp > maxLinkId)
				maxLinkId = tmp;
		}
		maxLinkId += 100;
		double freespeed;
		double capacity;
		double length;
		for(FlatLink fL : links)
		{
			if(fL.isNewLink)
			{
				maxLinkId = maxLinkId + 1;
				newId = new IdImpl("" + maxLinkId);
			}
			else
			{
				newId = new IdImpl(fL.link.getId().toString());
			}
			//log.debug("linkid: " + newId.toString());
			length = fL.length;
			capacity = fL.link.getCapacity(1.);
			freespeed = fL.link.getFreespeed(1.);
			output.createAndAddLink(newId, 
								output.getNode(fL.fromNode.getId().toString()),
								output.getNode(fL.toNode.getId().toString()),
								length, freespeed, capacity, 1);
			if(fL.isNewLink)
			{
				addSplittedLinkToMap(output.getLink(newId), fL.link);
			}
			if(fL.hasReverseLink)
			{
				if(fL.isNewLink)
				{
					maxLinkId += 1;
					newId = new IdImpl("" + maxLinkId);
				}
				else
				{
					newId = new IdImpl(fL.reverseId.toString());
				}
				//log.debug("linkid: " + newId.toString());
				output.createAndAddLink(newId, 
						output.getNode(fL.toNode.getId().toString()),
						output.getNode(fL.fromNode.getId().toString()),
						length, freespeed, capacity, 1);
				if(fL.isNewLink)
				{
					addSplittedLinkToMap(output.getLink(newId), fL.link);
				}
			}
		}
		return output;
	}
	
	public NetworkLayer flatten(NetworkLayer input)
	{
		this.input = input;
		parseNetwork();
		calcIntersections();
		createNewNodesAndLinks();
		NetworkLayer output = composeOutputNetwork();
		return output;
	}
	
	protected Long getHash(Link l)
	{
		BigInteger hash = new BigInteger("1");
		hash = hash.multiply(BigInteger.valueOf(Long.parseLong(l.getFromNode().getId().toString())));
		hash = hash.multiply(BigInteger.valueOf(Long.parseLong(l.getToNode().getId().toString())));
		return hash.longValue();
	}
	

	protected void addSplittedLinkToMap(Link splitted, Link old)
	{
		Set<Id> set = splittedLinks.get(old.getId());
		if(set == null)
		{
			set = new HashSet<Id>();
			set.add(splitted.getId());
			splittedLinks.put(old.getId(), set);
		}
		else
		{
			set.add(splitted.getId());
		}
		
	}
}

