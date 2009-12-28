/******************************************************************************
 *project: org.matsim.*
 * PlacePplMap.java
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


package playground.rost.controller.map;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.controller.uicontrols.PopulationDensity;
import playground.rost.graph.Point;
import playground.rost.graph.populationpoint.PopulationPoint;
import playground.rost.graph.populationpoint.PopulationPointCollection;

public class PlacePplMap extends BasicMapImpl {
	
	private static final Logger log = Logger.getLogger(PlacePplMap.class);
	
	protected PopulationPointCollection pplPoints = new PopulationPointCollection();
	protected Map<Node, Integer> nodeDensity = new HashMap<Node, Integer>();
	protected int maxDensity;
	
	protected PopulationDensity popDensity;
	
	protected NetworkLayer network;
	
	public PlacePplMap(int maxDensity, NetworkLayer network, PopulationDensity popDensity)
	{
		this.maxDensity = maxDensity;
		this.popDensity = popDensity;
		this.network = network;
	}
	
	public int getMaxDensity()
	{
		return maxDensity;
	}

	@Override
	public void handleMouseClick(MouseEvent event)
	{
		if(event.getButton() == MouseEvent.BUTTON1)
		{
			super.handleMouseClick(event);
			return;
		}
		double x,y;
		x = getX(event.getX());
		y = getY(event.getY());
		Point p = new Point(x,y);
		log.debug("Point recorded (x,y): (" + x + "," + y + ")");
		PopulationPoint popPoint = new PopulationPoint(x,y,popDensity.getDensity());
		pplPoints.add(popPoint);
	}	
	
	public PopulationPointCollection getPplPoints()
	{
		return pplPoints;
	}
	
	public void setPplPoints(PopulationPointCollection pplPoints)
	{
		this.pplPoints = pplPoints;
	}
	
	public int getDensity(Node n)
	{
		return nodeDensity.get(n);
	}
	
	public NetworkLayer getNetwork()
	{
		return network;
	}
}

