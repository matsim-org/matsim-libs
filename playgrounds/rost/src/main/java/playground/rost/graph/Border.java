/******************************************************************************
 *project: org.matsim.*
 * Border.java
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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;

public class Border {
	
	private static final Logger log = Logger.getLogger(Border.class);
	
	String Id;
	Set<Node> borderElements = new HashSet<Node>();
	
	public void addNode(Node node)
	{
		borderElements.add(node);
	}
	
	public boolean contains(Node node)
	{
		return borderElements.contains(node);
	}
	
	public int size()
	{
		return borderElements.size();
	}
	
	protected List<Node> getUpperConvexHullForward()
	{
		List<Node> result = new LinkedList<Node>();
		List<Node> ordered = orderX(borderElements);
		int i = 0;
		result.add(ordered.get(i));
		while(i < ordered.size()-1)
		{
			Node current = ordered.get(i);
			int bestIndex = -1;
			Node best = null;
			double bestScore = -Double.MAX_VALUE;
			for(int j = i+1; j < ordered.size(); ++j)
			{
				Node tmp = ordered.get(j);
				double score = (tmp.getCoord().getY() - current.getCoord().getY()) / (tmp.getCoord().getX() - current.getCoord().getX());
				if(score > bestScore)
				{
					best = tmp;
					bestIndex = j;
					bestScore = score;
				}
			}
			if(best != null)
			{
				result.add(best);
				i = bestIndex;
			}
			else
			{
				break;
			}
		}
		return result;
	}
	
	protected List<Node> getLowerConvexHullBackward()
	{
		List<Node> result = new LinkedList<Node>();
		List<Node> ordered = orderX(borderElements);
		int i = ordered.size()-1;
		while(i > 0)
		{
			Node current = ordered.get(i);
			int bestIndex = -1;
			Node best = null;
			double bestScore = +Double.MAX_VALUE;
			for(int j = i-1; j >= 0; --j)
			{
				Node tmp = ordered.get(j);
				double score = (current.getCoord().getY() - tmp.getCoord().getY()) / (tmp.getCoord().getX() - current.getCoord().getX());
				if(score < bestScore)
				{
					best = tmp;
					bestIndex = j;
					bestScore = score;
				}
			}
			if(best != null)
			{
				result.add(best);
				i = bestIndex;
			}
			else
			{
				break;
			}
		}
		return result;
	}
	
	public List<Node> getConvexHull()
	{
		List<Node> result = new LinkedList<Node>();
		List<Node> tmp = getUpperConvexHullForward();
		for(Node node : tmp)
		{
			result.add(node);
		}
		tmp = getLowerConvexHullBackward();
		for(Node node : tmp)
		{
			result.add(node);
		}
		return result;
	}
	
	
	
	public List<Node> getAreaHull()
	{
		//log.debug("getAreaHull()..");
		List<Node> allNodes = new LinkedList<Node>();
		for(Node node : borderElements)
		{
			allNodes.add(node);
		}
		List<Node> convexHull = getConvexHull();
		double maxDist = GraphAlgorithms.getMaxDistance(convexHull);
		while(convexHull.size() <= borderElements.size())
		{
			double bestScore = Double.MAX_VALUE;
			int index = -1;
			Node nodeToAdd = null;
			for(Node node : allNodes)
			{
				if(!convexHull.contains(node))
				{
					double score;
					for(int i = 0; i < convexHull.size()-1; ++i)
					{
						Node n1, n2;
						n1 = convexHull.get(i);
						n2 = convexHull.get(i+1);
						if(GraphAlgorithms.getDistance(node, n1) + GraphAlgorithms.getDistance(node, n2) > 1 * maxDist)
							continue;
						score = getArea(n1,n2,node);
						//log.debug("Possible area: " + score);
						if(score < bestScore)
						{
							bestScore = score;
							index = i;
							nodeToAdd = node;
						}
					}
				}
			}
			if(nodeToAdd != null)
			{
				//log.debug("chosen area: " + bestScore);
				convexHull.add(index+1, nodeToAdd);
			}
		}
		return convexHull;
	}
	

	public List<Node> getDistHull()
	{
		//log.debug("getDistHull()..");
		List<Node> allNodes = new LinkedList<Node>();
		for(Node node : borderElements)
		{
			allNodes.add(node);
		}
		List<Node> convexHull = getConvexHull();
		while(convexHull.size() <= borderElements.size())
		{
			double bestScore = Double.MAX_VALUE;
			int index = -1;
			Node nodeToAdd = null;
			for(Node node : allNodes)
			{
				if(!convexHull.contains(node))
				{
					double score;
					for(int i = 0; i < convexHull.size()-1; ++i)
					{
						Node n1, n2;
						n1 = convexHull.get(i);
						n2 = convexHull.get(i+1);
						double dist = GraphAlgorithms.getDistance(n1, n2);
						double newDist = GraphAlgorithms.getDistance(n1, node);
						newDist += GraphAlgorithms.getDistance(node, n2);
						score = newDist / dist * getArea(node, n1, n2);
						//log.debug("newDist / dist: " + score);
						if(score < bestScore)
						{
							bestScore = score;
							index = i;
							nodeToAdd = node;
						}
					}
				}
			}
			if(nodeToAdd != null)
			{
				//log.debug("chosen dist: " + bestScore);
				convexHull.add(index+1, nodeToAdd);
			}
		}
		return convexHull;
	}
	protected double getArea(Node node1, Node node2, Node node3)
	{
		double area;
		double s13 = Math.sqrt(Math.pow((node3.getCoord().getY() - node1.getCoord().getY()),2) + Math.pow((node3.getCoord().getX() - node1.getCoord().getX()),2));
		double s12 = Math.sqrt(Math.pow((node2.getCoord().getY() - node1.getCoord().getY()),2) + Math.pow((node2.getCoord().getX() - node1.getCoord().getX()),2));
		double s23 = Math.sqrt(Math.pow((node2.getCoord().getY() - node3.getCoord().getY()),2) + Math.pow((node2.getCoord().getX() - node3.getCoord().getX()),2));
		
		//Heronsche Flächenformel
		double s = (s13+s12+s23)/2;
		area = Math.sqrt(s*(s-s12)*(s-s13)*(s-s23));
		return area;
	}
	
	protected List<Node> orderX(Collection<Node> list)
	{
		List<Node> result = new LinkedList<Node>();
		while(result.size() < list.size())
		{
			Node mostLeft = null;
			double minX = Double.MAX_VALUE;
			for(Node node : list)
			{
				if(!result.contains(node) && node.getCoord().getX() < minX)
				{
					mostLeft = node;
					minX = node.getCoord().getX();
				}
			}
			result.add(mostLeft);
		}
		return result;
	}
}
