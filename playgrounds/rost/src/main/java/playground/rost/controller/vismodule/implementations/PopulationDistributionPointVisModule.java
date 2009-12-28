/******************************************************************************
 *project: org.matsim.*
 * PopulationDistributionPointVisModule.java
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

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.map.PlacePplMap;
import playground.rost.controller.uicontrols.PopulationDensityColorGradient;
import playground.rost.controller.vismodule.AbstractVisModuleImpl;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.graph.populationpoint.PopulationPoint;

public class PopulationDistributionPointVisModule extends AbstractVisModuleImpl {

	PlacePplMap map;
	
	public PopulationDistributionPointVisModule(VisModuleContainer vMContainer, PlacePplMap map)
	{
		super(vMContainer, "PopulationPoints");
		this.attributes.put("size", "40");
		this.attributes.put("show", "true");
		this.map = map;
	}
	
	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		boolean show = this.parseBoolean("show", true);
		if(!show)
			return;
		int size = this.parseInt("size", 20);
		int calcSize;
		int halfCalcSize;
		for(PopulationPoint p : this.map.getPplPoints().get())
		{
			if(!map.isVisible(p.point.x, p.point.y))
				continue;
			int x = map.getXonPanel(p.point.x);
			int y = map.getYonPanel(p.point.y);
			Color c = PopulationDensityColorGradient.getColor(p.population, this.map.getMaxDensity());
			g.setColor(c);
			calcSize = (int)(Math.max(size, (map.getZoom() * size)));
			halfCalcSize = calcSize/2;
			g.fillOval(x-halfCalcSize, y-halfCalcSize, calcSize, calcSize);
			g.setColor(Color.black);
			g.drawOval(x-halfCalcSize-1, y-halfCalcSize-1, calcSize+1, calcSize+1);
			g.setColor(Color.black);
			g.drawString(""+p.population, x-halfCalcSize+2, y+4);
		}
	}
	
}
