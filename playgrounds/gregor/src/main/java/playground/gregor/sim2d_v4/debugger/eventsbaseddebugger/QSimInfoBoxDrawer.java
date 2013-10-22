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
import org.matsim.api.core.v01.events.Event;
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

import processing.core.PConstants;
import processing.core.PVector;



public class QSimInfoBoxDrawer implements VisDebuggerAdditionalDrawer, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{

	private static final Logger log = Logger.getLogger(QSimInfoBoxDrawer.class);

	List<LinkInfo> links = new ArrayList<LinkInfo>();
	Map<Id,LinkInfo> map = new HashMap<Id,LinkInfo>();

	private final double minScale = 1.6;
	private final double detailMinScale = 20;
	private final double fs = 1.5;

	//	private final double boxSize = 4;
	private final double boxWidth = 4;
	private final double boxLength = 5.5;

	double lastUpdate = 0;

	public QSimInfoBoxDrawer(Scenario sc) {
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
			info.length = l.getLength();
			info.area = l.getLength() * width;
			info.id = l.getId();

			double x0 = l.getFromNode().getCoord().getX();//+dy/2;
			double y0 = l.getFromNode().getCoord().getY();//-dx/2;
			double x1 = l.getToNode().getCoord().getX();//+dy/2;
			double y1 = l.getToNode().getCoord().getY();//-dx/2;


			info.width = width;
			info.x0 = x0;
			info.y0 = y0;
			info.x1 = x1;
			info.y1 = y1;
			info.length = length;

			//		if ()

			double boxStart = info.length*1/2+.25;
			//			double boxStart = info.length*1/2+.25-4;
			double diff = length - (boxStart+this.boxLength);
			if (diff < .5) {
				double mv = .5 - diff;
				boxStart -= mv;
			}

			info.rx0 = x0+boxStart*dx-dy*this.boxWidth/2;
			info.ry0 = y0+boxStart*dy+dx*this.boxWidth/2;



			double tan = dx/dy;
			double atan = Math.atan(tan);
			//		if (atan >0) {
			//			atan -= Math.PI/2;
			//		} else {
			atan += Math.PI/2;
			//		}
			double theta = 0.0;
			if (dx > 0) {
				theta = Math.PI+Math.atan(dx/dy);
			} else if (dx < 0) {
				theta =  Math.atan(dx/dy);
			}
			theta  -= 1.5*Math.PI;

			if (theta < 0.0) theta += 2*Math.PI;
			if (theta < Math.PI/2) theta += Math.PI;
			else if (theta > Math.PI && theta < 1.5*Math.PI) theta -= Math.PI;


			//		double offsetX = dy * .075;
			//		double offsetY = -dx * .075;
			//		if (dx > 0) {
			//			offsetX *= -1;
			//			offsetY *= -1;
			//		}
			//		
			//		
			//		info.tx = x0+offsetX;
			//		info.ty = y0+offsetY;
			info.text = "0";
			info.atan = theta;
			this.links.add(info);
			this.map.put(l.getId(), info);
		}
	}



	@Override
	public void draw(EventsBasedVisDebugger p) {
		if (p.zoomer.getZoomScale() < this.minScale ) {
			return;
		}

		p.ellipseMode(PConstants.RADIUS);
		//		


		p.fill(0);
		for (LinkInfo li : this.links) {

			p.strokeCap(PConstants.ROUND);
			p.strokeWeight(.9f);
			p.stroke(255);
			p.line((float)(li.x0+p.offsetX),(float)-(li.y0+p.offsetY),(float)(li.x1+p.offsetX),(float)-(li.y1+p.offsetY));
			p.strokeCap(PConstants.SQUARE);
			p.stroke(0);
			p.strokeWeight(.5f);
			p.line((float)(li.x0+p.offsetX),(float)-(li.y0+p.offsetY),(float)(li.x1+p.offsetX),(float)-(li.y1+p.offsetY));
			//			p.fill(255);
			//			p.rect((float)(li.x0+p.offsetX-li.dy*2-3*li.dx-.06),(float)-(li.y0+p.offsetY+li.dx*2-3*li.dy),(4),(4));



			//			p.beginShape();

			//			p.vertex(tx, ty);
			//			p.vertex((float) (tx+li.dx*this.boxSize),ty);
			//			p.vertex((float) (tx+li.dx*this.boxSize),(float) (ty+li.dy*this.boxSize));
			//			p.vertex(tx,(float) (ty+li.dy*this.boxSize));
			//			p.vertex(tx,ty);
			//			p.endShape();
		}
	}


	@Override
	public void drawText(EventsBasedVisDebugger p) {
		if (p.zoomer.getZoomScale() < this.minScale ) {
			return;
		}
		for (LinkInfo li : this.links) {

			if (li.length < this.boxLength+1) {
				continue;
			}
			//			final double round = 1/10;
			float round = (float) (1/10.*p.zoomer.getZoomScale());
			float tx = (float)(li.rx0+p.offsetX);
			float ty = (float)-(li.ry0+p.offsetY);
			PVector cv = p.zoomer.getCoordToDisp(new PVector(tx,ty));
			float bx = cv.x + (float)(this.boxLength*p.zoomer.getZoomScale());
			float by = cv.y + (float)(this.boxWidth*p.zoomer.getZoomScale());
			if (cv.x < 0 || cv.x > p.displayWidth || cv.y < 0 || cv.y > p.displayHeight) {
				if (cv.x < 0 || cv.x > p.displayWidth || by < 0 || by > p.displayHeight) {
					if (bx < 0 || bx > p.displayWidth || by < 0 || by > p.displayHeight) {
						if ((bx < 0 || bx > p.displayWidth || cv.y < 0 || cv.y > p.displayHeight)) {
							continue;
						}
					}
				}
			}
			p.pushMatrix();
			p.translate(cv.x, cv.y);
			p.rotate((float) li.atan);
			p.strokeWeight((float) (.2*p.zoomer.getZoomScale()));
			p.stroke(255);
			p.fill(0, 0);
			p.rect(0, 0, (float)(this.boxLength*p.zoomer.getZoomScale()), (float)(this.boxWidth*p.zoomer.getZoomScale()),round);
			//			p.rect(0, (float)(.19*p.zoomer.getZoomScale()),  (float)((this.boxLength)*p.zoomer.getZoomScale()),(float)((this.boxWidth-.19)*p.zoomer.getZoomScale()),0,0,round,round);
			p.fill(255);
			p.stroke(0);
			p.strokeWeight((float) (.1*p.zoomer.getZoomScale()));
			p.rect(0, 0, (float)(this.boxLength*p.zoomer.getZoomScale()), (float)(this.boxWidth*p.zoomer.getZoomScale()),round);

			p.stroke(0);
			p.fill(0);
			//			p.strokeJoin(PConstants.ROUND);
			double arrowLength = .8;
			float lx0 = (float)(1*p.zoomer.getZoomScale());
			float ly0 = (float)((this.boxWidth-.5)*p.zoomer.getZoomScale());
			float lx1 = (float)((this.boxLength-1-arrowLength)*p.zoomer.getZoomScale());
			float ly1 = ly0;
			p.line(lx0, ly0, lx1, ly1);
			float tx0 = lx1;
			float ty0 = (float) (ly0-.2*p.zoomer.getZoomScale());
			float tx1 = tx0;
			float ty1 = (float) (ty0 + .4 * p.zoomer.getZoomScale());
			float tx2 = (float) (tx0 + arrowLength * p.zoomer.getZoomScale());
			float ty2  =(float) (ty0 + .2 * p.zoomer.getZoomScale());
			p.triangle(tx0, ty0, tx1, ty1, tx2, ty2);
			//			p.strokeJoin(PConstants.NORMAL);
			float ts = (float) (this.fs*p.zoomer.getZoomScale());


			//text;
			if (p.zoomer.getZoomScale() < this.detailMinScale) {
				p.textSize(ts);	
				p.fill(0,0,0,255);
				li.text = li.onLink + "";
				float w = p.textWidth(li.text);
				float shiftX = (float) ((this.boxLength*p.zoomer.getZoomScale() - w)/2);
				float shiftY = (float) ((this.boxWidth*p.zoomer.getZoomScale() +ts/2)/2);
				//			System.out.println(w);
				p.textAlign(PConstants.LEFT);
				//			p.text(li.text, cv.x-w/2, cv.y+ts/2);

				if (li.atan > Math.PI/2 && li.atan < 1.5 * Math.PI) {
					float cx = shiftX + w/2;
					float cy = shiftY + ts/2;
					p.translate(cx, cy);
					p.rotate((float) Math.PI);
					p.text(li.text, -w/2, ts);
				} else {
					p.text(li.text, shiftX, shiftY);
				}
			} else {
				float fs = ts/3;
				p.textSize(fs);
				p.fill(0,0,0,255);
				li.text = "#agents: "+li.onLink;

				float dty0 = (float) ((.5) * p.zoomer.getZoomScale())+fs;
				if (li.atan > Math.PI/2 && li.atan < 1.5 * Math.PI) {
					p.translate((float) (lx1+arrowLength*p.zoomer.getZoomScale()),(float) ((this.boxWidth-.5) * p.zoomer.getZoomScale())-fs);
					p.rotate((float)Math.PI);
				} else {
					p.translate(lx0, 0);
				}


				p.text(li.text, 0, dty0);
				float dty1 = (float) (dty0 + 1.5*fs);
				p.text(li.dens, 0, dty1);
				float dty2 = dty0 + 3*fs;
				p.text(li.flowStr,0,dty2);
			}
			p.popMatrix();
			if (p.zoomer.getZoomScale() >= this.detailMinScale ) {
				p.pushMatrix();
//				p.translate(cv.x, cv.y);
//				System.out.println(li.atan);
				if (li.atan >= Math.PI) {
					p.translate(cv.x, cv.y+(float)(this.boxWidth*p.zoomer.getZoomScale()));
					p.rotate((float) li.atan);
				} else {
					p.translate(cv.x-(float)(this.boxLength*p.zoomer.getZoomScale()), cv.y);
					p.rotate((float) li.atan);
					p.rotate((float)Math.PI);
					
				}
//				p.translate(0, (float)(this.boxWidth*p.zoomer.getZoomScale()));
//				p.strokeWeight((float) (.2*p.zoomer.getZoomScale()));
//				p.stroke(255);
//				p.fill(0, 0);
//				p.rect(0, 0, (float)(this.boxLength*p.zoomer.getZoomScale()), (float)(this.boxWidth*p.zoomer.getZoomScale()),round);
//				//			p.rect(0, (float)(.19*p.zoomer.getZoomScale()),  (float)((this.boxLength)*p.zoomer.getZoomScale()),(float)((this.boxWidth-.19)*p.zoomer.getZoomScale()),0,0,round,round);
//				p.fill(255);
//				p.stroke(0);
//				p.strokeWeight((float) (.1*p.zoomer.getZoomScale()));
//				p.rect(0, 0, (float)(this.boxLength*p.zoomer.getZoomScale()), (float)(this.boxWidth*p.zoomer.getZoomScale()),round);
//
//				p.stroke(0);
//				p.fill(0);
				
//				p.translate(0, (float)(this.boxWidth*p.zoomer.getZoomScale()));
				
				p.popMatrix();
			}




		}
	}




	//
	//
	private static final class LinkInfo {
		public Id id;
		public double area;
		public double ry0;
		public double rx0;
		public String text;
		public double atan;
		public double length;
		float width;
		double x0;
		double x1;
		double y0;
		double y1;
		int onLink;
		public String dens = "\u03C1= 0 m\u207B\u00B2";
		public double flow;
		public double avgFlow = 0.;
		public String flowStr = "J = 0.0 (ms)\u207B\u00B9";
		public double avgDens = 0.;

		
	}





	@Override
	public void reset(int iteration) {
		for (LinkInfo li :this.links) {
			li.onLink = 0;
			double dens = li.onLink/li.area;
			int tmp0 = (int) dens;
			int tmp1 = (int) (dens*100-tmp0*100);


			li.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		}

	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		update(event);

		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink--;
			double dens = info.onLink/info.area;
			int tmp0 = (int) dens;
			int tmp1 = (int) (dens*100-tmp0*100);

			info.flow++;

			info.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		}

	}


	private void update(Event event) {
		if (event.getTime() >= (this.lastUpdate+2)) {
			double timeSpan = event.getTime()-this.lastUpdate;
			for ( LinkInfo li : this.map.values()) {
				synchronized(li) {
					li.avgFlow = 0.9 * li.avgFlow + 0.1 * (li.flow/timeSpan)/li.width;
					int tmp0 = (int)li.avgFlow;
					int tmp1 = (int)(li.avgFlow*100 - tmp0*100+.5);
					li.flowStr = "J = "+ tmp0+"." + tmp1 +" (ms)\u207B\u00B9";

					double dens = li.onLink/li.area;
					li.avgDens = 0.9 * li.avgDens + 0.1 * dens;
					
					li.flow = 0;
				}
			}

			this.lastUpdate = event.getTime();
		}

	}



	@Override
	public void handleEvent(LinkEnterEvent event) {
		update(event);
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
			double dens = info.onLink/info.area;
			int tmp0 = (int) dens;
			int tmp1 = (int) (dens*100-tmp0*100);


			info.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		}		
	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {
		update(event);
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			info.onLink++;
			double dens = info.onLink/info.area;
			int tmp0 = (int) dens;
			int tmp1 = (int) (dens*100-tmp0*100);


			info.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		}		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		update(event);
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		synchronized(info) {
			//			info.onLink--;
			double dens = info.onLink/info.area;
			int tmp0 = (int) dens;
			int tmp1 = (int) (dens*100-tmp0*100);


			info.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		}
	}



	@Override
	public void handleEvent(ActivityEndEvent event) {
		update(event);
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		//		synchronized(info) {
		//			info.onLink--;
		//			double dens = info.onLink/info.area;
		//			int tmp0 = (int) dens;
		//			int tmp1 = (int) (dens*100-tmp0*100);
		//			
		//			
		//			info.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		//		}
	}



	@Override
	public void handleEvent(ActivityStartEvent event) {
		update(event);
		LinkInfo info = this.map.get(event.getLinkId());
		if (info == null) {
			return;
		}
		//		synchronized(info) {
		//			info.onLink++;
		//			double dens = info.onLink/info.area;
		//			int tmp0 = (int) dens;
		//			int tmp1 = (int) (dens*100-tmp0*100);
		//			
		//			
		//			info.dens = "\u03C1=" + tmp0 + "." + tmp1+" m\u207B\u00B2";
		//		}		
	}

















}
