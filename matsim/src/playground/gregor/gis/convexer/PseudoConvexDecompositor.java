/* *********************************************************************** *
 * project: org.matsim.*
 * ConvexDecompositor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.convexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class PseudoConvexDecompositor {
	
	private final static GeometryFactory geofac = new GeometryFactory();
	private static final double CONVEXITY_THRESHOLD = 1.5;
	private static final double AREA_THRESHOLD = 1.5;
	public Collection<Polygon> decompose(final Polygon polygon) {
		
		if (polygon.getNumInteriorRing() > 0) {
			System.err.println("Polygons with holes currently not supported!");
		}
		final ArrayList<Polygon> convex = new ArrayList<Polygon>();

		final ConcurrentLinkedQueue<Polygon> pendingPolygons = new ConcurrentLinkedQueue<Polygon>();
		pendingPolygons.add(polygon);
		Polygon current;
		while(pendingPolygons.peek() != null) {
			current  = pendingPolygons.poll();
			if (current.getExteriorRing().getNumPoints() <= 5 || isConvex(current) || current.getArea() < AREA_THRESHOLD) {
				convex.add(current);
			} else {
				current = simplify(current);
				if (isConvex(current)) {
					convex.add(current);
				} else {
					split(current,pendingPolygons);	
				}
				
			}
			if (pendingPolygons.size() > 10) {
				
//				convex.addAll(pendingPolygons);
				convex.clear();
				break;
			}
		}
		
		
		
		return convex;
	}

	private Polygon simplify(final Polygon p) {
		final ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		final LineString exterior = p.getExteriorRing();
		final Coordinate old = exterior.getCoordinateN(0);
		coords.add(old);
		for (int i = 1; i < exterior.getNumPoints()-1; i++) {
			Coordinate current = exterior.getCoordinateN(i);
			while (old.distance(current) < 0.01 && i < exterior.getNumPoints()-1) {
				current = exterior.getCoordinateN(++i);
			}
			coords.add(current);
		}
		coords.add(exterior.getCoordinateN(0));
		Coordinate [] coordsA = new Coordinate [coords.size()]; 
		coordsA = coords.toArray(coordsA);
		

		return geofac.createPolygon(geofac.createLinearRing(coordsA), null);
	}

	private void split(final Polygon polygon, final ConcurrentLinkedQueue<Polygon> pendingPolygons) {
		
		final LineString exterior = polygon.getExteriorRing();
		LineSegment ls = null;
		double length = 0;
		int pos = 0;
		for (int i = 1; i < exterior.getNumPoints()-1; i++) {
			final Coordinate c0 = exterior.getCoordinateN(i-1);
			
			Coordinate c2 = exterior.getCoordinateN(i+1);
			while (c0.distance(c2) < 0.01 && i < exterior.getNumPoints()-2) {
				c2 = exterior.getCoordinateN(++i+1);
			}
			if (c0.distance(c2) < 0.01) {
				System.err.println("Polygon not splitable!");
				return;
			}
			
			final LineString ltmp = geofac.createLineString(new Coordinate[] {c2,c0});
			try {
				if (!polygon.contains(ltmp)) {
					
					final Coordinate c1 = exterior.getCoordinateN(i);
					if (c0.distance(c2) > length) {
						length = c0.distance(c2);
						ls = new LineSegment(c1,c0);
						pos = i;
					}
				}
			} catch (final RuntimeException e) {
				e.printStackTrace();
				System.err.println("Polygon not splitable!");
				return;
			}		
		}
		if ( ls == null ) {
			System.err.println("Polygon not splitable!");
		} else {
			final int s2 = find2ndSplittingPoint(polygon, ls,pos);
			if (s2 == pos) {
				System.err.println("Polygon not splitable!");
				return;
			}
			splittPolygon(pos,s2,polygon,pendingPolygons);			
		}


	}

	private void splittPolygon(final int s1, final int s2, final Polygon polygon, final ConcurrentLinkedQueue<Polygon> pendingPolygons) {
		final LineString exterior = polygon.getExteriorRing();
		int pos = s1;
		
		final ArrayList<Coordinate> cp1 = new ArrayList<Coordinate>();
		for (int i = 0; i < exterior.getNumPoints(); i++) {
			 cp1.add(exterior.getCoordinateN(pos));
			 if (pos == s2) {
				 cp1.add(exterior.getCoordinateN(s1));
				 break;
			 }
			 if (pos == 0) pos = exterior.getNumPoints()-1;
			 else pos--;
		}

		final ArrayList<Coordinate> cp2 = new ArrayList<Coordinate>();
		for (int i = 0; i < exterior.getNumPoints(); i++) {
			 cp2.add(exterior.getCoordinateN(pos));
			 if (pos == s1) {
				 cp2.add(exterior.getCoordinateN(s2));
				 break;
			 }
			 if (pos == 0) pos = exterior.getNumPoints()-1;
			 else pos--;
		}
		Coordinate [] cc1 = new Coordinate[cp1.size()];
		cc1 = cp1.toArray(cc1);
		Coordinate [] cc2 = new Coordinate[cp2.size()];
		cc2 = cp2.toArray(cc2);
		
		try {
			pendingPolygons.add(geofac.createPolygon(geofac.createLinearRing(cc1), null));
			pendingPolygons.add(geofac.createPolygon(geofac.createLinearRing(cc2), null));
		} catch (final RuntimeException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();s
		}
		
		
	}

	private int find2ndSplittingPoint(final Polygon polygon, final LineSegment lineSegment, final int current) {
		final LineString exterior = polygon.getExteriorRing();
		 final double angle = lineSegment.angle();
		 int pos = current;
		 double bestAngleDiff = Double.POSITIVE_INFINITY;
		 int bestSplittingLocation = current;
		 final Coordinate c0 = lineSegment.p0;
		 for (int  i =  0; i < exterior.getNumPoints()-1; i++ ) {
			 if (pos == 0) pos = exterior.getNumPoints()-1;
			 else pos--;
			 
			 final Coordinate c1 = exterior.getCoordinateN(pos);
			 if (!polygon.contains(geofac.createLineString(new Coordinate[] {c0,c1}))) {
				 continue;
			 }
			 
			 final LineSegment temp = new LineSegment(c0,c1);
			 
			 
			 final double tempDiff = Math.abs(Math.PI/2-Math.abs(temp.angle()-angle));
			 if (tempDiff < bestAngleDiff) {
				 bestAngleDiff = tempDiff;
				 bestSplittingLocation = pos;
//				 if (tempDiff <=Math.PI/8) {
//					 break;
//				 }
			 }
			 
		 }
			
		 return bestSplittingLocation;
		
		
	}

	private boolean isConvex(final Polygon polygon) {
		final Polygon convex = (Polygon) polygon.convexHull();
		final double a1 = polygon.getArea();
		final double a2 = convex.getArea();
		
		if (a2/a1 <= CONVEXITY_THRESHOLD) {
			return true;
		}
		
		return false;
	}

//	private static boolean isConvex(final Polygon p) {
//		final LineString exterior = p.getExteriorRing();
//		
//		
//		
//		for (int i = 1; i < exterior.getNumPoints()-1; i++) {
//			final Coordinate c0 = exterior.getCoordinateN(i-1);
//			final Coordinate c2 = exterior.getCoordinateN(i+1);
//			if (!p.contains(geofac.createLineString(new Coordinate[] {c0,c2}))) {
//				return false;
//			}
//			
//		}
//
//		final Coordinate c0 = exterior.getCoordinateN(exterior.getNumPoints()-2);
//		final Coordinate c2 = exterior.getCoordinateN(1);
//		if (!p.contains(geofac.createLineString(new Coordinate[] {c0,c2}))) {
//			return false;
//		}
//		
//		return true;
//	}
	

}
