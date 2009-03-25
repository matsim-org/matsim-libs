/* *********************************************************************** *
 * project: org.matsim.*
 * RealConvexer.java
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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Concaver {
	
	private final static double MAX_DIST_CRIT = 0.8;
	
	private final GeometryFactory geofac;
	private QuadTree<Coordinate> mpQuad;

	public Concaver() {
		this.geofac = new GeometryFactory();
	}
	
	public Polygon getConcaveHull(final MultiPoint mp)  {
		
		Geometry hull = mp.convexHull();
		
		if (! (hull instanceof Polygon) ) {
//			throw new RuntimeException("Convex hull is not a polygon!");
			return null;
		}
		initQuadTree((Polygon)hull, mp);		
		
		
		LineString ls = ((Polygon)hull).getExteriorRing();
		LinkedList<Coordinate> l = new LinkedList<Coordinate>();
		HashSet<Coordinate> hullSet = new LinkedHashSet<Coordinate>();
		for (int i = 0; i < ls.getNumPoints(); i++) {
			l.add(ls.getCoordinateN(i));
			hullSet.add(ls.getCoordinateN(i));
		}
		
		boolean finished = false;
		while (!finished) {
			int splittPos = findSplittPos(l);
		
			if (splittPos == -1){
				finished = true;
			} else {
				LinkedList<Coordinate> tmp = splittAtPos(splittPos,l,hullSet);
				if (tmp == null) {
					finished = true;
				} else {
					l = tmp;
				}
			}
		}
		
		
		return getPolygonFromCoords(l);
	}

	private LinkedList<Coordinate> splittAtPos(final int splittPos, final LinkedList<Coordinate> l, final HashSet<Coordinate> hullSet) {
		Coordinate splittCoord = l.get(splittPos);
		Coordinate succCoord = l.get(splittPos+1);
		double distance = splittCoord.distance(succCoord);
		
		
		Collection<Coordinate>  coords = this.mpQuad.get(splittCoord.x, splittCoord.y, distance);
		if (coords.size() == 0) {
			return null;
		}
		
		SortedMap<Double,Coordinate> sortedCoords = new TreeMap<Double, Coordinate>();
		for (Coordinate c : coords) {
			if (c != splittCoord && c != succCoord){
				sortedCoords.put(c.distance(splittCoord), c);
			}
		}
		
		for (Map.Entry<Double, Coordinate>  e : sortedCoords.entrySet()) {
			Coordinate c = e.getValue();
			if (succCoord.distance(c) > distance || hullSet.contains(c)) { 
				continue;
			}
			LinkedList<Coordinate> hull = new LinkedList<Coordinate>(l);
			hull.add(splittPos + 1 , c);
			Polygon p = getPolygonFromCoords(hull);
			if (isValidHull(p,coords)) {
				hullSet.add(c);
				return hull;
			}
		}
		
		return null;
	}

	private boolean isValidHull(final Polygon p, final Collection<Coordinate> coords) {
		for (Coordinate coord : coords) {
			Point tmp = this.geofac.createPoint(coord);
			if ( !(p.contains(tmp)  || p.touches(tmp)) ) {
				return false;
			}
		}
		return true;
	}

	private int findSplittPos(final LinkedList<Coordinate> l) {
		double maxDist = 0;
		int maxDistPos = -1;
		double distSum = 0;
		for (int i = 0; i < l.size()-1; i++) {
			double dist = l.get(i).distance(l.get(i+1));
			distSum += dist;
			if (dist > maxDist) {
				maxDist = dist;
				maxDistPos = i;
			}
		}
		
		
		if (maxDist <= MAX_DIST_CRIT * distSum / l.size()) {
			return -1;
		}
		
		return maxDistPos;
	}

	
	private Polygon getPolygonFromCoords(final LinkedList<Coordinate> l) {
		
		
		Coordinate [] coords = new Coordinate[l.size()];
		for (int i = 0; i < l.size(); i++) {
			coords[i] = l.get(i);
		}
		
		LinearRing lr = this.geofac.createLinearRing(coords);
		
		return this.geofac.createPolygon(lr, null);
	}
	
	
	private void initQuadTree(final Polygon polygon, final MultiPoint mp) {
		Geometry geo = polygon.getBoundary();
		Coordinate coordA = geo.getCoordinates()[0];
		Coordinate coordB = geo.getCoordinates()[2];
		
		double minX = Math.min(coordA.x, coordB.x);
		double maxX = Math.max(coordA.x, coordB.x);
		
		double minY = Math.min(coordA.y, coordB.y);
		double maxY = Math.max(coordA.y, coordB.y);
		
		this.mpQuad = new QuadTree<Coordinate>(0,0,2*maxX,2*maxY);
		
		for (int i = 0; i < mp.getNumPoints(); i++){
			Coordinate c = mp.getGeometryN(i).getCoordinate();
			this.mpQuad.put(c.x, c.y, c);
		}

	}

}
