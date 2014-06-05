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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;



public class QSimVehiclesDrawer implements VisDebuggerAdditionalDrawer, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{

	private static final Logger log = Logger.getLogger(QSimVehiclesDrawer.class);
	
	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();

	private final double maxScale = 10;
	private final double fadeOutScale = 1.6;
	
	private PImage red;
	private PImage green;
	private PImage yellow;
	private PImage pedRed;
	private PImage pedGreen;
	private PImage pedYellow;

	private PImage flipRotPedYellow;

	private PImage flipRotPedGreen;

	private PImage flipRotPedRed;

	private PImage flipRotRed;

	private PImage flipRotGreen;

	private PImage flipRotYellow;
	
	public QSimVehiclesDrawer(Scenario sc) {
		for (Link l : sc.getNetwork().getLinks().values()) {
		if (l.getAllowedModes().contains("walk2d")||  l.getCapacity() >= 100000 || l.getFreespeed() > 100 || l.getId().toString().contains("el")) {
			continue;
		} 

		float width = (float) (l.getCapacity()/sc.getNetwork().getCapacityPeriod()/1.3);

		boolean isCar = false;
		if (l.getFreespeed() > 1.35) {
			width = (float)(l.getNumberOfLanes() * 3.5);
			isCar = true;
		} else {
			width = (float)(l.getNumberOfLanes() * 0.71);
		}
		
		double dx = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
		double dy = l.getToNode().getCoord().getY() - l.getFromNode().getCoord().getY();
		
		
		
		double length = Math.sqrt(dx*dx + dy*dy);
		dx /= length;
		dy /= length;
		
		
		LinkInfo info = new LinkInfo();
		if (dx > 0){
			info.angle = (float) Math.PI;
		} else if(dx < 0) {
			info.angle = 0;
		} 
		
		info.id = l.getId();
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
		info.dx = dx;
		info.dy = dy;
		info.length = length;
		
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
		
		info.tx = (x0+x1)/2+offsetX;
		info.ty = (y0+y1)/2+offsetY;
		info.text = "0";
		info.atan = atan;
		info.isCar = isCar;
		if (isCar) {
			info.cap = l.getLength()/7.5*l.getNumberOfLanes();
		}
		
		this.links.add(info);
		this.map.put(l.getId(), info);
	}
	}
	
	

	@Override
	public void draw(EventsBasedVisDebugger p) {

		
		if (this.red == null) {
			this.red = p.loadImage("/Users/laemmel/tmp/resources/red.png","png");
			this.green = p.loadImage("/Users/laemmel/tmp/resources/green.png","png");
			this.yellow = p.loadImage("/Users/laemmel/tmp/resources/yellow.png","png");
			this.pedRed = p.loadImage("/Users/laemmel/tmp/resources/pedRed.png","png");
			this.pedGreen = p.loadImage("/Users/laemmel/tmp/resources/pedGreen.png","png");
			this.pedYellow = p.loadImage("/Users/laemmel/tmp/resources/pedYellow.png","png");
			this.flipRotPedRed = p.loadImage("/Users/laemmel/tmp/resources/flipRotPedRed.png","png");
			this.flipRotPedGreen = p.loadImage("/Users/laemmel/tmp/resources/flipRotPedGreen.png","png");
			this.flipRotPedYellow = p.loadImage("/Users/laemmel/tmp/resources/flipRotPedYellow.png","png");
			this.flipRotRed = p.loadImage("/Users/laemmel/tmp/resources/flipRotRed.png","png");
			this.flipRotGreen = p.loadImage("/Users/laemmel/tmp/resources/flipRotGreen.png","png");
			this.flipRotYellow = p.loadImage("/Users/laemmel/tmp/resources/flipRotYellow.png","png");
		}
		

		
		
		float scale = 1;
		float alfa = 255;
		if (p.zoomer.getZoomScale() <= 2) {
			return;
//			scale += 0.5/p.zoomer.getZoomScale();
		} else if (p.zoomer.getZoomScale() <= 4){
			alfa=(float) (255*(p.zoomer.getZoomScale()-2)/2);
		}
		
		p.stroke(0);
//		p.strokeCap(PConstants.ROUND);
		p.strokeCap(PConstants.SQUARE);
		p.imageMode(PApplet.CENTER);
		
		for (LinkInfo li : this.links) {
			if (li.onLink > 0) {
					p.strokeWeight(li.width*scale);
			} else {
				p.strokeWeight(li.width);
			}
			p.stroke(0,0,0,alfa);//li.a * fade);
//			p.line((float)(li.x0+p.offsetX),(float)-(li.y0+p.offsetY),(float)(li.x1+p.offsetX),(float)-(li.y1+p.offsetY));
			
			double incr = li.length/(li.onLink+1);
			for (double mv = incr; mv < li.length; mv += incr) {
				p.pushMatrix();
				float mvX = (float) (mv*li.dx);
				float mvY = (float) (mv*li.dy);
				p.translate((float)(li.x0+p.offsetX+mvX),(float)-(li.y0+p.offsetY+mvY));
				p.rotate((float) li.atan+li.angle);
				if (li.isCar) {
				p.image(li.veh, 0,0,2.14f*li.width,1f*li.width);
				} else {
					p.image(li.veh, 0,0,0.5903f*li.width,1f*li.width);
				}
				p.popMatrix();
			}
		}
		p.imageMode(PApplet.CORNER);
//		p.strokeCap(PConstants.SQUARE);
	}


