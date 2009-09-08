/******************************************************************************
 *project: org.matsim.*
 * NodeVisModule.java
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

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.AbstractVisModuleImpl;
import playground.rost.controller.vismodule.VisModuleContainer;

public class NodeVisModule extends AbstractVisModuleImpl {
	
	protected NetworkLayer network;
	
	public NodeVisModule(VisModuleContainer vMContainer, NetworkLayer network)
	{
		super(vMContainer, "NodeView");
		this.network = network;
		this.attributes.put("color", "0xFF0000");
		this.attributes.put("show", "true");
		this.attributes.put("ids", "false");
		
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		boolean show = this.parseBoolean("show", true);
		if(!show)
			return;
		boolean ids = this.parseBoolean("ids", false);
		Color c = this.parseColor("color", Color.red);
		g.setColor(c);
		int size, x, y;
		for(Node node : network.getNodes().values())
		{
			if(!map.isVisible(node))
				continue;
			size = 1;
			x = map.getXonPanel(node);
			y = map.getYonPanel(node);
			size = (int)Math.ceil((size*Math.sqrt(map.getZoom())));
			g.fillRect(x, y, size, size);
			if(ids)
			{
				g.drawString("Node " + node.getId().toString(), x+5, y+5);
			}
		}
	}
}
