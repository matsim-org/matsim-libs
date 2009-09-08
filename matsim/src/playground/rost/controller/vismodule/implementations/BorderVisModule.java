/******************************************************************************
 *project: org.matsim.*
 * BorderVisModule.java
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

import java.awt.Graphics;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.graph.Border;

public class BorderVisModule extends SimplePolygonVisModule {

	protected Border border;
	
	public BorderVisModule(VisModuleContainer vMContainer, Border border)
	{
		super(vMContainer, "BorderView", null);
		this.border = border;
	}
	
	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		if(this.border == null || this.border.size() < 3)
			return;
		this.nodes = border.getDistHull();
		super.paintGraphics(map, g);
	}
}
