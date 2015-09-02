/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiAreaTest.java
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

package playground.gregor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v4.cgal.CGAL;
import processing.core.PApplet;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

public class VoronoiAreaTest extends PApplet{


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<GraphEdge> edges;
	private int redraws;
	private int cnt=4000;
	private HashMap<Integer, Cell> cells;

	@Override
	public void setup() {
		//				size(1024,768);
		size(1920,1080);
		//		background(0);
		buildVD();
		frameRate(90);

	}

	private void buildVD() {
		double  width = 60;
		int idx =0;
		boolean even = false;
		for (int yc = 0; yc < 1080; yc += width*Math.sqrt(3)/4) {
			int xc = 0;
			if (even) {
				xc += width*1.5/2;
			}
			for (; xc < 1920; xc+=width*1.5) {
				idx++;
			}
			even = !even;
		}
		
		double x[] = new double[idx];
		double y[] = new double[idx];
		even = false;
		idx =0;
		
		for (int yc = 0; yc < 1080; yc += width*Math.sqrt(3)/4) {
			int xc = 0;
			if (even) {
				xc += width*1.5/2;
			}
			for (; xc < 1920; xc+=width*1.5) {
				x[idx] = xc;
				y[idx++] = yc;
			}
			even = !even;
		}
		System.out.println(idx);

		Voronoi v = new Voronoi(0.0001);
		this.edges = v.generateVoronoi(x, y, 0, 1920, 0, 1080);
		this.redraws = 25;
		this.cnt++;

		computeCells(x,y);
	}

	private void computeCells(double[] x, double[] y) {
		this.cells = new HashMap<Integer,Cell>();



		// iterate over edges
		// create cell edge in cw order (left of procedure)
		// collect cell edges (&check for duplicates)
		//voronoi cells are non-intersecting simple polygons
		//the area A(p) of a non-intersecting simple polygon is defined as:
		// A(p) = 1/2 * sum_{i=0}^{N-1} (x_i*y_{i+1}-x_{i+1}*y_i)



		for (GraphEdge e : this.edges) {
			
			double length = Math.sqrt((e.x1-e.x2)*(e.x1-e.x2)+(e.y1-e.y2)*(e.y1-e.y2));
//			System.out.println(length);
			

			Cell c1 = this.cells.get(e.site1);
			if (c1 == null) {
				c1 = new Cell();
				c1.x = x[e.site1];
				c1.y = y[e.site1];
				this.cells.put(e.site1, c1);
			}

			
			//area computation (order of segments does not matter, i.e. we don't have to know the polygonal structure of the cells) - complexity is O(n)
			double contr = e.x1*e.y2 - e.x2*e.y1;
			double leftOf = CGAL.isLeftOfLine(e.x2, e.y2, x[e.site1], y[e.site1], e.x1, e.y1) < 0 ? -1 : 1;
			contr *= leftOf;
			c1.area += contr;
			c1.edges.add(e);


			Cell c2 = this.cells.get(e.site2);
			if (c2 == null) {
				c2 = new Cell();
				c2.x = x[e.site2];
				c2.y = y[e.site2];
				this.cells.put(e.site2, c2);
			}
			c2.area -= contr;
			c2.edges.add(e);
		}
		
		
		
		
		for (Cell c : this.cells.values()) {
			
			
			c.area /= 2;
			
			
			Edge e = new Edge();
			Iterator<GraphEdge> it = c.edges.iterator();
			GraphEdge ee = it.next(); it.remove();
			double leftOf = CGAL.isLeftOfLine(ee.x2, ee.y2, c.x, c.y, ee.x1, ee.y1);
			if (leftOf > 0) {
				e.x1 = ee.x1;
				e.x2 = ee.x2;
				e.y1 = ee.y1;
				e.y2 = ee.y2;
			} else {
				e.x1 = ee.x2;
				e.x2 = ee.x1;
				e.y1 = ee.y2;
				e.y2 = ee.y1;
			}
			c.es.offer(e);
			boolean loop = true;
			while (c.edges.size() > 0 && loop) {
				loop = false;
				Iterator<GraphEdge> it2 = c.edges.iterator();
				while (it2.hasNext()) {
					GraphEdge ee2 = it2.next(); 
					Edge e2 = new Edge();
					double leftOf2 = CGAL.isLeftOfLine(ee2.x2, ee2.y2, c.x, c.y, ee2.x1, ee2.y1);
					if (leftOf2 > 0) {
						e2.x1 = ee2.x1;
						e2.x2 = ee2.x2;
						e2.y1 = ee2.y1;
						e2.y2 = ee2.y2;
					} else {
						e2.x1 = ee2.x2;
						e2.x2 = ee2.x1;
						e2.y1 = ee2.y2;
						e2.y2 = ee2.y1;
					}
					if (Math.abs(e2.x1-c.es.getLast().x2) < 0.1 && Math.abs(e2.y1 - c.es.getLast().y2) < 0.1) {
						c.es.offer(e2);
						it2.remove();
						
						loop = true;
					}
				}
			}
			//			c.area /= 10000;
//			System.out.println(c.edges.size());
			c.area2 = 0.;
			for (Edge es : c.es ) {
				c.area2 += (es.x1*es.y2 - es.x2*es.y1);
			}
			c.area2 /= 2.;


			////			System.out.println(c.area);
			//			for (GraphEdge e : c.edges) {
			//				System.out.println(e.site1 + " " + e.site2 +  " " + e.x1 + " " + e.y1 + " " + e.x2 + " " + e.y2);
			//			}
			//			System.out.println("=======================");
			//			avg += Math.abs(c.area);
		}
	}

	@Override
	public void draw() {
		textSize(14);
		clear();
		strokeCap(SQUARE);

		stroke(255);
		for (Cell c : this.cells.values()) {
			//			
			//			
						fill(c.hashCode()%255,0,255-(c.hashCode()%255),255);
						stroke(0,255,MatsimRandom.getRandom().nextInt(256),255);
						beginShape();
						for (Edge e : c.es) {
							vertex((float)e.x1, (float) e.y1);
							vertex((float)e.x2, (float) e.y2);
						}
						
			//			vertex((float)c.edges.getFirst().x1, (float) c.edges.getFirst().y1);
						endShape();
			//			
						stroke(255);
						fill(255);
//						text(c.area+"", (float)c.x, (float)c.y);
//						text(c.area2+"", (float)c.x, (float)c.y+21);
						ellipse((float)c.x, (float)c.y,5,5);
		}

		for (GraphEdge e : this.edges) {
			float fx1 = (float) e.x1;
			float fy1 = (float) e.y1;
			float fx2 = (float) e.x2;
			float fy2 = (float) e.y2;

			stroke(255,32);
			strokeWeight(16);
			line(fx1,fy1,fx2,fy2);

			stroke(255,64);
			strokeWeight(8);
			line(fx1,fy1,fx2,fy2);

			stroke(255,128);
			strokeWeight(4);
			line(fx1,fy1,fx2,fy2);

			strokeWeight(1);
			stroke(0);
			line(fx1,fy1,fx2,fy2);
		}



		if (this.redraws-- <= 0) {
			buildVD();
		}

	}

	private static final class Cell {

		public double area2;
		public List<GraphEdge> edges= new ArrayList<GraphEdge>() ;
		public LinkedList<Edge> es= new LinkedList<Edge>() ;
		double area;
		double x;
		double y;
		int site;
	}
	private static final class Edge {
		double x1;
		double y1;
		double x2;
		double y2;
	}
}
