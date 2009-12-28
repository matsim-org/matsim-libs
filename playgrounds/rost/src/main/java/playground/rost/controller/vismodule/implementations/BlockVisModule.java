/******************************************************************************
 *project: org.matsim.*
 * BlockVisModule.java
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
import java.awt.Polygon;
import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.network.Node;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.AbstractVisModuleImpl;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.graph.block.Block;

public class BlockVisModule extends AbstractVisModuleImpl {

	protected Random rand = new Random(System.currentTimeMillis());

	protected Collection<Block> blocks;
	
	public BlockVisModule(VisModuleContainer vMContainer, Collection<Block> blocks)
	{
		super(vMContainer, "BlockView");
		this.blocks = blocks;
		this.attributes.put("color", "0xFF0000");
		this.attributes.put("show", "true");
		this.attributes.put("ids", "false");
		this.attributes.put("random color", "true");
		this.attributes.put("opacity", "180");
		this.attributes.put("show connections", "false");
		this.attributes.put("show connections #s", "false");
		this.attributes.put("area size", "false");
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		boolean show = this.parseBoolean("show", true);
		if(!show)
			return;
		boolean ids = this.parseBoolean("ids", false);
		
		boolean conn = this.parseBoolean("show connections", false);
		boolean connNo = this.parseBoolean("show connections #s", false);
		
		boolean randomColor = this.parseBoolean("random color", true);
		
		boolean areaSize = this.parseBoolean("area size", false);
		
		int opacity = this.parseInt("opacity", 180);
		Color c = Color.black;
		if(!randomColor)
		{
			c = this.parseColor("color", Color.red);
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), opacity);
		}
		int x,y,x2,y2;
		for(Block b : blocks)
		{
			if(randomColor)
				c = getRandomColor(opacity);
			g.setColor(c);
			if(!map.isVisible(b.x_mean, b.y_mean))
				continue;
			g.fillPolygon(getPolygon(map, b));
			g.setColor(g.getColor().darker().darker().darker());
			x = map.getXonPanel(b.x_mean);
			y = map.getYonPanel(b.y_mean);
			if(ids)
				g.drawString("Block: " + b.id, x+10, y+10);
			x += 2;
			y += 2;
			int counter = 0;
			int rOff = (int)map.getZoom();
			if(conn || connNo)
			{
				for(Node n : b.border)
				{
					
					x2 = map.getXonPanel(n);
					y2 = map.getYonPanel(n);
					if(conn)
						g.drawLine(x, y, x2, y2);
					if(connNo)
					{
						int mX = (x+x2)/2;
						int mY = (y+y2)/2;
						g.drawString(" " + ++counter, mX, mY);
					}
				}
			}
			if(areaSize)
			{
				x = map.getXonPanel(b.x_mean);
				y = map.getYonPanel(b.y_mean);
				g.drawString("area: " + b.area_size, x, y);
			}
		}
	}
	
	protected Polygon getPolygon(BasicMap map, Block b)
	{
		Polygon p = new Polygon();
		for(Node n : b.border)
		{
			p.addPoint(map.getXonPanel(n.getCoord().getX()), map.getYonPanel(n.getCoord().getY()));
		}
		return p;
	}
		
	protected Color getRandomColor(int opacity)
	{
		int r = rand.nextInt(192),
			g = rand.nextInt(192),
			b = rand.nextInt(192);
		return new Color(r, g, b, 200);
	}
	
}
