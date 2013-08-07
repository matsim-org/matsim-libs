/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiDiagramDrawer.java
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
import java.util.List;

import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

public class VoronoiDiagramDrawer implements XYVxVyEventsHandler, ClockedVisDebuggerAdditionalDrawer, VisDebuggerOverlay{

	List<Double> x = new ArrayList<Double>();
	List<Double> y = new ArrayList<Double>();
	private List<GraphEdge> d = null;
	
	private final double minZoom = 0;
	@Override
	public void draw(EventsBasedVisDebugger p) {
		if (this.d == null || p.zoomer.getZoomScale() < this.minZoom) {
			return;
		}
		
//		p.fill(0);
//		p.textSize((float) (16/p.zoomer.getZoomScale()));
//		for (int i = 0; i < this.x.size(); i ++) {
//			float tx = (float)(this.x.get(i) + p.offsetX);
//			float ty = -(float)(this.y.get(i) + p.offsetY);
//			p.text(i, tx, ty);
//		}
		
		for (GraphEdge g : this.d) {
			float fx0 = (float)(g.x1 + p.offsetX);
			float fx1 = (float)(g.x2 + p.offsetX);
			float fy0 = -(float)(g.y1 + p.offsetY);
			float fy1 = -(float)(g.y2 + p.offsetY);
			p.stroke(0);
			p.strokeWeight((float) (.05));
			p.line(fx0, fy0, fx1, fy1);
//			String sites = g.site1 + " " + g.site2;
//			p.text(sites, (fx0+fx1)/2,(fy0 + fy1)/2);
			
		}

	}

	@Override
	public void drawText(EventsBasedVisDebugger eventsBasedVisDebugger) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		this.x.add(event.getX());
		this.y.add(event.getY());
		
	}

	@Override
	public void update(double time) {
		buildVoronoi();
		
	}

	private void buildVoronoi() {
		if (this.x.size() < 1) {
			return;
		}
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < this.x.size(); i++) {
			double x = this.x.get(i);
			double y = this.y.get(i);
			if (x  < minX) {
				minX = x;
			}
			if (x > maxX) {
				maxX = x;
			}
			
			if (y < minY) {
				minY = y;
			}
			if (y > maxY) {
				maxY = y;
			}
		}
		
		
		int xres = (int) (maxX-minX)/2;
		int yres = (int) (maxY-minY)/2;
		int s = (xres+1)*(yres+1);
		double [] x = new double[this.x.size()+s];
		double [] y = new double[this.y.size()+s];
		int i = 0;
		for (; i < this.x.size(); i++) {
			x[i] = this.x.get(i);
			y[i] = this.y.get(i);
		}
		
		int loop =0;
		for (double xx = minX; xx < maxX; xx += 2) {
			double offset = 0;
			if (loop % 2 == 0) {
				offset = 1;
			}
			loop++;
			for (double yy = minY; yy < maxY; yy += 2){
				x[i]=xx+offset;
				y[i++]=yy+offset;
			}
			
		}
		
		this.x.clear();
		this.y.clear();
		maxX += 1;
		maxY += 1;
		minX -= 1;
		minY -= 1;
		
		Voronoi v = new Voronoi(.00001);
		this.d = v.generateVoronoi(x, y, minX, maxX, minY, maxY);
	}

}
