/******************************************************************************
 *project: org.matsim.*
 * LinkVisModule.java
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.uicontrols.TimeControl;
import playground.rost.controller.vismodule.AbstractVisModuleImpl;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.eaflow.ea_flow.Flow;
import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings;
import playground.rost.eaflow.ea_flow.TimeExpandedPath;

public class TimeExpandedPathVisModule extends AbstractVisModuleImpl {

	protected NetworkLayer network;
	protected final Flow flow;
	protected final TimeControl tControl;
	protected final Map<Integer, Set<TimeExpandedPath>> timeExpandedPathsStartAtMap = new HashMap<Integer, Set<TimeExpandedPath>>();
	protected final Node sink;

	public TimeExpandedPathVisModule(VisModuleContainer vMContainer, NetworkLayer network, TimeControl timeControl, Flow flow)
	{
		super(vMContainer, "FlowLinkView");
		sink = network.getNodes().get(new IdImpl(GlobalFlowCalculationSettings.superSinkId));
		this.flow = flow;
		this.tControl = timeControl;
		this.network = network;

		calculateMap();

		this.attributes.put("show", "true");
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics oldG) {
		Graphics2D g = (Graphics2D)oldG;
		boolean show = this.parseBoolean("show", true);
		if(!show)
			return;
		boolean ids = this.parseBoolean("ids", false);
		int size, x, y, x2, y2;
		int currentTime = this.tControl.getTime();
		Set<TimeExpandedPath> tEPathSet = timeExpandedPathsStartAtMap.get(currentTime);
		if(tEPathSet == null || tEPathSet.isEmpty())
			return;
		int count = 0;
		Map<Link, Integer> countLinkUsed = new HashMap<Link, Integer>();
		for(TimeExpandedPath tEPath : tEPathSet)
		{
			Color color = getNewColorForPath(++count);
			g.setColor(color);
			for(TimeExpandedPath.PathEdge pEdge : tEPath.getPathEdges())
			{
				Link link = pEdge.getEdge();
				if(link.getToNode().equals(sink))
					continue;
				if(!map.isVisible(link))
					continue;
				int flowValue = flow.getFlow().get(link).getFlowAt(currentTime);
				int cap = (int)link.getCapacity();
				int countUsed = 1;
				if(countLinkUsed.containsKey(link))
					countUsed = countLinkUsed.get(link)+1;
				countLinkUsed.put(link, countUsed);
				g.setStroke(new BasicStroke(5));

				x = map.getXonPanel(link.getFromNode());
				y = map.getYonPanel(link.getFromNode());
				x2 = map.getXonPanel(link.getToNode());
				y2 = map.getYonPanel(link.getToNode());
				if(x2-x == 0)
				{
					x += countUsed * 5;
					x2 += countUsed * 5;
				}
				else if(y2-y == 0)
				{
					y += countUsed *5;
					y2 += countUsed * 5;
				}
				else
				{
					double delta = (double)(x2-x) / (double)(y2-y);
					double nDelta = -1.0 / delta;
					double dX = 5 / nDelta;
					int newX = (int)(x + dX * countUsed);
					int newY = (int)(nDelta * (newX - x) + y);
					int newX2 = (int)(x2 + dX * countUsed);
					int newY2 = (int)(nDelta * (newX2 - x2) + y2);
					x = newX;
					y = newY;
					x2 = newX2;
					y2 = newY2;
				}
				g.drawLine(x, y, x2, y2);
			}
		}
		g.setStroke(new BasicStroke(1));
	}

	protected Color getNewColorForPath(int count)
	{
		int newCount = 91*count;
		int red = newCount % 256;
		int green = ((newCount / 256) * 91) % 256;
		int blue = ((newCount / (256*256)) * 91) % 256;
		return new Color(red, green, blue);
	}

	protected void calculateMap()
	{
		Set<TimeExpandedPath> tEPathSet;
		int startTime;
		for(TimeExpandedPath tEPath : flow.getPaths())
		{
			startTime = tEPath.getStartTime();
			tEPathSet = timeExpandedPathsStartAtMap.get(startTime);
			if(tEPathSet != null)
			{
				tEPathSet.add(tEPath);
			}
			else
			{
				tEPathSet = new HashSet<TimeExpandedPath>();
				tEPathSet.add(tEPath);
				timeExpandedPathsStartAtMap.put(startTime, tEPathSet);
			}
		}
	}
}
