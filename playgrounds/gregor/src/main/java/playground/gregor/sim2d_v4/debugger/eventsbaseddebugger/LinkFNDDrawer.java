/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFNDDrawer.java
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import playground.gregor.boundarycondition.ScenarioGenerator;
import processing.core.PConstants;
import processing.core.PVector;


public class LinkFNDDrawer implements VisDebuggerAdditionalDrawer,
LinkEnterEventHandler, LinkLeaveEventHandler {


	private static Set<Id> linkIds = new HashSet<Id>();
	static {
//		linkIds.add(new IdImpl("l1"));
//		linkIds.add(new IdImpl("l2d0"));
//		linkIds.add(new IdImpl("l2"));
//		linkIds.add(new IdImpl("l5b"));
//		linkIds.add(new IdImpl("l5"));
//		linkIds.add(new IdImpl("l6"));
//		linkIds.add(new IdImpl("t_l5b"));
//		linkIds.add(new IdImpl("t_l5"));
//		linkIds.add(new IdImpl("t_l6"));
	}



	private final Map<Id,LinkPair> linkPairs = new HashMap<Id,LinkPair>();

	public LinkFNDDrawer(Scenario sc) {

		for (Id id : linkIds) {
			Link l = sc.getNetwork().getLinks().get(id);
			Link rev = null;
			for (Link ll : l.getToNode().getOutLinks().values()) {
				if (ll.getToNode() == l.getFromNode()) {
					rev = ll;
					break;
				}
			}
			LinkPair lp = new LinkPair();
			lp.length = l.getLength();
			lp.area = l.getCapacity() /ScenarioGenerator.SEPC_FLOW*lp.length;

			double x = l.getCoord().getX();
			double y = l.getCoord().getY();
			double dx = l.getToNode().getCoord().getX() - l.getFromNode().getCoord().getX();
			double dy = l.getToNode().getCoord().getY() - l.getFromNode().getCoord().getY();
			dx /= lp.length;
			dy /= lp.length;
			
			lp.x = x - 3*dy + 0.25*dx;
			lp.y = y + 3*dx + 0.25*dy;

			lp.x1 = x - 3*dy - 5.25*dx;
			lp.y1 = y + 3*dx - 5.25*dy;
			
			this.linkPairs.put(l.getId(), lp);
			this.linkPairs.put(rev.getId(), lp);

		}


	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkPair lp = this.linkPairs.get(event.getLinkId());
		if (lp != null) {
			lp.onLink--;
			AgentInfo ai = lp.agentsOnLink.remove(event.getPersonId());

			double density = lp.onLink/lp.area;
//			if (Math.abs(density-ai.density) <  0.1) {
				ai.density += density;
				ai.density /= 2;
				double tt = event.getTime() - ai.enterTime;
				double speed = lp.length/tt;
				double flow = ai.density * speed;
				synchronized (lp.dataPoints) {
					double frac = ai.density - ((int)ai.density);
					frac = ((int)(frac*50))/50.;
					double rndDens = ((int)ai.density)+frac;
					DataPoint dp = lp.dataPoints.get(rndDens);
					if (dp == null) {
						dp = new DataPoint();
						lp.dataPoints.put(rndDens,dp);
					}
					dp.density = (dp.cnt/(dp.cnt+1.)) * dp.density + (1./(dp.cnt+1.)) * ai.density;
					dp.flow = (dp.cnt/(dp.cnt+1.)) * dp.flow + (1./(dp.cnt+1.)) * flow;
					dp.speed = (dp.cnt/(dp.cnt+1.)) * dp.speed + (1./(dp.cnt+1.)) * speed;
				}
//			}
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		LinkPair lp = this.linkPairs.get(event.getLinkId());
		if (lp != null) {
			lp.onLink++;
			AgentInfo ai = new AgentInfo();
			ai.enterTime = event.getTime();
			ai.density = lp.onLink/lp.area;
			
			if (event.getLinkId().toString().contains("rev")) {
				ai.discount =128;
			}
			lp.agentsOnLink.put(event.getPersonId(), ai);
			

		}
	}

	@Override
	public void draw(EventsBasedVisDebugger p) {

	}

	@Override
	public void drawText(EventsBasedVisDebugger p) {
		if (p.zoomer.getZoomScale() < 10 ) {
			return;
		}
		float sz = (float) (.05*p.zoomer.getZoomScale());
		float fs = (float)(.25 *p.zoomer.getZoomScale());
		p.textSize(fs);;
		p.textAlign(PConstants.CENTER);
		p.strokeWeight(sz);
		for (LinkPair lp : this.linkPairs.values()) {
			synchronized (lp.dataPoints) {
				
				
				drawFlowFND(p,lp,fs,sz);
				drawSpeedFND(p,lp,fs,sz);
		

			}
		}


	}

	private void drawSpeedFND(EventsBasedVisDebugger p, LinkPair lp, float fs,
			float sz) {
		float tx = (float)(lp.x1+p.offsetX);
		float ty = (float)-(lp.y1+p.offsetY);
		PVector cv = p.zoomer.getCoordToDisp(new PVector(tx,ty));
		p.pushMatrix();
		p.translate(cv.x, cv.y);
		p.stroke(0);
		p.fill(0);
		//x-axis 
		float xlength = (float) (5 * p.zoomer.getZoomScale());
		float arrow = (float) (.2 * p.zoomer.getZoomScale());
		p.line(0, 0, xlength, 0);
		p.triangle(xlength, 0-arrow/3, xlength+arrow, 0, xlength, arrow/3);
		float yheight = -(float) (2.5*1.5 * p.zoomer.getZoomScale());
		p.line(0, 0, 0, yheight);
		p.triangle(arrow/3, yheight, 0, yheight-arrow, -arrow/3, yheight);
		
		for (int i = 1; i < 5; i ++) {
			float x = (float) (i * p.zoomer.getZoomScale());
			p.line(x, 0, x, arrow);
			p.text(i, x, arrow+fs);
		}
		
		float x = (float) (5 * p.zoomer.getZoomScale());
		p.text("\u03C1 in m\u207B\u00B2", x, arrow+fs);
//		
		for (int i = 1; i <= 1.5; i++) {
			float y = -(float)(2.5 * i * p.zoomer.getZoomScale());
			p.line(0, y, -arrow, y);
			float len = p.textWidth(i+"");
			p.text(i,-arrow-len,y+fs/4);
		}
		float y = -(float)(2.5 * 1.5 * p.zoomer.getZoomScale());
		float len = p.textWidth("v in ms\u207B\u00B9");
		p.text("v in ms\u207B\u00B9",-arrow-len,y+fs/4);
		
		p.stroke(0,0);
		p.fill(0,0,255,255);
		for (DataPoint ai : lp.dataPoints.values()) {
			p.fill(0,0,255-ai.discount,255);
			p.ellipse((float)(ai.density*p.zoomer.getZoomScale()),(float)-(2.5*ai.speed*p.zoomer.getZoomScale()), sz, sz);
		}
		p.popMatrix();
		
	}

	private void drawFlowFND(EventsBasedVisDebugger p, LinkPair lp, float fs,
			float sz) {
		float tx = (float)(lp.x+p.offsetX);
		float ty = (float)-(lp.y+p.offsetY);
		PVector cv = p.zoomer.getCoordToDisp(new PVector(tx,ty));
		p.pushMatrix();
		p.translate(cv.x, cv.y);
		p.stroke(0);
		p.fill(0);
		//x-axis 
		float xlength = (float) (5 * p.zoomer.getZoomScale());
		float arrow = (float) (.2 * p.zoomer.getZoomScale());
		p.line(0, 0, xlength, 0);
		p.triangle(xlength, 0-arrow/3, xlength+arrow, 0, xlength, arrow/3);
		float yheight = -(float) (1.5*2.5 * p.zoomer.getZoomScale());
		p.line(0, 0, 0, yheight);
		p.triangle(arrow/3, yheight, 0, yheight-arrow, -arrow/3, yheight);
		
		for (int i = 1; i < 5; i ++) {
			float x = (float) (i * p.zoomer.getZoomScale());
			p.line(x, 0, x, arrow);
			p.text(i, x, arrow+fs);
		}
		
		float x = (float) (5 * p.zoomer.getZoomScale());
		p.text("\u03C1 in m\u207B\u00B2", x, arrow+fs);
//		
		for (int i = 1; i <= 2.5; i++) {
			float y = -(float)(1.5 * i * p.zoomer.getZoomScale());
			p.line(0, y, -arrow, y);
			float len = p.textWidth(i+"");
			p.text(i,-arrow-len,y+fs/4);
		}
		float y = -(float)(1.5 * 2.5 * p.zoomer.getZoomScale());
		float len = p.textWidth("J in (ms)\u207B\u00B9");
		p.text("J in (ms)\u207B\u00B9",-arrow-len,y+fs/4);
		
		p.stroke(0,0);
		
		for (DataPoint ai : lp.dataPoints.values()) {
			p.fill(0,255-ai.discount,0,255);
			p.ellipse((float)(ai.density*p.zoomer.getZoomScale()),(float)-(1.5*ai.flow*p.zoomer.getZoomScale()), sz, sz);
		}
		p.popMatrix();
		
	}

	private static final class LinkPair {

		public double x1;
		public double y1;
		public double x;
		public double y;
		public double area;
		public double length;
		public int onLink;
		public Map<Double,DataPoint> dataPoints = new TreeMap<Double,DataPoint>();
		public final Map<Id,AgentInfo> agentsOnLink = new HashMap<Id,AgentInfo>();


	}

	private static final class AgentInfo {

		public int discount = 0;
		public double flow;
		public double speed;
		public double density;
		double enterTime;
	}

	private static final class DataPoint {
		double density;
		double flow;
		double speed;
		int discount;
		
		int cnt = 0;
	}
}
