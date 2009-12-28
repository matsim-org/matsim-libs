/******************************************************************************
 *project: org.matsim.*
 * PopulationBlockVisModule.java
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
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Node;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.map.PlacePplMap;
import playground.rost.controller.uicontrols.PopulationDensityColorGradient;
import playground.rost.controller.vismodule.AbstractVisModuleImpl;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.graph.PopulateBlocks;
import playground.rost.graph.block.Block;
import playground.rost.graph.block.Blocks;

public class PopulationBlockVisModule extends AbstractVisModuleImpl {

	protected Random rand = new Random(System.currentTimeMillis());

	protected Blocks blocks;
	
	public PopulationBlockVisModule(VisModuleContainer vMContainer, Blocks blocks)
	{
		super(vMContainer, "PopulationBlockView");
		this.blocks = blocks;
		this.attributes.put("show", "true");
		this.attributes.put("ids", "false");
		this.attributes.put("show connections", "false");
		this.attributes.put("show ppl in block", "true");
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		if(!(map instanceof PlacePplMap))
			throw new RuntimeException("PopulationBlockVisModule can only work on PlacePplMap!");
		PlacePplMap pplMap = (PlacePplMap)map;
		boolean show = this.parseBoolean("show", true);
		if(!show)
			return;
		boolean ids = this.parseBoolean("ids", false);
		boolean conn = this.parseBoolean("show connections", false);
		boolean pplInBlock = this.parseBoolean("show ppl in block", true);
		Map<Block, Integer> populationOfBlock = PopulateBlocks.getPopulation(pplMap.getPplPoints(), blocks, pplMap.getNetwork());
		
		int x,y,x2,y2;
		for(Block b : blocks.getBlocks())
		{
			if(!pplMap.isVisible(b.x_mean, b.y_mean))
				continue;
			int density = populationOfBlock.get(b);
			Color c = getColorForBlock(pplMap, density);
			g.setColor(c);
			g.fillPolygon(getPolygon(pplMap, b));
			g.setColor(Color.black);
			x = map.getXonPanel(b.x_mean);
			y = map.getYonPanel(b.y_mean);
			if(ids)
				g.drawString("Block: " + b.id, x, y+10);
			if(pplInBlock)
			{
				g.drawString("ppl: " + (int)(b.area_size * density), x-20, y);
			}
			int counter = 0;
			if(conn)
			{
				for(Node n : b.border)
				{
					
					x2 = pplMap.getXonPanel(n);
					y2 = pplMap.getYonPanel(n);
					if(conn)
						g.drawLine(x, y, x2, y2);
				}
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
		
	protected Color getColorForBlock(PlacePplMap pplMap, int density)
	{
		return PopulationDensityColorGradient.getColor(density, pplMap.getMaxDensity());
	}
		
}
