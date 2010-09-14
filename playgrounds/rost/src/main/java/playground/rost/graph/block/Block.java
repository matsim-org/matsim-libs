/******************************************************************************
 *project: org.matsim.*
 * Block.java
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


package playground.rost.graph.block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

import playground.rost.graph.GraphAlgorithms;

public class Block {
	private static final Logger log = Logger.getLogger(Block.class);
	
	protected static long id_counter = 1000;
	
	public double x_mean;
	public double y_mean;
	public long id = ++id_counter;
	public double area_size;
	
	
	
	public List<Node> border = new LinkedList<Node>();
	
	public Block(List<Node> border)
	{
		this.setBorder(border);
	}
	
	protected Block()
	{
		
	}
	
	public static Block create(NetworkImpl network, long id, double x, double y, double areaSize, List<String> borderIds)
	{
		List<Node> borderNodes = new LinkedList<Node>();
		Block result = new Block();
		result.id = id;
		result.area_size = areaSize;
		result.x_mean = x;
		result.y_mean = y;
		Node node;
		for(String s : borderIds)
		{
			node = network.getNodes().get(new IdImpl(s));
			if(node == null)
				return null;
			borderNodes.add(node);
		}
		result.border = borderNodes;
		return result;
	}
	
	public void setBorder(List<Node> border)
	{
		if(border == null || border.size() < 2)
			throw new RuntimeException("invalid call to setBorder");
		this.border = border;
		x_mean = 0;
		y_mean = 0;
		for(Node node : border)
		{
			x_mean += node.getCoord().getX();
			y_mean += node.getCoord().getY();
		}
		this.area_size = GraphAlgorithms.getSimplePolygonArea(this.border);
		x_mean /= (border.size());
		y_mean /= (border.size());
	}
	
	public static Set<Block> extractMinimalBlocks(List<Node> border)
	{
		
		log.debug("extractMinimalBlocks!");
		String temp = "";
		for(Node n : border)
		{
			temp += n.getId().toString() + ", ";
		}
		log.debug(temp);
		Set<Block> result = new HashSet<Block>();
		Map<Node, Integer> mapFirstOccurence = new HashMap<Node, Integer>();
		//build map, to define whether some border includes some other..

		for(int i = 0; i < border.size(); ++i)
		{
			Node current = border.get(i);
			if(!mapFirstOccurence.containsKey(current))
			{
				mapFirstOccurence.put(current, i);
			}
		}
		
		for(int i = border.size()-1; i >= 0; --i)
		{
			Node current = border.get(i);
			if(mapFirstOccurence.get(current) != i)
			{
				//sub Block found
				List<Node> subBlockBorder = new LinkedList<Node>();
				for(int j = mapFirstOccurence.get(current); j < i; ++j)
				{
					subBlockBorder.add(border.get(j));
				}
				log.debug("found subBlock: ");
				temp = "";
				for(Node n : subBlockBorder)
				{
					temp += n.getId().toString() + ", ";
				}
				log.debug(temp);
				
				if(isRealBorder(subBlockBorder))
				{
					log.debug("is real!.. get subSub");
					Set<Block> subSubBlocks = Block.extractMinimalBlocks(subBlockBorder);
					for(Block subSub : subSubBlocks)
					{
						temp = "";
						for(Node n : subSub.border)
						{
							temp += n.getId().toString() + ", ";
						}
						log.debug(temp);
						result.add(subSub);
					}
				}
				
				int d =  mapFirstOccurence.get(current);
				//remove this border..
				for(int j = i; j > d; --j)
				{
					border.remove(j);
				}
				//rebuild map
				mapFirstOccurence.clear();
				//build map, to define whether some border includes some other..

				for(int k = 0; k < border.size(); ++k)
				{
					current = border.get(k);
					if(!mapFirstOccurence.containsKey(current))
					{
						mapFirstOccurence.put(current, k);
					}
				}
				i = d;

			}
		}
		if(isRealBorder(border))
		{
			Block whole = new Block(border);
			result.add(whole);
		}
		return result;	
	}
	
	protected static boolean isRealBorder(List<Node> border)
	{
		if(border.size() < 3)
			return false;
		Node n1, n2, n3;
		n1 = border.get(0);
		n2 = border.get(1);
		n3 = border.get(2);
		int i = 3;
		do
		{
			if(!n1.equals(n2) && !n2.equals(n3) && !n1.equals(n3))
				return true;
			n1 = n2;
			n2 = n3;
			n3 = border.get(i % border.size());
			++i;
		}while(i <= border.size());
		return false;
	}	
}
