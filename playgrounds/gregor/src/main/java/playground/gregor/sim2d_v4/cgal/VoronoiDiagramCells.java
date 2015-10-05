/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiDiagram.java
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

package playground.gregor.sim2d_v4.cgal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v4.events.debug.LineEvent;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class VoronoiDiagramCells <T extends VoronoiCenter> {
	
	private static final Logger log = Logger.getLogger(VoronoiDiagramCells.class);

	private final Envelope envelope;

	private final QuadTree<Coordinate> qt;

	private final EventsManager em;

	public static final double SPATIAL_BUFFER = 10;

	public VoronoiDiagramCells(Envelope e, EventsManager em) {
		
		this.em = em;
		LineSegment ls0 = new LineSegment();
		ls0.x0 = e.getMaxX();
		ls0.y0 = e.getMaxY();
		ls0.x1 = e.getMinX();
		ls0.y1 = e.getMaxY();
		em.processEvent(new LineEvent(0, ls0, true, 0, 0, 0, 255, 0,.2,.5));

		LineSegment ls1 = new LineSegment();
		ls1.x0 = e.getMinX();
		ls1.y0 = e.getMaxY();
		ls1.x1 = e.getMinX();
		ls1.y1 = e.getMinY();
		em.processEvent(new LineEvent(0, ls1, true, 0, 0, 0, 255, 0,.2,.5));
		
		LineSegment ls2 = new LineSegment();
		ls2.x0 = e.getMinX();
		ls2.y0 = e.getMinY();
		ls2.x1 = e.getMaxX();
		ls2.y1 = e.getMinY();
		em.processEvent(new LineEvent(0, ls2, true, 0, 0, 0, 255, 0,.2,.5));
		
		LineSegment ls3 = new LineSegment();
		ls3.x0 = e.getMaxX();
		ls3.y0 = e.getMinY();
		ls3.x1 = e.getMaxX();
		ls3.y1 = e.getMaxY();
		em.processEvent(new LineEvent(0, ls3, true, 0, 0, 0, 255, 0,.2,.5));
		
		this.envelope = e;
		this.qt = new QuadTree<Coordinate>(this.envelope.getMinX()-SPATIAL_BUFFER, this.envelope.getMinY()-SPATIAL_BUFFER, this.envelope.getMaxX()+SPATIAL_BUFFER, this.envelope.getMaxY()+SPATIAL_BUFFER);
		
	}

	public List<VoronoiCell> update(List<T> points) {
		List<VoronoiCell> ret = new ArrayList<VoronoiCell>();
		
		
		if (points.size() < 3) {
			return ret;
		}

		double [] x = new double[points.size()];
		double [] y = new double[points.size()];
		VoronoiCell [] ca = new VoronoiCell[points.size()];

		int idx = 0;
		for (T point : points) {
			double xx = point.getX();
			double yy = point.getY();
			VoronoiCell vc = new VoronoiCell(point,idx);
			point.setVoronoiCell(vc);
			ca[idx] = vc;
			x[idx] = xx; 
			y[idx++] = yy;
		}



		Voronoi vd = new Voronoi(CGAL.EPSILON);
		List<GraphEdge> edges = vd.generateVoronoi(x, y, this.envelope.getMinX()-SPATIAL_BUFFER, this.envelope.getMaxX()+SPATIAL_BUFFER,this.envelope.getMinY()-SPATIAL_BUFFER,this.envelope.getMaxY()+SPATIAL_BUFFER);

		//computation
		for (GraphEdge ed : edges) {
			if (ed.x1 == ed.x2 && ed.y1 == ed.y2) {
				continue;
			}
			VoronoiCell vc1 = ca[ed.site1];
			VoronoiCell vc2 = ca[ed.site2];
			vc1.addNeighbor(vc2.getVoronoiCenter());
			vc2.addNeighbor(vc1.getVoronoiCenter());
			vc1.addGraphEdge(ed);
			vc2.addGraphEdge(ed);

			double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
			double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[ed.site1], y[ed.site1], ed.x1, ed.y1) < 0 ? -1 : 1;
			contr *= leftOf;
			vc1.incrementAreaBy(contr/2);
			vc2.incrementAreaBy(-contr/2);
		}
		
		//validation
		for (VoronoiCell vc : ca) {
			this.qt.clear();
			
			for (GraphEdge ed : vc.getGraphEdges()){
				Collection<Coordinate> tmpColl = this.qt.getDisk(ed.x1, ed.y1, 0.0001);
				if (tmpColl.size() > 1) {
					log.warn("Somthing is broken here, ignoring for now!");
					return new ArrayList<VoronoiCell>();
				} else if (tmpColl.size() == 1) {
					Coordinate rm = tmpColl.iterator().next();
					this.qt.remove(rm.x, rm.y, rm);
				} else {
					this.qt.put(ed.x1, ed.y1, new Coordinate(ed.x1,ed.y1));
				}
				tmpColl = this.qt.getDisk(ed.x2, ed.y2, 0.0001);
				if (tmpColl.size() > 1) {
					log.warn("Somthing is broken here, ignoring for now!");
					return new ArrayList<VoronoiCell>();
				} else if (tmpColl.size() == 1) {
					Coordinate rm = tmpColl.iterator().next();
					this.qt.remove(rm.x, rm.y, rm);
				} else {
					this.qt.put(ed.x2, ed.y2, new Coordinate(ed.x2,ed.y2));
				}
			}
			if (this.qt.size() == 0 && vc.getGraphEdges().size() > 0 && this.envelope.contains(vc.getVoronoiCenter().getX(),vc.getVoronoiCenter().getY())) {
				vc.setIsClosed(true);
				ret.add(vc);
				debug(vc.getGraphEdges(),((T) vc.getVoronoiCenter()));
			}
		}
	
		return ret;
	}

	private void debug(List<GraphEdge> b, T t) {

		for (GraphEdge e : b) {
			LineSegment ls = new LineSegment();
			ls.x0 = e.x1;
			ls.x1 = e.x2;
			ls.y0 = e.y1;
			ls.y1 = e.y2;
			this.em.processEvent(new LineEvent(0, ls, false, 0, 0, 0, 255, 0));//, .8, .2));
		}

	}

}
