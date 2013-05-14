/* *********************************************************************** *
 * project: org.matsim.*
 * QSimFibonacciPulser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.NetworkImpl;

import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventsBasedVisDebugger.Text;
import processing.core.PConstants;



public class QSimDensityDrawer implements VisDebuggerAdditionalDrawer, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler{

	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();
	
	public QSimDensityDrawer(Scenario sc) {
		for (Link l : sc.getNetwork().getLinks().values()) {
		if (l.getAllowedModes().contains("walk2d") || l.getCapacity() == 2340){
			continue;
		}

		float width = (float) (l.getCapacity()/sc.getNetwork().getCapacityPeriod()/1.3);

		double dx = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
		double dy = l.getToNode().getCoord().getY() - l.getFromNode().getCoord().getY();
		double length = Math.sqrt(dx*dx + dy*dy);
		dx /= length;
		dy /= length;
		LinkInfo info = new LinkInfo();
		info.cap = (l.getLength()/((NetworkImpl)sc.getNetwork()).getEffectiveCellSize())*l.getNumberOfLanes();

		double x0 = l.getFromNode().getCoord().getX();
		double y0 = l.getFromNode().getCoord().getY();

		double x1 = l.getToNode().getCoord().getX();
		double y1 = l.getToNode().getCoord().getY();
		
		x0 += dy * width/2;
		x1 += dy * width/2;
		y0 -= dx * width/2;
		y1 -= dx * width/2;
		
		info.width = width;
		info.x0 = x0;
		info.x1 = x1;
		info.y0 = y0;
		info.y1 = y1;
		
		info.t = new Text();
		
		this.links.add(info);
		this.map.put(l.getId(), info);
	}
	}
	
	

	@Override
	public void draw(EventsBasedVisDebugger p) {
		p.stroke(0);
		p.strokeCap(PConstants.SQUARE);
		for (LinkInfo li : this.links) {
			p.strokeWeight(li.width);
			p.stroke(li.r,li.g,li.b,li.a);
			p.line((float)(li.x0+p.offsetX),(float)-(li.y0+p.offsetY),(float)(li.x1+p.offsetX),(float)-(li.y1+p.offsetY));
		}
	}



	private void updateColor(LinkInfo l) {
//		if (l.onLink > l.cap) {
//			l.cap = l.onLink;
//		}
		double density =5.4*l.onLink/l.cap;
		if (density == 0) {
			l.r = 0;
			l.g = 0;
			l.b = 0;
			l.a = 255;
		} else if (density < 0.25) {
			l.a = (int) (128 + 128 * (density/0.25));
			l.r = 0;
			l.g = 255;
			l.b = 0;
		} else if (density < 1) {
			l.a = 255;
			l.g = 255;
			l.b = 0;
			l.r = (int) (255 * ((density-.25)/0.75));
		} else if (density < 2) {
			l.a = 255;
			l.g = 255 - (int) (255 * ((density-1)));;
			l.b = 0;
			l.r = 255;			
		} else {
			l.a = 255;
			l.g = 0;
			l.b = (int) (128 * (density-2)/3.4);
			l.r = 255 - l.b;
		}

	}
//
//
	private static final class LinkInfo {
		public int r;
		public int b;
		public int g;
		public int a = 255;
		double cap;
		float width;
		double x0;
		double x1;
		double y0;
		double y1;
		int onLink;
		Text t;
	}





	@Override
	public void reset(int iteration) {
		for (LinkInfo li :this.links) {
			li.onLink = 0;
		}

	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
			info.t.text = Integer.toString(info.onLink);
			updateColor(info);
		}

	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
			info.t.text = Integer.toString(info.onLink);
			updateColor(info);
		}		
	}


	@Override
	public void handleEvent(AgentDepartureEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
			info.t.text = Integer.toString(info.onLink);
			updateColor(info);
		}		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
			info.t.text = Integer.toString(info.onLink);
			updateColor(info);
		}
	}


}
