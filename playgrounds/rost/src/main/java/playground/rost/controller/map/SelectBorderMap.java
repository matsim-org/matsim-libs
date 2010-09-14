/******************************************************************************
 *project: org.matsim.*
 * SelectBorderMap.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import playground.rost.graph.Border;
import playground.rost.graph.GraphAlgorithms;

public class SelectBorderMap extends BasicMapImpl {

	private static final Logger log = Logger.getLogger(SelectBorderMap.class);
	

	public Border border = new Border();
	
	NetworkImpl network;
	public SelectBorderMap(NetworkImpl network)
	{
		this.network = network;
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
		log.debug("Point recorded (x,y): (" + x + "," + y + ")");
		log.debug("lookup nearest Point in map..");
		Node bestNodeSoFar = null;
		double minDistance = Double.MAX_VALUE;
		double distance = Double.MAX_VALUE;
		for(Node node : network.getNodes().values())
		{
			distance = Math.sqrt(Math.pow(x-node.getCoord().getX(), 2)+Math.pow(y-node.getCoord().getY(), 2));
			if(distance < minDistance)
			{
				minDistance = distance;
				bestNodeSoFar = node;
			}
		}
		if(bestNodeSoFar != null)
		{
			border.addNode(bestNodeSoFar);
			log.debug("minimal distance: " + distance);
			UIChange();
		}
		if(border.size() > 4)
		{
			log.debug(GraphAlgorithms.getSimplePolygonArea(border.getDistHull()));
		}
	}
}
