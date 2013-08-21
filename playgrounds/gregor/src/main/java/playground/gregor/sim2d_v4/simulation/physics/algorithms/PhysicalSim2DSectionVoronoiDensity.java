/* *********************************************************************** *
 * project: org.matsim.*
 * SectionVoronoiDensity.java
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

package playground.gregor.sim2d_v4.simulation.physics.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Envelope;

public class PhysicalSim2DSectionVoronoiDensity {

	private final PhysicalSim2DSection pSec;
	private final Envelope bounds;

	public PhysicalSim2DSectionVoronoiDensity(PhysicalSim2DSection pSec){
		this.pSec = pSec;
		this.bounds = new Envelope(pSec.getObstacles().get(0).x0,pSec.getObstacles().get(0).x1,pSec.getObstacles().get(0).y0,pSec.getObstacles().get(0).y1);
		for (Segment o : pSec.getOpenings()) {
			this.bounds.expandToInclude(o.x0-2*o.dy, o.y0+2*o.dx);
			this.bounds.expandToInclude(o.x1-2*o.dy, o.y1+2*o.dx);
		}

		for (Segment o : pSec.getObstacles()) {
			this.bounds.expandToInclude(o.x0, o.y0);
			this.bounds.expandToInclude(o.x1, o.y1);
		}

	}

	public void buildDensityMap() {
		List<Sim2DAgent> agents = new ArrayList<Sim2DAgent>(this.pSec.getAgents());
		if (agents.size() == 0) {
			return;
		}
		for (Segment o : this.pSec.getOpenings()) {
			PhysicalSim2DSection n = this.pSec.getNeighbor(o);
			for (Sim2DAgent a : n.getAgents()) {
				if (this.bounds.contains(a.getPos()[0], a.getPos()[1])) {
					agents.add(a);
				}
			}
		}

		double [] x = new double[agents.size()];
		double [] y = new double[agents.size()];
		int idx = 0;
		for (Sim2DAgent a : agents) {
			x[idx] = a.getPos()[0];
			y[idx] = a.getPos()[1];
			idx++;
		}
		Voronoi vd = new Voronoi(CGAL.EPSILON);
		List<GraphEdge> edges = vd.generateVoronoi(x, y,this.bounds.getMinX(),this.bounds.getMaxX(),this.bounds.getMinY(),this.bounds.getMaxY());

		debug();
		debug(edges);
	}

	private void debug(List<GraphEdge> edges) {
		EventsManager em = this.pSec.getPhysicalEnvironment().getEventsManager();
		for (GraphEdge e : edges) {
			Segment seg = new Segment();
			seg.x0 = e.x1;
			seg.y0 = e.y1;
			seg.x1 = e.x2;
			seg.y1 = e.y2;
			em.processEvent(new LineEvent(0, seg, false,0,255,255,128,0,.2,.2));
		}
		
	}

	private void debug() {
		EventsManager em = this.pSec.getPhysicalEnvironment().getEventsManager();
		Segment seg0 = new Segment();
		seg0.x0 = this.bounds.getMaxX();
		seg0.y0 = this.bounds.getMaxY();
		seg0.x1 = this.bounds.getMinX();
		seg0.y1 = this.bounds.getMaxY();
		Segment seg1 = new Segment();
		seg1.x0 = this.bounds.getMinX();
		seg1.y0 = this.bounds.getMaxY();
		seg1.x1 = this.bounds.getMinX();
		seg1.y1 = this.bounds.getMinY();
		Segment seg2 = new Segment();
		seg2.x0 = this.bounds.getMinX();
		seg2.y0 = this.bounds.getMinY();
		seg2.x1 = this.bounds.getMaxX();
		seg2.y1 = this.bounds.getMinY();
		Segment seg3 = new Segment();
		seg3.x0 = this.bounds.getMaxX();
		seg3.y0 = this.bounds.getMinY();
		seg3.x1 = this.bounds.getMaxX();
		seg3.y1 = this.bounds.getMaxY();		

		em.processEvent(new LineEvent(0, seg0, false,0,0,255,255,0,.5,.2));
		em.processEvent(new LineEvent(0, seg1, false,0,0,255,255,0,.5,.2));
		em.processEvent(new LineEvent(0, seg2, false,0,0,255,255,0,.5,.2));
		em.processEvent(new LineEvent(0, seg3, false,0,0,255,255,0,.5,.2));

	}

	//	private void init() {
	//		
	//		
	//	}

}
