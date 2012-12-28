/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.sim2d_v3.simulation.floor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Triangle;

public class FinishLineCrossedChecker {

	private final Scenario sc;

	private HashMap<Id, FinishLines> finishLines;
	private HashMap<Id, LineString> perpendicularLines;

	private final HashMap<Id, ArrivalArea> arrivalAreas = new HashMap<Id,ArrivalArea>();

	private GeometryFactory geofac;

	private List<Geometry> geos;


	private static final double COS_LEFT = Math.cos(Math.PI / 2);
	private static final double SIN_LEFT = Math.sin(Math.PI / 2);
	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);


	public FinishLineCrossedChecker(Scenario sc) {
		this.sc = sc;
	}

	public void init() {

		initGeometries();

		this.finishLines = new HashMap<Id, FinishLines>();
		this.perpendicularLines = new HashMap<Id, LineString>();
		this.geofac = new GeometryFactory();
		for (Link link : this.sc.getNetwork().getLinks().values()) {
			FinishLines lines = new FinishLines();
			ArrivalArea a = new ArrivalArea();
			this.arrivalAreas.put(link.getId(), a);
			for (Link next : link.getToNode().getOutLinks().values()) {
				LineString ls = getPerpendicularLine(link);
				this.perpendicularLines.put(link.getId(), ls);
				if (next.getToNode() != link.getFromNode()) {
					ls = getBisectorialLine(link,next);
					if (ls == null) { //collinear links
						ls = getPerpendicularLine(link);
					}
				}
				lines.finishLines.put(next.getId(), ls);
			}
			this.finishLines.put(link.getId(), lines);

		}

	}

	private void initGeometries() {
		this.geos = new ArrayList<Geometry>();
		for (SimpleFeature ft : this.sc.getScenarioElement(ShapeFileReader.class).getFeatureSet()) {
			this.geos.add((Geometry) ft.getDefaultGeometry());
		}

	}

	private LineString getBisectorialLine(Link link, Link next) {

		Coordinate a = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate b = MGC.coord2Coordinate(link.getToNode().getCoord());



		Coordinate c = MGC.coord2Coordinate(next.getToNode().getCoord());
		Coordinate d = Triangle.angleBisector(a, b, c);

		double lengthAb = a.distance(b);

		double abx = b.x - a.x;
		double aby = b.y - a.y;

		double nbx = a.x + abx/lengthAb * (lengthAb );
		double nby = a.y + aby/lengthAb * (lengthAb );

		Coordinate nb = new Coordinate(nbx,nby);

		double lengthBd = b.distance(d);

		//happens if links are collinear
		if (lengthBd == 0){
			return null;
		}
		double dx = 30 *(d.x - b.x) / lengthBd;
		double dy = 30 * (d.y - b.y)/ lengthBd;

		Coordinate c1 = new Coordinate(nbx + dx, nby + dy);
		Coordinate c2 = new Coordinate(nbx - dx, nby - dy);




		Geometry bisec = this.geofac.createLineString(new Coordinate[]{c1,c2}).buffer(0.);

		List<Coordinate> intersects = getIntersections(bisec);

		double minCc1 = Double.POSITIVE_INFINITY;
		double minCc2 = Double.POSITIVE_INFINITY;

		Coordinate cc1 = c1;
		Coordinate cc2 = c2;

		for (Coordinate intersection : intersects) {
			if (c1.distance(intersection) < c2.distance(intersection)) {
				if (minCc1 > intersection.distance(nb)) {
					minCc1 = intersection.distance(nb);
					cc1 = intersection;
				}
			} else {
				if (minCc2 > intersection.distance(nb)) {
					minCc2 = intersection.distance(nb);
					cc2 = intersection;
				}
			}
		}

		LineString ret;
		if ( Algorithms.isLeftOfLine(cc1, a, cc2) > 0) {
			ret = this.geofac.createLineString(new Coordinate[]{cc1,cc2});
		} else {
			ret = this.geofac.createLineString(new Coordinate[]{cc2,cc1});
		}


		Coordinate c0 = MGC.coord2Coordinate(next.getFromNode().getCoord());
		double aa0 = c.x - c0.x;
		double aa1 = c.y - c0.y;
		Coordinate ccc1 = ret.getCoordinateN(0);
		Coordinate ccc2 = ret.getCoordinateN(1);

		Coordinate [] arrival = new Coordinate[]{ccc2, new Coordinate(ccc2.x+aa0,ccc2.y+aa1),new Coordinate(ccc1.x+aa0,ccc1.y+aa1),ccc1};
		this.arrivalAreas.get(link.getId()).arrivalAreas.put(next.getId(), arrival);


		return ret;
	}

	private List<Coordinate> getIntersections(Geometry bisec) {
		List<Coordinate> ret = new ArrayList<Coordinate>();
		for (Geometry geo : this.geos) {
			Geometry itrs = geo.intersection(bisec);
			if (itrs.isEmpty()) {
				continue;
			}

			if (itrs instanceof Point) {
				ret.add(itrs.getCoordinate());
			} else if (itrs instanceof MultiPoint){
				for (int i = 0; i < itrs.getNumGeometries(); i++) {
					ret.add(itrs.getGeometryN(i).getCoordinate());
				}
			} else  {
				throw new RuntimeException("Geometry type" + itrs.getGeometryType() + " is not supported here!");
			}
		}
		return ret;
	}

	private LineString getPerpendicularLine(Link link) {
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
		LineString ls = this.geofac.createLineString(new Coordinate[] { c1, c2 });

		return ls;
	}

	public boolean crossesFinishLine(Id currentLinkId, Id nextLinkId,
			Coordinate oldPos, Coordinate newPos) {
		LineString ls;
		if (nextLinkId == null) {
			ls = this.perpendicularLines.get(currentLinkId);
		} else {
			ls = this.finishLines.get(currentLinkId).finishLines.get(nextLinkId);
		}


		if (Algorithms.isLeftOfLine(newPos, ls.getCoordinateN(0), ls.getCoordinateN(1)) > 0) {
			//			Coordinate[] coords = this.arrivalAreas.get(currentLinkId).arrivalAreas.get(nextLinkId);
			//
			//			//TODO repair this!!
			//			if (coords == null) {
			//				return true;
			//			}
			//
			//			if (Algorithms.isLeftOfLine(newPos, coords[0],coords[1]) > 0 && Algorithms.isLeftOfLine(newPos, coords[2],coords[3]) > 0) {
			//				return true;
			//			}
			return true;
			//			LineString trajectory = this.geofac.createLineString(new Coordinate[]{oldPos,newPos});
			//			return trajectory.crosses(ls);
		}

		return false;
	}

	private static final class FinishLines {
		public Map<Id,LineString> finishLines = new HashMap<Id,LineString>();
	}

	private static final class ArrivalArea {
		public Map<Id,Coordinate[]> arrivalAreas = new HashMap<Id,Coordinate[]>();
	}

}
