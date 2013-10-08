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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;

import processing.core.PConstants;
import processing.core.PVector;



public class QSimQDrawer implements VisDebuggerAdditionalDrawer, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{

	private static final Logger log = Logger.getLogger(QSimQDrawer.class);
	
	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();

	private final double minScale = 10;
	
	public QSimQDrawer(Scenario sc) {
		for (Link l : sc.getNetwork().getLinks().values()) {
		if (l.getAllowedModes().contains("walk2d")){
			continue;
		}

		float width = (float) (l.getCapacity()/sc.getNetwork().getCapacityPeriod()/1.3);

		double dx = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
		double dy = l.getToNode().getCoord().getY() - l.getFromNode().getCoord().getY();
		double length = Math.sqrt(dx*dx + dy*dy);
		dx /= length;
		dy /= length;
		LinkInfo info = new LinkInfo();
		info.id = l.getId();
		info.slots = (int) ((l.getLength()/((NetworkImpl)sc.getNetwork()).getEffectiveCellSize())*l.getNumberOfLanes());
		info.lanes = (int) l.getNumberOfLanes();
		info.cellSize = l.getLength()/(info.slots/info.lanes);
		info.laneWidth = width/info.lanes;
		info.length = l.getLength();
		
		double x0 = l.getFromNode().getCoord().getX();
		double y0 = l.getFromNode().getCoord().getY();
		double x1 = l.getToNode().getCoord().getX();
		double y1 = l.getToNode().getCoord().getY();
		
//		x0 += dy * width/2;
//		x1 += dy * width/2;
//		y0 -= dx * width/2;
//		y1 -= dx * width/2;
		
		info.width = width;
		info.x0 = x0;
		info.y0 = y0;
		info.x1 = x1;
		info.y1 = y1;
		info.dx = dx;
		info.dy = dy;
		
		double tan = dx/dy;
		double atan = Math.atan(tan);
		if (atan >0) {
			atan -= Math.PI/2;
		} else {
			atan += Math.PI/2;
		}
		
		
		double offsetX = dy * .075;
		double offsetY = -dx * .075;
		if (dx > 0) {
			offsetX *= -1;
			offsetY *= -1;
		}
		
		
		info.tx = x0+offsetX;
		info.ty = y0+offsetY;
		info.text = "0";
		info.atan = atan;
		this.links.add(info);
		this.map.put(l.getId(), info);
	}
	}
	
	

	@Override
	public void draw(EventsBasedVisDebugger p) {


		
		p.stroke(0);
		p.ellipseMode(PConstants.RADIUS);
//		p.strokeCap(PConstants.ROUND);
		p.strokeCap(PConstants.SQUARE);
		p.strokeWeight(.1f);
		p.fill(0);
		for (LinkInfo li : this.links) {
//			p.line((float)(li.x0+p.offsetX),(float)-(li.y0+p.offsetY),(float)(li.x1+p.offsetX),(float)-(li.y1+p.offsetY));
			p.fill(255);
			p.rect((float)(li.x0+p.offsetX-li.dy*2-3*li.dx-.06),(float)-(li.y0+p.offsetY+li.dx*2-3*li.dy),(4),(4));
			
			
////			int lines = li.onLink/li.lanes+1;
//			if (li.onLink <= 0) {
//				continue;
//			}
//			double incr = li.length/lines;
//			double delta = incr;
//			while (delta < li.length) {
//				double x0 = li.x0 + delta*li.dx;
//				double x1 = li.x1 + delta*li.dx;
//				double y0 = li.y0 + delta*li.dy;
//				double y1 = li.y1 + delta*li.dy;
//				delta += incr;
//				p.strokeWeight((float)(.1*p.zoomer.getZoomScale()));
//				p.stroke(0,0);
////				p.strokeCap(PConstants.ROUND);
////				p.line((float)(x0+p.offsetX),(float)-(y0+p.offsetY),(float)(x1+p.offsetX),(float)-(y1+p.offsetY));
//				p.ellipseMode(PConstants.RADIUS);
//				p.ellipse((float)((x0+x1)/2 + p.offsetX), (float)-((y0+y1)/2 + p.offsetY), .35f, .35f);
////				p.strokeCap(PConstants.SQUARE);
////				p.stroke(0);
////				p.strokeWeight((float)(.1*p.zoomer.getZoomScale()));
////				p.line((float)(x0+p.offsetX),(float)-(y0+p.offsetY),(float)(x1+p.offsetX),(float)-(y1+p.offsetY));
//			}
//			p.strokeWeight(li.width);
//			p.stroke(0,0,0,255);
//			p.line((float)(li.x0+p.offsetX),(float)-(li.y0+p.offsetY),(float)(li.x1+p.offsetX),(float)-(li.y1+p.offsetY));
		}
//		p.strokeCap(PConstants.SQUARE);
	}


	@Override
	public void drawText(EventsBasedVisDebugger p) {
		if (p.zoomer.getZoomScale() < this.minScale ) {
			return;
		}
		for (LinkInfo li : this.links) {
		
			float ts = (float) (10*p.zoomer.getZoomScale()/this.minScale);
			p.textSize(ts);	
			PVector cv = p.zoomer.getCoordToDisp(new PVector((float)(li.tx+p.offsetX-1),(float)-(li.ty+p.offsetY)));
			p.fill(0,0,0,255);
			li.text = li.onLink + "";
			float w = p.textWidth(li.text);
			p.textAlign(PConstants.LEFT);
			p.text(li.text, cv.x-w/2, cv.y+ts/2);
		}
	}




//
//
	private static final class LinkInfo {
		public double tx;
		public double ty;
		public String text;
		public double atan;
		public double length;
		public double laneWidth;
		public double dy;
		public double dx;
		public double cellSize;
		public int lanes;
		public Id id;
		public int r;
		public int b;
		public int g;
		public int a = 255;
		int slots;
		float width;
		double x0;
		double x1;
		double y0;
		double y1;
		int onLink;
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
		}		
	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
		}		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
		}
	}



	@Override
	public void handleEvent(ActivityEndEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
		}
	}



	@Override
	public void handleEvent(ActivityStartEvent event) {
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
		}		
	}










}
