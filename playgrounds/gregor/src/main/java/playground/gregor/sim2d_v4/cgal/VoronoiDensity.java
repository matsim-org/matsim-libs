/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiDensity.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

// area of a simple polygon is 1/2 * sum_{i=0}^{n-1} (x_i*y_{i+1}-x_{i+1}*y_i)
public class VoronoiDensity {

	private final double minSiteDist;
	private final double minX;
	private final double minY;
	private final double maxX;
	private final double maxY;

	public VoronoiDensity(double minSiteDist,double minX, double minY, double maxX, double maxY) {
		this.minSiteDist = minSiteDist;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public List<VoronoiCell> computeVoronoiDensityFast(double [] x, double [] y) {
		List<VoronoiCell> cells = new ArrayList<VoronoiCell>(x.length);
		for (int i = 0; i < x.length; i++) {
			cells.add(new VoronoiCell(x[i],y[i]));
		}
		Voronoi vd = new Voronoi(this.minSiteDist);
		List<GraphEdge> edges = vd.generateVoronoi(x, y, this.minX-4, this.maxX+4, this.minY, this.maxY);
		for (GraphEdge e : edges) {
			VoronoiCell c1 = cells.get(e.site1);
			VoronoiCell c2 = cells.get(e.site2);

			double contr = e.x1*e.y2 - e.x2*e.y1;
			double leftOf = CGAL.isLeftOfLine(e.x2, e.y2, x[e.site1], y[e.site1], e.x1, e.y1) < 0 ? -1 : 1;
			contr *= leftOf;
			c1.area += contr/2;
			c2.area -= contr/2;

			//test whether c1 and c2 are at the bounding box -- needs special treatment, to be implemented at a later time
			if (touchesBoundingBox(e)){
				c1.area = Double.POSITIVE_INFINITY;
				c2.area = Double.POSITIVE_INFINITY;
			}
		}

		return cells;
	}

	public List<VoronoiCell> computeVoronoiDensity(double [] x, double [] y) {
		List<VoronoiCell> cells = new ArrayList<VoronoiCell>(x.length);
		for (int i = 0; i < x.length; i++) {
			cells.add(new VoronoiCell(x[i],y[i]));
		}
		Voronoi vd = new Voronoi(this.minSiteDist);
		List<GraphEdge> edges = vd.generateVoronoi(x, y, this.minX, this.maxX, this.minY, this.maxY);
		for (GraphEdge e : edges) {
			if (e.x1 == e.x2 || e.y1 == e.y2) {
				continue;
			}
			
			
			VoronoiCell c1 = cells.get(e.site1);
			VoronoiCell c2 = cells.get(e.site2);

			double contr = e.x1*e.y2 - e.x2*e.y1;
			double leftOf = CGAL.isLeftOfLine(e.x2, e.y2, x[e.site1], y[e.site1], e.x1, e.y1) < 0 ? -1 : 1;
			contr *= leftOf;
			c1.area += contr/2;
			c2.area -= contr/2;
			if (leftOf == -1)  {
				GraphEdge e1 = new GraphEdge();
				e1.x1 = e.x2;
				e1.x2 = e.x1;
				e1.y1 = e.y2;
				e1.y2 = e.y1;
				c1.edges.add(e1);
				c2.edges.add(e);
			}else {
				GraphEdge e2 = new GraphEdge();
				e2.x1 = e.x2;
				e2.x2 = e.x1;
				e2.y1 = e.y2;
				e2.y2 = e.y1;
				c1.edges.add(e);
				c2.edges.add(e2);
			}

			//test whether c1 and c2 are at the bounding box -- needs special treatment, to be implemented at a later time
			if (intersectsBoundingBoxX(e)){
				c1.area = Double.POSITIVE_INFINITY;
				c2.area = Double.POSITIVE_INFINITY;
			}

			handleYMatch(e,c1,this.maxY,e.site1,x,y,e.x1,e.y1);
			handleYMatch(e,c1,this.maxY,e.site1,x,y,e.x2,e.y2);
			handleYMatch(e,c1,this.minY,e.site1,x,y,e.x1,e.y1);
			handleYMatch(e,c1,this.minY,e.site1,x,y,e.x2,e.y2);
			handleYMatch(e,c2,this.maxY,e.site2,x,y,e.x1,e.y1);
			handleYMatch(e,c2,this.maxY,e.site2,x,y,e.x2,e.y2);
			handleYMatch(e,c2,this.minY,e.site2,x,y,e.x1,e.y1);
			handleYMatch(e,c2,this.minY,e.site2,x,y,e.x2,e.y2);
		}
		
		for (VoronoiCell c : cells) {
			if (c.matches.size() > 0) {
				c.area = Double.POSITIVE_INFINITY;
			}
		}

		return cells;
	}

	private void handleYMatch(GraphEdge ed, VoronoiCell c, double yM, int site,
			double[] x, double[] y, double xx, double yy) {
		if (Math.abs(yy - yM) < CGAL.EPSILON) {
			Coordinate val1 = c.matches.remove(yM);
			if (val1 == null) {
				c.matches.put(yM, new Coordinate(xx,yy));
			} else {
				double cntr = xx *val1.y - val1.x * yy;
				double lft = CGAL.isLeftOfLine(val1.x, val1.y, x[site], y[site], xx, yy) < 0? -1 : 1;
				cntr *= lft;
				if (Math.abs(cntr) < CGAL.EPSILON) {
					c.area = Double.POSITIVE_INFINITY;
				} else {
					GraphEdge e = new GraphEdge();
					if (lft == -1) {
						e.x1 = xx;
						e.y1 = yy;
						e.x2 = val1.x;
						e.y2 = val1.y;
					} else {
						e.x1 = val1.x;
						e.y1 = val1.y;
						e.x2 = xx;
						e.y2 = yy;
					}
					c.edges.add(e);
					
				}
				c.area += cntr/2;
			}

		}

	}
	
	private boolean touchesBoundingBox(GraphEdge e) {
		if (e.x1 >= this.maxX){
			return true;
		}
		if (e.x1 <= this.minX){
			return true;
		}
		if (e.x2 >= this.maxX){
			return true;
		}
		if (e.x2 <= this.minX){
			return true;
		}
		if (Math.abs(e.y1-this.maxY) <= CGAL.EPSILON){
			return true;
		}
		if (Math.abs(e.y1-this.minY) <= CGAL.EPSILON){
			return true;
		}
		if (Math.abs(e.y2-this.maxY) <= CGAL.EPSILON){
			return true;
		}
		if (Math.abs(e.y2-this.minY) <= CGAL.EPSILON){
			return true;
		}		
		return false;
	}
	
	private boolean intersectsBoundingBoxX(GraphEdge e) {
		if (e.x1 >= this.maxX){
			return true;
		}
		if (e.x1 <= this.minX){
			return true;
		}
		if (e.x2 >= this.maxX){
			return true;
		}
		if (e.x2 <= this.minX){
			return true;
		}
		
		return false;
	}

	public static final class VoronoiCell {
		public List<GraphEdge> edges = new ArrayList<GraphEdge>();
		public VoronoiCell(double x, double y) {
			this.x = x;
			this.y = y;
		}
		public double area;
		public double x;
		public double y;


		private final Map<Double,Coordinate> matches = new HashMap<Double,Coordinate>();


	}

	private static final class Coordinate {
		public Coordinate(double xx, double yy) {
			this.x = xx;
			this.y = yy;
		}
		double x;
		double y;
	}

}
