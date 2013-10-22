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

import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Envelope;

public class VoronoiDiagramCells <T extends VoronoiCenter> {

	private final Envelope envelope;

	public static final double SPATIAL_BUFFER = 2;

	public VoronoiDiagramCells(Envelope e) {

		this.envelope = e;
		
	}

	public void update(List<T> points) {

		if (points.size() < 3) {
			return;
		}

		double [] x = new double[points.size()];
		double [] y = new double[points.size()];
		VoronoiCell [] ca = new VoronoiCell[points.size()];

		//for better performance lets think about VoronoiCell recycling ...
		int idx = 0;
		for (T point : points) {
			double xx = point.getXLocation();
			double yy = point.getYLocation();
			VoronoiCell vc = new VoronoiCell(point,idx);
			point.setVoronoiCell(vc);
			ca[idx] = vc;
			x[idx] = xx; 
			y[idx++] = yy;
		}



		Voronoi vd = new Voronoi(CGAL.EPSILON);
		List<GraphEdge> edges = vd.generateVoronoi(x, y, this.envelope.getMinX()-SPATIAL_BUFFER, this.envelope.getMaxX()+SPATIAL_BUFFER,this.envelope.getMinY()-SPATIAL_BUFFER,this.envelope.getMaxY()+SPATIAL_BUFFER);

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

//			double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
//			double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[ed.site1], y[ed.site1], ed.x1, ed.y1) < 0 ? -1 : 1;
//			contr *= leftOf;
//			vc1.area += contr/2;
//			vc2.area -= contr/2;
			
			
		}
//		debug(edges,points.get(0));
		//		Map<VoronoiCell,VoronoiCellTmpData> tmpData = new HashMap<VoronoiCell, VoronoiCellTmpData>();
		//		List<GraphEdge> boundaryEdges = new ArrayList<GraphEdge>();
		//		
		//		Iterator<GraphEdge> it = edges.iterator();
		//		while (it.hasNext()) {
		//			GraphEdge ed = it.next();
		////			System.out.println(ed.site1);
		//			
		////			System.out.println(ed.x1 + "  " + ed.y1 + "  " + ed.x2 + "  " + ed.y2);
		//			if (ed.x1 == ed.x2 && ed.y1 == ed.y2) {
		//				it.remove();
		//				continue;
		//			}
		//			
		//
		//			VoronoiCell c1 = ca[ed.site1];
		//			VoronoiCell c2 = ca[ed.site2];
		//			intersection(c1,c2,ed,tmpData,boundaryEdges);
		//
		//			double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
		//			double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[ed.site1], y[ed.site1], ed.x1, ed.y1) < 0 ? -1 : 1;
		//			contr *= leftOf;
		//			c1.area += contr/2;
		//			c2.area -= contr/2;

	}

	//		ArrayList<GraphEdge> tmpBoundaryEdges = new ArrayList<GraphEdge>();
	//		for (Entry<VoronoiCell, VoronoiCellTmpData> e : tmpData.entrySet()) {
	//			VoronoiCell c = e.getKey();
	//			for (Entry<LineSegment, GraphEdge> d : e.getValue().tmpEdges.entrySet()){
	//				tmpBoundaryEdgeConstruction(c,d.getKey(),d.getValue(),tmpBoundaryEdges);
	//			}
	//		}
	//		tmpData.clear();
	//		
	//		
	//		for (GraphEdge ed : tmpBoundaryEdges) {
	//
	//			VoronoiCell c1 = ca[ed.site1];
	//			VoronoiCell c2 = ca[idx];//dummy
	//			intersection(c1,c2,ed,tmpData,boundaryEdges);
	//
	//			double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
	//			double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[ed.site1], y[ed.site1], ed.x1, ed.y1) < 0 ? -1 : 1;
	//			contr *= leftOf;
	//			c1.area += contr/2;
	//		}
	//		
	//		
	//		for (GraphEdge ed : boundaryEdges) {
	//			VoronoiCell c1 = ca[ed.site1];
	//			double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
	//			double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[ed.site1], y[ed.site1], ed.x1, ed.y1) < 0 ? -1 : 1;
	//			contr *= leftOf;
	//			c1.area += contr/2;
	//		}
	//		
	//		debug(tmpBoundaryEdges, points.get(0));
	//		debug(boundaryEdges, points.get(0));
	//		debug(edges, points.get(0));
	//	}


	private void debug(List<GraphEdge> b, T t) {
		Sim2DAgent agent = (Sim2DAgent)t;
		PhysicalSim2DSection psec = agent.getPSec();
		EventsManager em = psec.getPhysicalEnvironment().getEventsManager();

		for (GraphEdge e : b) {
			LineSegment ls = new LineSegment();
			ls.x0 = e.x1;
			ls.x1 = e.x2;
			ls.y0 = e.y1;
			ls.y1 = e.y2;
			//			if (e.y1 > 2 && e.y2 > 2) {
			//				System.out.println(e.site1 + "  " + e.site2);
			//			}
			em.processEvent(new LineEvent(0, ls, false, 0, 255, 255, 255, 0));//, .8, .2));
		}

	}
	//	private void tmpBoundaryEdgeConstruction(VoronoiCell c, LineSegment ls,
	//			GraphEdge ge, ArrayList<GraphEdge> tmpBoundaryEdges) {
	//		
	//		double leftOf1 = CGAL.isLeftOfLine(c.getPointX(), c.getPointY(), ge.x2,ge.y2,ge.x1,ge.y1);
	//		double leftOf2 = CGAL.isLeftOfLine(ls.x0, ls.y0, ge.x2,ge.y2,ge.x1,ge.y1);
	//		if (leftOf1*leftOf2 < 0) {
	//			ge.x2 = ls.x1 + ls.dx * (SPATIAL_BUFFER+CGAL.EPSILON);
	//			ge.y2 = ls.y1 + ls.dy * (SPATIAL_BUFFER+CGAL.EPSILON);
	//		} else {
	//			ge.x2 = ls.x0 - ls.dx * (SPATIAL_BUFFER+CGAL.EPSILON);
	//			ge.y2 = ls.y0 - ls.dy * (SPATIAL_BUFFER+CGAL.EPSILON);
	//		}
	//		tmpBoundaryEdges.add(ge);
	//	}
	//
	//	private void intersection(VoronoiCell c1, VoronoiCell c2, GraphEdge ed,
	//			Map<VoronoiCell, VoronoiCellTmpData> tmpData,
	//			List<GraphEdge> boundaryEdges) {
	//
	//		double dxu = ed.x2 - ed.x1;
	//		double dyu = ed.y2 - ed.y1;
	//		for (LineSegment o : this.bounds) {
	//			double lft0 = CGAL.isLeftOfLine(ed.x1, ed.y1, o.x0, o.y0, o.x1, o.y1);
	//			double lft1 = CGAL.isLeftOfLine(ed.x2, ed.y2, o.x0, o.y0, o.x1, o.y1);
	//			
	//			if (lft0 > 0 && lft1 > 0) {
	//				ed.x1 = 0; ed.x2 = 0; ed.y1 =0; ed.y2 = 0; //invalid edge (outside boundary)
	//				return;
	//			}
	//			
	//			if (lft0 * lft1 < 0) {
	//				double dxv = o.x1 - o.x0;
	//				double dyv = o.y1 - o.y0;
	//				double dxw = ed.x1 - o.x0;
	//				double dyw = ed.y1 - o.y0;
	//				double d = CGAL.perpDot(dxu, dyu, dxv, dyv);
	//				double c = CGAL.perpDot(dxv, dyv, dxw, dyw)/d;
	//				if (c < 0 || c > 1) {
	//					continue;
	//				}
	//				double cc = CGAL.perpDot(dxu, dyu, dxw, dyw)/d;
	//				if (cc < 0 || cc > 1) {
	//					continue;
	//				}
	//
	//				double xx = ed.x1 + dxu * c;
	//				double yy = ed.y1 + dyu * c;
	//
	//				
	//				double xxPrime;
	//				double yyPrime;
	//				if (lft0 >= 0) {
	//					ed.x1 = xx;
	//					ed.y1 = yy;
	//					xxPrime = ed.x2;
	//					yyPrime = ed.y2;
	//				} else {
	//					ed.x2 = xx;
	//					ed.y2 = yy;
	//					xxPrime = ed.x1;
	//					yyPrime = ed.y1;
	//				}
	//				handleBoundaryIntersection(c1,xx,yy, boundaryEdges,tmpData,o,xxPrime,yyPrime);
	//				if (!Double.isNaN(c2.getPointX())) {
	//					handleBoundaryIntersection(c2,xx,yy, boundaryEdges,tmpData,o,xxPrime,yyPrime);
	//				}
	//			}
	//		}
	//
	//	}
	//
	//	private void handleBoundaryIntersection(VoronoiCell c, double xx,
	//			double yy, List<GraphEdge> boundaryEdges,
	//			Map<VoronoiCell, VoronoiCellTmpData> tmpData, LineSegment o, double xxPrime, double yyPrime) {
	//		VoronoiCellTmpData tmp = tmpData.get(c);
	//		if (tmp == null) {
	//			tmp = new VoronoiCellTmpData();
	//			tmpData.put(c, tmp);
	//		}
	//		GraphEdge edge = tmp.tmpEdges.remove(o);
	//		if (edge != null) {
	//			edge.x2 = xx;
	//			edge.y2 = yy;
	//			boundaryEdges.add(edge);
	//			if (tmp.tmpEdges.size() == 0) {
	//				tmpData.remove(c);
	//			}
	//		} else {
	//			edge = new GraphEdge();
	//			edge.x1 = xx;
	//			edge.y1 = yy;
	//			
	//			//needed later on to figure out on which side of the edge the corresponding point is located
	//			//... hard to explain, read the code and you will see :-) [GL Oct '13]
	//			edge.x2 = xxPrime;
	//			edge.y2 = yyPrime;
	//			edge.site1 = c.getIdx();
	//			edge.site2 = -1;
	//			tmp.tmpEdges.put(o, edge);
	//		}
	//	}
	//
	//	private final class VoronoiCellTmpData {
	//		Map<LineSegment,GraphEdge> tmpEdges = new HashMap<LineSegment, GraphEdge>();
	//	}

}
