/* *********************************************************************** *
 * project: matsim
 * MentalLinkSwitcher.java
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

package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v3.scenario.MyDataContainer;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MentalLinkSwitcher implements LinkSwitcher {

	RobustLineIntersector its = new RobustLineIntersector();

	GeometryFactory geofac = new GeometryFactory();

	private static final double COS_LEFT = Math.cos(Math.PI / 2);
	private static final double SIN_LEFT = Math.sin(Math.PI / 2);
	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	private final Scenario sc;
	private final Map<Id,LinkFinishLines> finishLines = new HashMap<Id,LinkFinishLines>();
	private ArrayList<Geometry> geos;

	public MentalLinkSwitcher(Scenario sc) {
		this.sc = sc;
		init();
	}

	private void init() {

		initGeometries();

		for (Link link : this.sc.getNetwork().getLinks().values()) {
			LinkFinishLines lines = new LinkFinishLines();
			for (Link next : link.getToNode().getOutLinks().values()) {
				
				// ignore non-walk2d links
				if (!next.getAllowedModes().contains("walk2d")) continue;
				
				Tuple<Coordinate,Coordinate> ls = null;
				if (next.getToNode() == link.getFromNode()) {
					ls = getPerpendicularLine(link);
				} else{
					ls = getVisibilityLine(link,next);
				}
				lines.finishLines.put(next.getId(), ls);
			}

			calcBorderLines(link,lines);

			this.finishLines.put(link.getId(), lines);
		}
	}

	private void calcBorderLines(Link link, LinkFinishLines lines) {

		double minWidth = getMinWidth(link);

		Coordinate c0 = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate c1 = MGC.coord2Coordinate(link.getToNode().getCoord());
		double l = c0.distance(c1);
		double dx = minWidth*(c1.x-c0.x)/l;
		double dy = minWidth * (c1.y-c0.y)/l;

		Coordinate r0 = new Coordinate(c0.x+dy,c0.y-dx);
		Coordinate r1 = new Coordinate(c1.x+dy,c1.y-dx);

		lines.rightBorder = new Tuple<Coordinate,Coordinate>(r0,r1);

		Coordinate l0 = new Coordinate(c0.x-dy,c0.y+dx);
		Coordinate l1 = new Coordinate(c1.x-dy,c1.y+dx);


		lines.leftBorder = new Tuple<Coordinate,Coordinate>(l0,l1);

	}

	private double getMinWidth(Link l) {
		
		LineString link = this.geofac.createLineString(new Coordinate[]{MGC.coord2Coordinate(l.getFromNode().getCoord()),MGC.coord2Coordinate(l.getToNode().getCoord())});
		Coord coord = l.getCoord();
		QuadTree<Coordinate> q = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree();
		Collection<Coordinate> coll = q.get(coord.getX(), coord.getY(), link.getLength()*2);
		double minDist = Double.POSITIVE_INFINITY;
		for (Coordinate c : coll) {
			Point p = this.geofac.createPoint(c);
			double dist = p.distance(link);
			if (dist < minDist) {
				minDist = dist;
			}
		}
		return minDist;
	}


	private void initGeometries() {
		this.geos = new ArrayList<Geometry>();
		for (Feature ft : this.sc.getScenarioElement(ShapeFileReader.class).getFeatureSet()) {
			this.geos.add(ft.getDefaultGeometry());
		}

	}

	private Tuple<Coordinate, Coordinate> getVisibilityLine(Link link, Link next) {

		Coordinate cFrom = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate cTo = MGC.coord2Coordinate(link.getToNode().getCoord());
		Coordinate cNextTo = MGC.coord2Coordinate(next.getToNode().getCoord());

		/*
		 * Limit the visible line calculation to the last 100.0 meters of a link.
		 * Otherwise a mental link switch could happen when an agent is far away
		 * from its next link which would result in problems in the PathForceModule
		 * where a force is calculated link Math.exp(distance to mental link).
		 */
		double maxLength = 100.0;	// TODO: which values makes sense here?
		double length = link.getLength();
		if (length > maxLength) {
			double dx = cTo.x - cFrom.x;
			double dy = cTo.y - cFrom.y;
			double dxy = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
			dx = dx * maxLength/dxy; 
			dy = dy * maxLength/dxy;
			cFrom = new Coordinate(cTo.x - dx, cTo.y - dy);
		}

		double dx = (cTo.x - cFrom.x)/10;
		double dy = (cTo.y - cFrom.y)/10;

			
		for (int i = 1; i <= 10; i++) {
			Coordinate tmp = new Coordinate(cFrom.x + dx*i,cFrom.y + dy*i);
			if (!intersectsEnv(cNextTo,tmp)) {
				
				// barrier between 4504 and 4505
				Coordinate[] barrier = new Coordinate[2];
				barrier[0] = tmp;			
				barrier[1] = cNextTo;
				
				LineString ls = geofac.createLineString(barrier);
				Point p = geofac.createPoint(cTo);
				double distance = ls.distance(p);
				if (distance > 5) continue;
				
				if (Algorithms.isLeftOfLine(cFrom, cNextTo, tmp) <= 0) {
					
					return new Tuple<Coordinate,Coordinate>(cNextTo,tmp);
				} else {
					return new Tuple<Coordinate,Coordinate>(tmp,cNextTo);
				}
			}
		}
		throw new RuntimeException("Link with id " + link.getId() + " intertsects the environment!");
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.LinkSwitcher#checkForMentalLinkSwitch(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, playground.gregor.sim2d_v2.simulation.floor.Agent2D)
	 */
	@Override
	public void checkForMentalLinkSwitch(Id curr, Id next, Agent2D agent) {

		if (agent.isMentalSwitched()) {
			return;
		}
		Coordinate pos = agent.getPosition();

		//Last link of leg
		if (next == null) {
			return;
		}

		LinkFinishLines lines = this.finishLines.get(curr);
		Tuple<Coordinate, Coordinate> fl = lines.finishLines.get(next);
		if (Algorithms.isLeftOfLine(pos, fl.getFirst(), fl.getSecond()) > 0) {
			LinkFinishLines nextLines = this.finishLines.get(next);
			if (Algorithms.isLeftOfLine(pos, nextLines.rightBorder.getFirst(), nextLines.rightBorder.getSecond()) > 0) {
				if (Algorithms.isLeftOfLine(pos, nextLines.leftBorder.getFirst(), nextLines.leftBorder.getSecond())<0){
					agent.switchMental();
				}
			}
		}

	}

	private boolean intersectsEnv(Coordinate c0, Coordinate c1) {
		for (Geometry geo : this.geos) {
			if (intersectsWithGeometry(geo,c0,c1)) {
				return true;
			}

		}
		return false;
	}

	private boolean intersectsWithGeometry(Geometry geo, Coordinate c0,
			Coordinate c1) {
		if (geo instanceof LineString) {
			return intersectsWithLineString((LineString)geo,c0,c1);
		} else if (geo instanceof Polygon){
			return intersectsWithPolygon((Polygon)geo,c0,c1);
		} else if (geo instanceof MultiPolygon) {
			return intersectsWithMultiGeometry(geo,c0,c1);
		} else if (geo instanceof MultiLineString) {
			return intersectsWithMultiGeometry(geo,c0,c1);
		}

		throw new RuntimeException("Unsupported geometry type:" + geo.getGeometryType());
	}

	private boolean intersectsWithMultiGeometry(Geometry geo,
			Coordinate c0, Coordinate c1) {
		for (int i = 0; i < geo.getNumGeometries(); i++) {
			if (intersectsWithGeometry(geo.getGeometryN(i),c0,c1)) {
				return true;
			}
		}
		return false;
	}

	private boolean intersectsWithPolygon(Polygon geo, Coordinate c0,
			Coordinate c1) {
		LineString ls = geo.getExteriorRing();
		return intersectsWithLineString(ls,c0,c1);
	}

	private boolean intersectsWithLineString(LineString geo, Coordinate c0,
			Coordinate c1) {
		Coordinate[] coords = geo.getCoordinates();
		for (int i = 0; i < coords.length-1; i++) {
			this.its.computeIntersection(c0, c1, coords[i],coords[i+1]);
			if (this.its.hasIntersection()) {
				return true;
			}
		}
		return false;
	}

	private Tuple<Coordinate,Coordinate> getPerpendicularLine(Link link) {
		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate c = new Coordinate(from.x - to.x, from.y - to.y);
		// length of finish line is 30 m// TODO does this make sense?
		double scale = 30 / Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
		c.x *= scale;
		c.y *= scale;
		Coordinate c1 = new Coordinate(COS_LEFT * c.x + SIN_LEFT * c.y, -SIN_LEFT * c.x + COS_LEFT * c.y);
		c1.x += to.x;
		c1.y += to.y;
		Coordinate c2 = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);
		c2.x += to.x;
		c2.y += to.y;

		return new Tuple<Coordinate,Coordinate>(c1,c2);
	}

	private static class LinkFinishLines {
		public final Map<Id,Tuple<Coordinate,Coordinate>> finishLines
		= new HashMap<Id,Tuple<Coordinate,Coordinate>>();

		public Tuple<Coordinate,Coordinate> rightBorder;
		public Tuple<Coordinate,Coordinate> leftBorder;
	}

}
