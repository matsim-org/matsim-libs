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

import java.awt.Color;
import java.awt.Graphics;

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
import playground.rost.graph.visnetwork.OneWayLink;
import playground.rost.graph.visnetwork.OneWayVisNetwork;

public class FlowLinkVisModule extends AbstractVisModuleImpl {

	protected NetworkLayer network;
	protected OneWayVisNetwork oWVN;
	protected final Flow flow;
	protected final TimeControl tControl;
	protected final Node sink;

	public FlowLinkVisModule(VisModuleContainer vMContainer, NetworkLayer network, OneWayVisNetwork oWVN, TimeControl timeControl, Flow flow)
	{
		super(vMContainer, "FlowLinkView");
		this.flow = flow;
		this.tControl = timeControl;
		this.network = network;
		this.oWVN = oWVN;
		sink = network.getNodes().get(new IdImpl(GlobalFlowCalculationSettings.superSinkId));
		this.attributes.put("show", "true");
		this.attributes.put("ids", "false");
		this.attributes.put("length", "false");
		this.attributes.put("print flow", "true");
		this.attributes.put("show traversal time", "true");
	}

	@Override
	public void paintGraphics(BasicMap map, Graphics g) {
		boolean show = this.parseBoolean("show", true);
		if(!show)
			return;
		boolean ids = this.parseBoolean("ids", false);
		boolean length = this.parseBoolean("length", false);
		boolean printFlow = this.parseBoolean("print flow", true);
		boolean travelTime = this.parseBoolean("show traversal time", true);
		int size, x, y, x2, y2;
		int currentTime = this.tControl.getTime();
		boolean isForward;
		for(OneWayLink oWLink : oWVN.getOneWayLinks())
		{
			isForward = true;
			Link link = oWLink.getLink();
			if(link.getToNode().equals(sink))
				continue;
			if(!map.isVisible(link))
				continue;
			int flowValue = oWLink.getFlowAtTime(currentTime);
			if(flowValue < 0)
			{
				isForward = false;
				flowValue *= -1;
			}


			int cap = oWLink.getMaxCapacity();

			g.setColor(getColorForFlow(flowValue, cap));

			x = map.getXonPanel(link.getFromNode());
			y = map.getYonPanel(link.getFromNode());
			x2 = map.getXonPanel(link.getToNode());
			y2 = map.getYonPanel(link.getToNode());
			g.drawLine(x, y, x2, y2);

			if(flowValue != 0)
				this.drawArrow(g, x, y, x2, y2, isForward);

			if(ids || length || printFlow || travelTime)
			{
				g.setColor(Color.black);
				int mx = (x+x2)/2, my=(y+y2)/2;
				mx-=10;
				if(ids)
					g.drawString(link.getId().toString(), mx, my + 10);
				if(length)
					g.drawString(""+link.getLength(), mx, my + 5 );
				if(printFlow)
					g.drawString(flowValue + " / " + cap, mx + 5, my);
				if(travelTime)
					g.drawString("tau: " + link.getFreespeed(), mx+5, my+10);
			}
		}
	}

	protected void drawArrow(Graphics g, int x, int y, int x2, int y2, boolean isForward)
	{
		int mx, my, aX1, aX2, aY1, aY2;
		mx = (x+x2)/2;
		my = (y+y2)/2;
		if(x2-x == 0)
		{
			if(isForward)
			{
				aX1 = x-5;
				aX2 = x+5;
				my += 5;
				aY1 = my;
				aY2 = my;
			}
			else
			{
				aX1 = x-5;
				aX2 = x+5;
				my -= 5;
				aY1 = my;
				aY2 = my;
			}
		}
		else if(y2-y == 0)
		{
			if(isForward)
			{
				mx += 5;
				aX1 = mx;
				aX2 = mx;
				aY1 = y + 5;
				aY2 = y - 5;
			}
			else
			{
				mx -= 5;
				aX1 = mx;
				aX2 = mx;
				aY1 = y + 5;
				aY2 = y - 5;
			}
		}
		else
		{
			double delta = (double)(y2-y) / (double)(x2-x);
			double nDelta = -1.0 / delta;
			double dX = 10;
			double nDX = 20 / Math.sqrt(nDelta*nDelta + 1);
			aX1 = (int)(mx + nDX);
			aY1 = (int)(nDelta * (aX1 - mx) + my);
			aX2 = (int)(mx - nDX);
			aY2 = (int)(nDelta * (aX2 - mx) + my);
			if(isForward)
			{
				if(x2 > x)
				{
					mx += dX;
					if(mx > x2)
						mx = x2;
				}
				else
				{
					mx -= dX;
					if(mx < x2)
						mx = x2;
				}
			}
			else
			{
				if(x2 > x)
				{
					mx -= dX;
					if(mx < x)
						mx = x;
				}
				else
				{
					mx += dX;
					if(mx > x)
						mx = x;
				}
			}
			my = (int)(delta * (mx - x) + y);
		}
		g.drawLine(mx, my, aX1, aY1);
		g.drawLine(mx, my, aX2, aY2);
	}

	protected Color getColorForFlow(int flow, int cap)
	{
		int farbgrad = (int)( 512.0 * flow / cap);
		if(farbgrad < 256)
		{
			return new Color(farbgrad,255,0);
		}
		else if(farbgrad < 512)
		{
			return new Color(255, 255 - farbgrad % 256, 0);
		}
		else
		{
			return Color.black;
		}
	}

}
