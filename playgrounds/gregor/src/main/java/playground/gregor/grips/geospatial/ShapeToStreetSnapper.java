/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeToStreetSnapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.grips.geospatial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.grips.config.GripsConfigModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapeToStreetSnapper {
	
	GeometryFactory geofac = new GeometryFactory();
	
	
	private static final double epsilon = 0.00001;
	private final Scenario sc;

	public ShapeToStreetSnapper(Scenario sc) {
		this.sc = sc;
	}

	private Polygon run(Polygon p) {

		List<Node> nodes  = getBoundaryNodes(p);
		fixOneWayStreets();
		
		FreeSpeedTravelTimeCalculator fs = new FreeSpeedTravelTimeCalculator();
		PersonalizableTravelDisutility cost = new TravelCost(p);
		LeastCostPathCalculator dijkstra = new Dijkstra(this.sc.getNetwork(), cost, fs);
		
		List<Node> finalNodes = new ArrayList<Node>();
		for (int i = 1; i < nodes.size(); i++) {
			Node n0 = nodes.get(i-1);
			Node n1 = nodes.get(i);
			Path path = dijkstra.calcLeastCostPath(n0, n1, 0);
			finalNodes.addAll(path.nodes.subList(0, path.nodes.size()-1));
			
		}

		Set<Integer> rmIdxs = new HashSet<Integer>();
		for (int i = 0; i < finalNodes.size(); i++) {
			Node n0 = finalNodes.get(i);
			for (int j = i+1; j < finalNodes.size(); j++) {
				Node n1 = finalNodes.get(j);
				if (n1 == n0) {
					for (int k = i+1; k <= j; k++) {
						rmIdxs.add(k);
					}
					i = j;
					break;
				}
			}
			
		}
		
		int idx = 0;
		Iterator<Node> it = finalNodes.iterator();
		while (it.hasNext()) {
			it.next();
			if (rmIdxs.contains(idx)) {
				it.remove();
			}
			idx++;
		}
		
		
		
		GisDebugger.addGeometry(p);
		GisDebugger.dump("/Users/laemmel/tmp/!!snapperP.shp");
		

		for (Node node : finalNodes) {
			Point pp = this.geofac.createPoint(MGC.coord2Coordinate(node.getCoord()));
			GisDebugger.addGeometry(pp);
		}
		GisDebugger.dump("/Users/laemmel/tmp/!!snapperPP.shp");
		Coordinate [] coords = new Coordinate[finalNodes.size()+1];
		for (int i = 0; i < coords.length-1; i++) {
			coords[i] = MGC.coord2Coordinate(finalNodes.get(i).getCoord());
		}
		coords[coords.length-1] = coords[0];
		LinearRing shell = this.geofac.createLinearRing(coords);
		Polygon ep = this.geofac.createPolygon(shell, null);
		GisDebugger.addGeometry(ep);
		GisDebugger.dump("/Users/laemmel/tmp/!!snapperEvacArea.shp");
		// TODO Auto-generated method stub
		return null;
	}
	


	private void fixOneWayStreets() {

		List<Link> rev = new ArrayList<Link>();
		for (Link link : this.sc.getNetwork().getLinks().values()) {
			
			boolean oneWay = true;
			for (Link l : link.getToNode().getOutLinks().values()) {
				if (l.getToNode().equals(link.getFromNode())) {
					oneWay = false;
					break;
				}
			}
			if (oneWay) {
				Link reverse = this.sc.getNetwork().getFactory().createLink(new IdImpl(link.getId().toString()+"reverse"), link.getToNode(), link.getFromNode());
				reverse.setFreespeed(link.getFreespeed());
				reverse.setLength(link.getLength());
				reverse.setNumberOfLanes(link.getNumberOfLanes());
				reverse.setCapacity(link.getCapacity());
				rev.add(reverse);
			}
			
		}
		for (Link reverse : rev) {
			this.sc.getNetwork().addLink(reverse);
		}
		
	}

	private List<Node> getBoundaryNodes(Polygon p) {
		double maxL = 0;
		Envelope e = new Envelope();
		for (Link l : this.sc.getNetwork().getLinks().values()) {
			if (maxL < l.getLength()/2) {
				maxL = l.getLength()/2;
			}
			e.expandToInclude(MGC.coord2Coordinate(l.getFromNode().getCoord()));
			e.expandToInclude(MGC.coord2Coordinate(l.getToNode().getCoord()));
			
		}
		QuadTree<Node> qTree = new QuadTree<Node>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		for (Node n : this.sc.getNetwork().getNodes().values()) {
			qTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		
		
		List<Node> nodes = new ArrayList<Node>();
		for (int i = 1; i < p.getExteriorRing().getNumPoints(); i++) {
			Coordinate c0 = p.getExteriorRing().getCoordinateN(i-1);
			Coordinate c1 = p.getExteriorRing().getCoordinateN(i);
			
			LineString ls = this.geofac.createLineString(new Coordinate[]{c0,c1});
			
			Coordinate center = new Coordinate((c0.x+c1.x)/2,(c0.y+c1.y)/2);
			double searchRange = Math.max(maxL, c0.distance(c1));
			Collection<Node> coll = qTree.get(center.x, center.y,searchRange);
			
			List<Link> iLs = new ArrayList<Link>();
			for (Node n : coll) {
				for (Link l : n.getOutLinks().values()) {
					Coordinate tmpC0 = MGC.coord2Coordinate(l.getFromNode().getCoord());
					Coordinate tmpC1 = MGC.coord2Coordinate(l.getToNode().getCoord());
					LineString tmpLs = this.geofac.createLineString(new Coordinate[]{tmpC0,tmpC1});
					
					if (ls.intersects(tmpLs)) {
						iLs.add(l);
					}
				}
				for (Link l : n.getInLinks().values()) {
					Coordinate tmpC0 = MGC.coord2Coordinate(l.getFromNode().getCoord());
					Coordinate tmpC1 = MGC.coord2Coordinate(l.getToNode().getCoord());
					LineString tmpLs = this.geofac.createLineString(new Coordinate[]{tmpC0,tmpC1});
					
					if (ls.intersects(tmpLs)) {
						iLs.add(l);
					}
				}
			}
			
			LinkSorter sorter = new LinkSorter(c0, c1);//kaputt
			Collections.sort(iLs, sorter);
			for (Link link : iLs) {
				Node tmp = link.getFromNode();
				if (contains(MGC.coord2Coordinate(tmp.getCoord()),p.getExteriorRing().getCoordinates())) {
					tmp = link.getToNode();
				}
				if (contains(MGC.coord2Coordinate(tmp.getCoord()),p.getExteriorRing().getCoordinates())) {
					throw new RuntimeException("should not happen!!");
				}
				if (nodes.size() == 0 || nodes.get(nodes.size()-1) != tmp) {
					nodes.add(tmp);
				}
			}
			
			
		}
		return nodes;
	}

	public static void main(String [] args) {
	
		String config = "/Users/laemmel/svn/shared-svn/studies/countries/de/hh/hafen_fest_evacuation/MATSimData/config.xml";
		Config c = ConfigUtils.loadConfig(config);
		Module m = c.getModule("grips");
		GripsConfigModule gm = new GripsConfigModule(m);
		c.getModules().put("grips", gm);
		
		String shape = gm.getEvacuationAreaFileName();
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(shape);
		String targetS = c.global().getCoordinateSystem();
		CoordinateReferenceSystem target;
		try {
			target = CRS.decode(targetS, true);
		} catch (NoSuchAuthorityCodeException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		
		CoordinateReferenceSystem src = r.getCoordinateSystem();
		FeatureCoordinateTransformer tr = new FeatureCoordinateTransformer(src, target);
		tr.transform(r.getFeatureSet());
		if (r.getFeatureSet().size() != 1) {
			throw new RuntimeException("we need one and only one feature!");
		}
		Geometry geo = r.getFeatureSet().iterator().next().getDefaultGeometry();
		Polygon p;
		if (geo instanceof Polygon) {
			p = (Polygon) geo;
		} else if (geo instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon)geo;
			if (mp.getNumGeometries() != 1) {
				throw new RuntimeException("MultiPolygon has to contain one and only one Polygon!");
			}
			p = (Polygon) mp.getGeometryN(0);
		} else {
			throw new RuntimeException("Geometry type: " + geo.getGeometryType() + " not supportet!");
		}
		
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		ShapeToStreetSnapper snapper = new ShapeToStreetSnapper(sc);
		Polygon snapped = snapper.run(p);
		
	}

	
	//helper
	
	private static class LinkSorter implements Comparator<Link> {

		Map<Link,Double> distCache = new HashMap<Link,Double>();
		private final Coordinate c0;
		private final Coordinate c1;
		
		public LinkSorter(Coordinate c0, Coordinate c1) {
			this.c0 = c0;
			this.c1 = c1;
			
		}
		
		
		@Override
		public int compare(Link o1, Link o2) {
			double dist1 = getDistToC0(o1);
			double dist2 = getDistToC0(o2);
			
			if (dist1 < dist2) {
				return -1;
			}
			
			if (dist1 > dist2) {
				return 1;
			}
			return 0;
		}
		
		private double getDistToC0(Link o1) {
			Double dist1 = this.distCache.get(o1);
			if (dist1 == null) {
				Coordinate intersection = new Coordinate(Double.NaN,Double.NaN);
				computeLineIntersection(this.c0,this.c1,MGC.coord2Coordinate(o1.getFromNode().getCoord()),MGC.coord2Coordinate(o1.getToNode().getCoord()),intersection);
				dist1 = this.c0.distance(intersection);
				this.distCache.put(o1, dist1);
			}
			return dist1;
		}


		private boolean computeLineIntersection(Coordinate a0, Coordinate a1, Coordinate b0, Coordinate b1, Coordinate intersectionCoordinate) {
			
			
			
			double a = (b1.x - b0.x) * (a0.y - b0.y) - (b1.y - b0.y) * (a0.x - b0.x);
			double b = (a1.x - a0.x) * (a0.y - b0.y) - (a1.y - a0.y) * (a0.x - b0.x);
			double denom = (b1.y - b0.y) * (a1.x - a0.x) - (b1.x - b0.x) * (a1.y - a0.y);
			
			//conincident
			if (Math.abs(a) < epsilon && Math.abs(b) < epsilon && Math.abs(denom) < epsilon) {
				intersectionCoordinate.x = (a0.x+a1.x) /2;
				intersectionCoordinate.y = (a0.y+a1.y) /2;
				return true;
			}
			
			//parallel
			if (Math.abs(denom) < epsilon) {
				return false;
			}
			
			double ua = a / denom;
			double ub = b / denom;
			
			if (ua < 0 || ua > 1 || ub < 0 || ub > 1) {
				return false;
			}
			
			double x = a0.x + ua * (a1.x - a0.x);
			double y = a0.y + ua * (a1.y - a0.y);
			intersectionCoordinate.x = x;
			intersectionCoordinate.y = y;
			
			return true;
		}
	}
	
	
	/**
	 * Tests whether a polygon (defined by an array of Coordinate) contains a Coordinate
	 * @param coord
	 * @param p
	 * @return true if coord lays within p
	 */
	private boolean contains(Coordinate coord, Coordinate[] p) {
		int wn = getWindingNumber(coord,p);
		return wn != 0;
	}

	//winding number algorithm
	//see softSurfer (www.softsurfer.com) for more details
	private int getWindingNumber(Coordinate c, Coordinate[] p) {


		int wn = 0;

		for (int i=0; i<p.length-1; i++) {
			if (p[i].y <= c.y) {
				if (p[i+1].y > c.y)
					if (isLeftOfLine( c,p[i], p[i+1]) > 0)
						++wn;
			}
			else {
				if (p[i+1].y <= c.y)
					if (isLeftOfLine( c,p[i], p[i+1]) < 0)
						--wn;
			}

			//test for early return here
		}
		return wn;
	}
	
	
	/**
	 * tests if line segment a0a1 intersects b0b1
	 * @param a0
	 * @param a1
	 * @param b0
	 * @param b1
	 * @return true if segment a0a1 intersects b0b1
	 */
	private boolean intersects(Coordinate a0,
			Coordinate a1, Coordinate b0, Coordinate b1) {
		double b0Side = isLeftOfLine(b0,a0,a1);
		if (b0Side == 0) {
			return true;
		}
		double b1Side = isLeftOfLine(b1,a0,a1);
		if (b1Side == 0) {
			return true;
		}
		return b0Side*b1Side < 0;
	}
	
	/**
	 * tests whether coordinate c0 is located left of the infinite vector that runs through c1 and c2
	 * @param c0 the coordinate to test
	 * @param c1 one coordinate of the vector
	 * @param c2 another coordinate of the same vector
	 * @return >0 if c0 is left of the vector
	 * 		  ==0 if c0 is on the vector
	 * 		   <0 if c0 is right of the vector
	 */
	private double isLeftOfLine(Coordinate c0, Coordinate c1, Coordinate c2) {
		return (c2.x - c1.x)*(c0.y - c1.y) - (c0.x - c1.x) * (c2.y - c1.y);
	}

	
	private static final class TravelCost implements PersonalizableTravelDisutility {
		
		private final Polygon p;

		private final GeometryFactory geofac = new GeometryFactory();
		
		public TravelCost(Polygon p) {
			this.p = p;
		}
		
		@Override
		public double getLinkTravelDisutility(Link link, double time) {
			Coordinate c0 = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate c1 = MGC.coord2Coordinate(link.getToNode().getCoord());
			LineString ls = this.geofac.createLineString(new Coordinate[]{c0,c1});
			if (ls.intersects(this.p) || this.p.covers(ls)) {
				return link.getLength()+10000;
			}
			
			return link.getLength();
		}
		
		@Override
		public void setPerson(Person person) {
			// TODO Auto-generated method stub
			
		}
	};

}
