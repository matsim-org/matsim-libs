/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiFNDDrawer.java
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
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.VoronoiDensity;
import playground.gregor.sim2d_v4.cgal.VoronoiDensity.VoronoiCell;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import processing.core.PConstants;
import processing.core.PVector;
import be.humphreys.simplevoronoi.GraphEdge;

import com.vividsolutions.jts.geom.Envelope;

public class VoronoiFNDDrawer implements XYVxVyEventsHandler, VisDebuggerAdditionalDrawer, VisDebuggerOverlay{

	double time = -1;


	private final Map<Id,Double> velocities = new HashMap<Id,Double>();

	private final List<VoronoiCell> currentCells = new ArrayList<VoronoiCell>();
	private final List<VoronoiCell> newCells = new ArrayList<VoronoiCell>();

	private final QuadTree<XYVxVyEventImpl> quadTree; 

	//linear list --> slow --> should be a QuadTree 
	private static List<Envelope> envelopes = new ArrayList<Envelope>();
	static {
		Envelope e = new Envelope(15,25,-2,2);
		envelopes.add(e);

	}

	private final LinkPair lp;

	public VoronoiFNDDrawer() {
		this.quadTree = new QuadTree<XYVxVyEventImpl>(1, -2, 35, 2);
		this.lp = new LinkPair();

		double x = 20;
		double y = 0;
		double dx = 1;
		double dy = 0;
		//		dx /= lp.length;
		//		dy /= lp.length;

		this.lp.x = x - 3*dy + 0.25*dx;
		this.lp.y = y + 3*dx + 0.25*dy;

		this.lp.x1 = x - 3*dy - 5.25*dx;
		this.lp.y1 = y + 3*dx - 5.25*dy;

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.time) {
			processFrame();
			this.time = event.getTime();
		}

		if (event.getX() <= 1|| event.getX() >= 35 || event.getY() <= -2 || event.getY() >= 2) {
			return;
		}
		this.quadTree.put(event.getX(), event.getY(), event);


	}

	private void processFrame() {
		for (Envelope e : envelopes) {
			List<XYVxVyEventImpl> events = new ArrayList<XYVxVyEventImpl>();
			this.quadTree.get(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY(), events);

			if (events.size() > 1) {
				processEnvelope(events,e);
			}
		}

		synchronized(this.currentCells) {
			this.currentCells.clear();
			this.currentCells.addAll(this.newCells);
			this.newCells.clear();
		}

		this.quadTree.clear();
	}

	private void processEnvelope(List<XYVxVyEventImpl> events, Envelope e) {
		double [] x = new double [events.size()];
		double [] y = new double [events.size()];


		for (int i = 0; i < events.size(); i++) {
			x[i] = events.get(i).getX();
			y[i] = events.get(i).getY();
		}

		
		VoronoiDensity vd = new VoronoiDensity(CGAL.EPSILON, e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		List<VoronoiCell> cells = vd.computeVoronoiDensity(x, y);

		double area = 0;
		int p = 0;
		int cnt = 0;
		double v = 0;
		double j = 0;
		for (VoronoiCell c : cells) {
			if (!Double.isInfinite(c.area)) {
				area += c.area;
				p++;
				XYVxVyEventImpl ee = events.get(cnt);
				double vx = ee.getVX();
				double vy = ee.getVY();
				Double vv = this.velocities.get(ee.getPersonId());
				if (vv == null) {
					vv = vx;
					this.velocities.put(ee.getPersonId(), vv);
				} else {
					vv = 0.9*vv + 0.1*vx;
					this.velocities.put(ee.getPersonId(), vv);
				}
				double cv = Math.abs(vv);//Math.sqrt(vx*vx+vy*vy);
				v += cv*c.area;
				j += (cv * 1/c.area)*c.area;
			}
			cnt++;
		}

		double rho = p/area;
		v /= area;
		j /= area;
		//		System.out.println("rho: " + rho + "  v: " + v + "  j: " + j);

		
		
		synchronized (this.lp.dataPoints) {
			double roundRho = (int)rho + ((int)(50*rho))/50.;
			
			DataPoint dp = this.lp.dataPoints.get(roundRho);
			if (dp == null) {
				dp = new DataPoint();
				this.lp.dataPoints.put(roundRho, dp);
			}
			
			dp.density = dp.cnt/(dp.cnt+1.) * dp.density + 1./(dp.cnt+1)*rho;
			dp.speed = dp.cnt/(dp.cnt+1.) * dp.speed + 1./(dp.cnt+1)*v;
			dp.flow = dp.cnt/(dp.cnt+1.) * dp.flow + 1./(dp.cnt+1)*j;
			dp.cnt++;

			
		}
		this.newCells.addAll(cells);

	}


	@Override
	public void draw(EventsBasedVisDebugger p) {
		p.strokeWeight(.05f);
		p.stroke(255,0,0,128);
		synchronized (this.currentCells) {
			for (VoronoiCell c : this.currentCells) {
				if (Double.isInfinite(c.area)) {
					continue;
				}
				for (GraphEdge e : c.edges) {

					float x0 = (float) (e.x1 + p.offsetX);
					float y0 = -(float) (e.y1 + p.offsetY);
					float x1 = (float) (e.x2 + p.offsetX);
					float y1 = -(float) (e.y2 + p.offsetY);
					p.line(x0, y0, x1, y1);

				}
			}
		}

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

//		synchronized(this.currentCells) {
//			for (VoronoiCell c : this.currentCells) {
//				//				if (c) {
//				//					continue;
//				//				}
//				float x = (float) (c.x + p.offsetX);
//				float y = -(float) (c.y + p.offsetY);
//				PVector cv = p.zoomer.getCoordToDisp(new PVector(x, y));
//				p.text(c.area+"", cv.x, cv.y);
//
//			}
//		}
//		long start = System.nanoTime();

		synchronized (this.lp.dataPoints) {
			drawSpeedFND(p, this.lp, fs, sz);
			drawFlowFND(p, this.lp, fs, sz);
		}
//		long stop = System.nanoTime();
//		System.out.println("took: " + ((stop-start)/1000) + "  points:" + this.lp.dataPoints.size());
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

	private static final class DataPoint {
		double density;
		double flow;
		double speed;
		int discount;
		
		int cnt = 0;
	}

	private static final class LinkPair {

		public double x1;
		public double y1;
		public double x;
		public double y;
		public Map<Double,DataPoint> dataPoints = new TreeMap<Double,DataPoint>();


	}
}
