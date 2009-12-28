/* *********************************************************************** *
 * project: org.matsim.*
 * LineStringTree.java
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

package playground.gregor.snapshots.writers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.evacuation.collections.gnuclasspath.TreeMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class LineStringTree {
	
	private static final double TOLERANCE = 0.1;
	private final Network network;
	private final HashMap<String, TreeMap<Double, LineSegment>> lsMap;
	private final GeometryFactory geofac;

	public LineStringTree(final Collection<Feature> lsFts, final Network network) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.lsMap = buildLsMap(lsFts);
		
		
	}

	public TreeMap<Double,LineSegment> getTreeMap(final String id) {
		return this.lsMap.get(id);
	}
	
	private HashMap<String,TreeMap<Double,LineSegment>> buildLsMap(final Collection<Feature> lsFts) {

		final HashMap<String,TreeMap<Double,LineSegment>> lsMap = new HashMap<String,TreeMap<Double,LineSegment>>();
		for (final Feature ft : lsFts) {
			final String id1 = ((Integer) ft.getAttribute(1)).toString();
			final String id2 = ((Integer)(((Integer) ft.getAttribute(1)) + 100000)).toString(); 
			final Geometry g = ft.getDefaultGeometry();
			final LineString ls;
			if (g instanceof LineString) {
				ls = (LineString) g;
			} else if ( g instanceof MultiLineString) {
				ls = (LineString)((MultiLineString)g).getGeometryN(0);
			} else {
				throw new RuntimeException("Feature contains no LineString!");
			}
			final Link l1 = this.network.getLinks().get(new IdImpl(id1));
			final Link l2 = this.network.getLinks().get(new IdImpl(id2));
			if (l1 == null) {
				continue;
			}
			final double ll = l1.getLength();
			final double lls = ls.getLength();
			final double dev = Math.abs(ll-lls) / lls;
			
			if (dev > TOLERANCE ) {
				handleFragmentedLink(ls,l1, lsMap);
			} else {
				lsMap.put(id1, getLsTree(ls,l1));
				lsMap.put(id2, getLsTree(ls,l2));
			}
		}
		for (final Link link : this.network.getLinks().values()) {
			if (!lsMap.containsKey(link.getId().toString())) {
				lsMap.put(link.getId().toString(), getDummyLsTree(link));
			}
			
		}
		
		
		return lsMap;
	}

	private TreeMap<Double, LineSegment> getDummyLsTree(final Link link) {
		final TreeMap<Double, LineSegment> dummy = new TreeMap<Double, LineSegment>();
		final LineSegment ls = new LineSegment(MGC.coord2Coordinate(link.getFromNode().getCoord()),MGC.coord2Coordinate(link.getToNode().getCoord()));
		dummy.put(0., ls);
		
		return dummy;
	}

	private void handleFragmentedLink(final LineString ls, final Link l, final HashMap<String,TreeMap<Double,LineSegment>> lsMap) {
		final Link lr = getLR(l);
		final double dev = Math.abs((lr.getLength()+l.getLength())-ls.getLength()) / ls.getLength();
		if (dev > TOLERANCE ) {
			throw new RuntimeException("Could not find lr link!");
		}
		final Coordinate cfrom = MGC.coord2Coordinate(l.getFromNode().getCoord());
		final LineString ls1 = getLs(ls,l, cfrom);
		final Coordinate cto = MGC.coord2Coordinate(lr.getToNode().getCoord());
		final LineString ls2 = getLs(ls,lr, cto);
		lsMap.put(l.getId().toString(), getLsTree(ls1,l));
		lsMap.put(lr.getId().toString(), getLsTree(ls2,lr));
	}

	private LineString getLs(final LineString ls, final Link l, final Coordinate cfrom) {

		final double length = l.getLength();
	    final ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		double currL =  0;
		if (ls.getStartPoint().getCoordinate().distance(cfrom) > ls.getEndPoint().getCoordinate().distance(cfrom) ) {
			coords.add(ls.getCoordinateN(ls.getNumPoints()-1));
			for (int pos = ls.getNumPoints()-1; pos > 0; pos--) {
				final LineSegment lseg = new LineSegment(ls.getCoordinateN(pos),ls.getCoordinateN(pos-1));
				currL += lseg.getLength();
				if (currL < length) {
					coords.add(ls.getCoordinateN(pos-1));
				} else {
					final double toGo = currL - length;
					final double dx = lseg.p1.x - lseg.p0.x;
					final double dy = lseg.p1.y - lseg.p0.y;
					final double scale = toGo / lseg.getLength();
					coords.add(new Coordinate(lseg.p0.x + dx * scale , lseg.p0.y + dy * scale));
				}
				
			}
			
		} else {
			coords.add(ls.getCoordinateN(0));
			for (int pos = 0; pos < ls.getNumPoints()-1; pos++) {
				final LineSegment lseg = new LineSegment(ls.getCoordinateN(pos),ls.getCoordinateN(pos+1));
				currL += lseg.getLength();
				if (currL < length) {
					coords.add(ls.getCoordinateN(pos+1));
				} else {
					final double toGo = currL - length;
					final double dx = lseg.p1.x - lseg.p0.x;
					final double dy = lseg.p1.y - lseg.p0.y;
					final double scale = toGo / lseg.getLength();
					coords.add(new Coordinate(lseg.p0.x + dx * scale , lseg.p0.y + dy * scale));
				}
				
			}
			
		}
		
		Coordinate [] ca = new Coordinate [coords.size()];
		ca = coords.toArray(ca);
		return this.geofac.createLineString(ca);
	}

	private Link getLR(final Link l1) {
		
		Node n = l1.getFromNode();
		if (n.getOutLinks().size() != 1) {
			n = l1.getToNode();
		}
		if (n.getOutLinks().size() != 1) {
			throw new RuntimeException("Could not find lr link!");
		}

		return n.getOutLinks().values().iterator().next();
	}

	private TreeMap<Double, LineSegment> getLsTree(final LineString ls, final Link l) {
		final Coordinate cfrom = MGC.coord2Coordinate(l.getFromNode().getCoord());
		final TreeMap<Double,LineSegment> lsTree = new TreeMap<Double,LineSegment>();
		if (ls.getStartPoint().getCoordinate().distance(cfrom) > ls.getEndPoint().getCoordinate().distance(cfrom) ) {
			double distance = 0;
			for (int pos = ls.getNumPoints()-1; pos > 0; pos--) {
				final LineSegment lseg = new LineSegment(ls.getCoordinateN(pos),ls.getCoordinateN(pos-1));
				lsTree.put(distance, lseg);
				distance += lseg.getLength();
			}
		} else {
			double distance = 0;
			for (int pos = 0; pos < ls.getNumPoints()-1; pos++) {
				final LineSegment lseg = new LineSegment(ls.getCoordinateN(pos),ls.getCoordinateN(pos+1));
				lsTree.put(distance, lseg);
				distance += lseg.getLength();
			}
		}
		return lsTree;
	}

}
