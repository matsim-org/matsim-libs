/******************************************************************************
 *project: org.matsim.*
 * AreaNodeVisModule.java
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


package playground.rost.controller.vismodule.implementations;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.graph.Border;
import playground.rost.graph.GraphAlgorithms;

public class AreaNodeVisModule extends NodeVisModule {

	protected NetworkLayer network;
	protected Border border;
	
	public AreaNodeVisModule(VisModuleContainer vMContainer, Border border, NetworkLayer network)
	{
		super(vMContainer, network);
		this.network = network;
		this.border = border;
		this.attributes.put("color of border nodes", "0x000000");
		this.attributes.put("color of nodes in area", "0x00FF00");
		this.attributes.put("color of normal nodes", "0xFF0000");
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		boolean show = this.parseBoolean("show", true);
		if(!show || border == null)
			return;
		Color cBorder = this.parseColor("color of border nodes", Color.black);
		Color cArea = this.parseColor("color of nodes in area", Color.GREEN);
		Color cNormal = this.parseColor("color of normal nodes", Color.red);
		List<Node> hull = null;
		if(border.size() > 2)
			hull = border.getDistHull();
		int size = 1,x,y;
		for(Node node : network.getNodes().values())
		{
			size = 1;
			if(!map.isVisible(node))
				continue;
			if(border.contains(node))
			{
				g.setColor(cBorder);
				size = 5;
			}
			else if(hull != null && GraphAlgorithms.pointIsInPolygon(hull, node))
			{
				g.setColor(cArea);
			}
			else
			{
				g.setColor(cNormal);
			}
			size = (int)Math.ceil((size*Math.sqrt(map.getZoom())));
			x = map.getXonPanel(node);
			y = map.getYonPanel(node);
			g.fillRect(x, y, size, size);
		}
	}
	
}