	@Override
	public void drawText(EventsBasedVisDebugger p) {
		
//		if (p.zoomer.getZoomScale() >= this.maxScale ) {
//			return;
//		}
//		for (LinkInfo li : this.links) {
//		
//			float ts = (float) (10*p.zoomer.getZoomScale()/this.minScale);
//			p.textSize(ts);	
//			PVector cv = p.zoomer.getCoordToDisp(new PVector((float)(li.tx+p.offsetX),(float)-(li.ty+p.offsetY)));
//			p.fill(0,0,0,255);
//			float w = p.textWidth(li.text);
//			p.textAlign(PConstants.LEFT);
//			p.text(li.text, cv.x-w/2, cv.y+ts/2);
//		}
	}




	private void updateColor(LinkInfo l) {
//		if (l.onLink > l.cap) {
//			l.cap = l.onLink;
//		}
		if (l.onLink > l.cap) {
//			log.warn("Assumed storage capacity is smaler than actual storage capacity.");
			l.cap = l.onLink;
		}
		double density =5.4*l.onLink/l.cap;
		if (density == 0) {
			l.r = 0;
			l.g = 0;
			l.b = 0;
			l.a =0;
		} else if (density < 0.25) {
			l.a = (int) (128 + 128 * (density/0.25));
			l.r = 0;
			l.g = 255;
			l.b = 0;
			if (l.isCar) {
				if (l.dx < 0) {
					l.veh = this.green;
				} else {
					l.veh = this.flipRotGreen;
				}
			} else {
				if (l.dx < 0) {
					l.veh = this.pedGreen;
				} else {
					l.veh = this.flipRotPedGreen;
				}
			}
		} else if (density < 1) {
			l.a = 255;
			l.g = 255;
			l.b = 0;
			l.r = (int) (255 * ((density-.25)/0.75));
			if (l.isCar) {
				if (l.dx < 0) {
					l.veh = this.green;
				} else {
					l.veh = this.flipRotGreen;
				}
			} else {
				if (l.dx < 0) {
					l.veh = this.pedGreen;
				} else {
					l.veh = this.flipRotPedGreen;
				}
			}
		} else if (density < 2) {
			l.a = 255;
			l.g = 255 - (int) (255 * ((density-1)));;
			l.b = 0;
			l.r = 255;			
			if (l.isCar) {
				if (l.dx < 0) {
					l.veh = this.yellow;
				} else {
					l.veh = this.flipRotYellow;
				}
			} else {
				if (l.dx < 0) {
					l.veh = this.pedYellow;
				} else {
					l.veh = this.flipRotPedYellow;
				}
			}
		} else {
			l.a = 255;
			l.g = 0;
			l.b = (int) (128 * (density-2)/3.4);
			l.r = 255 - l.b;
			if (l.isCar) {
				if (l.dx < 0) {
					l.veh = this.red;
				} else {
					l.veh = this.flipRotRed;
				}
			} else {
				if (l.dx < 0) {
					l.veh = this.pedRed;
				} else {
					l.veh = this.flipRotPedRed;
				}
			}
		}
	}
//
//
	private static final class LinkInfo {
		public PImage veh;
		public double dx;
		public double dy;
		public double length;
		public float angle;
		public boolean isCar;
		public Id id;
		public double atan;
		public String text;
		public double tx;
		public double ty;
		public int r = 0;
		public int b = 0;
		public int g = 0;
		public int a = 0;
		double cap;
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
//		if (event.getLinkId().toString().contains("el")){
//			throw new RuntimeException("ERROR");
//		}
		
		synchronized(info) {
			info.onLink--;
			info.text = Integer.toString(info.onLink);
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
			info.text = Integer.toString(info.onLink);
			updateColor(info);
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
			info.text = Integer.toString(info.onLink);
			updateColor(info);
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
			info.text = Integer.toString(info.onLink);
			updateColor(info);
		}
	}



	@Override
	public void handleEvent(ActivityEndEvent event) {
//		LinkInfo info = this.map.get(event.getLinkId());
//		if (info == null) {
//			return;
//		}
//		synchronized(info) {
//			info.onLink++;
//			info.text = Integer.toString(info.onLink);
//			updateColor(info);
//		}
	}



	@Override
	public void handleEvent(ActivityStartEvent event) {
//		LinkInfo info = this.map.get(event.getLinkId());
//		if (info == null) {
//			return;
//		}
//		synchronized(info) {
//			info.onLink++;
//			info.text = Integer.toString(info.onLink);
//			updateColor(info);
//		}		
	}










}
