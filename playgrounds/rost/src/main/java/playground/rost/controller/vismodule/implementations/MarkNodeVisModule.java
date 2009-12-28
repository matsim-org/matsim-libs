/******************************************************************************
 *project: org.matsim.*
 * MarkNodeVisModule.java
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

public class MarkNodeVisModule extends AbstractVisModuleImpl {

	NetworkLayer network;
	
	public MarkNodeVisModule(VisModuleContainer vMContainer, NetworkLayer network)
	{
		super(vMContainer, "MarkNodeView");
		this.network = network;
		this.attributes.put("size", "5");
		this.attributes.put("color", "0xFFFFFF");
		this.attributes.put("ids", "");
		
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		String[] ids = this.parseList("ids");
		if(ids.length == 0)
			return;
		Color c = this.parseColor("color", Color.red);
		int size = this.parseInt("size", 5);
		g.setColor(c);
		int  x, y;
		for(int i = 0; i < ids.length; ++i)
		{
			if(ids[i].length() == 0)
				continue;
			Node node = network.getNode(ids[i]);
			if(!map.isVisible(node))
				continue;
			
			x = map.getXonPanel(node);
			y = map.getYonPanel(node);
			g.fillRect(x, y, size, size);
		}
	}
}
