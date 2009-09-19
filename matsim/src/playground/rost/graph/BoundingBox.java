/******************************************************************************
 *project: org.matsim.*
 * BoundingBox.java
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

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings;

public class BoundingBox {

	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;
	
	protected void clear()
	{
		minX = Double.POSITIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		maxX = Double.NEGATIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;		
	}

	public void run(Collection<Node> collection) {
		this.clear();
		for (Node n : collection) {
			if(n.getId().toString().equals(GlobalFlowCalculationSettings.superSinkId))
				continue;
			if (n.getCoord().getX() < this.minX) { this.minX = n.getCoord().getX(); }
			if (n.getCoord().getY() < this.minY) { this.minY = n.getCoord().getY(); }
			if (n.getCoord().getX() > this.maxX) { this.maxX = n.getCoord().getX(); }
			if (n.getCoord().getY() > this.maxY) { this.maxY = n.getCoord().getY(); }
		}
	}
	
	public void add(Node n)
	{
		if (n.getCoord().getX() < this.minX) { this.minX = n.getCoord().getX(); }
		if (n.getCoord().getY() < this.minY) { this.minY = n.getCoord().getY(); }
		if (n.getCoord().getX() > this.maxX) { this.maxX = n.getCoord().getX(); }
		if (n.getCoord().getY() > this.maxY) { this.maxY = n.getCoord().getY(); }	
	}
	
	public void run(NetworkLayer network)
	{
		this.clear();
		Node sink = network.getNode(GlobalFlowCalculationSettings.superSinkId);
		for (Node n : network.getNodes().values())
		{
			if(n == sink)
				continue;
			if (n.getCoord().getX() < this.minX) { this.minX = n.getCoord().getX(); }
			if (n.getCoord().getY() < this.minY) { this.minY = n.getCoord().getY(); }
			if (n.getCoord().getX() > this.maxX) { this.maxX = n.getCoord().getX(); }
			if (n.getCoord().getY() > this.maxY) { this.maxY = n.getCoord().getY(); }
		}
	}

	public double getMinX() {
		return this.minX;
	}

	public double getMaxX() {
		return this.maxX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxY() {
		return this.maxY;
	}
	
	public boolean outOfBox(Node node)
	{
		return outOfBox(node.getCoord().getX(), node.getCoord().getY());
	}
	
	public boolean outOfBox(double x, double y)
	{
		if(x < minX ||
				y < minY ||
				x > maxX ||
				y > maxY)
		{
			return true;
		}
		return false;
	}
	
	public boolean boxIntersects(BoundingBox other)
	{
		boolean hor = false;
		boolean ver = false;
		//test1
		double x0,x1,x2,x3,y0,y1,y2,y3;
		
		x0 = this.maxX;
		y0 = this.maxY;
		
		x1 = this.minX;
		y1 = this.maxY;
		
		x2 = this.maxX;
		y2 = this.minY;
		
		x3 = this.minX;
		y3 = this.minY;
		
		if(!outOfBox(x0,y0) || 
				!outOfBox(x1,y1) ||
				!outOfBox(x2,y2) ||
				!outOfBox(x3,y3))
		{
			return true;
		}
		return false;
	}
}
