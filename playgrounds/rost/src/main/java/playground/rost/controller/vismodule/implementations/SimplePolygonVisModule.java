/******************************************************************************
 *project: org.matsim.*
 * SimplePolygonVisModule.java
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

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.AbstractVisModuleImpl;
import playground.rost.controller.vismodule.VisModuleContainer;

public class SimplePolygonVisModule extends AbstractVisModuleImpl {

	
	protected List<Node> nodes;
	
	public SimplePolygonVisModule(VisModuleContainer vMContainer, String title, List<Node> nodes)
	{
		super(vMContainer, title);
		this.nodes = nodes;
		this.attributes.put("color", "0x00FF00");
		this.attributes.put("show", "true");		
	}
	
	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		boolean show = this.parseBoolean("show", true);
		if(!show || nodes == null || nodes.size() < 2)
			return;
		Color c = this.parseColor("color", Color.magenta);
		g.setColor(c);
		int i = 0,
			size = nodes.size(),
			x,y,x2,y2;
		Node current = nodes.get(i);
		Node next;
		do
		{
			i = ++i % size;
			next = nodes.get(i);
			x = map.getXonPanel(current);
			y = map.getYonPanel(current);
			x2 = map.getXonPanel(next);
			y2 = map.getYonPanel(next);
			g.drawLine(x, y, x2, y2);
			current = next;
		}while(i != 0);
	}	
}
