/* *********************************************************************** *
 * project: org.matsim.*
 * Network2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class Network2D {

	private final List<Floor> floors = new ArrayList<Floor>();

	private final Floor floor;

	private NetworkImpl networkLayer;

	private HashMap<Link, LineString> finishLines;

	private HashMap<Link, Coordinate> drivingDirections;

	private HashMap<Link, LineString> linksGeos;

	private Map<Id, LineString> ls;

	//needed to generated "finish lines"
	private static final GeometryFactory geofac = new GeometryFactory();

	//needed to generated "finish lines"
	private static final double COS_LEFT = Math.cos(Math.PI/2);
	//needed to generated "finish lines"
	private static final double SIN_LEFT = Math.sin(Math.PI/2);
	//needed to generated "finish lines"
	private static final double COS_RIGHT = Math.cos(-Math.PI/2);
	//needed to generated "finish lines"
	private static final double SIN_RIGHT = Math.sin(-Math.PI/2);



	public Network2D(NetworkImpl network, Map<MultiPolygon, NetworkLayer> floors, SegmentedStaticForceField sff, Map<Id, LineString> ls) {
		this.ls = ls;
		if (floors.size() > 1) {
			throw new RuntimeException("this has not been implemented yet!");
		}
		Entry<MultiPolygon,NetworkLayer> e  = floors.entrySet().iterator().next();
		this.networkLayer = e.getValue();
		init();
		try {
			initII();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		this.floor = new Floor(this.finishLines,this.linksGeos,this.drivingDirections,sff);
		this.floors.add(this.floor);
	}


	//"finish lines" now taken from nodes;
	private void initII() throws IOException {
		this.finishLines.clear();
		String nf = "/home/laemmel/devel/sim2d/data/duisburg/nodes.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(nf);
		Iterator it = fs.getFeatures().iterator();
//		Map<Id,LineString> nodes = new HashMap<Id, LineString>();
		Envelope e = fs.getBounds();
		QuadTree<LineString> nodes = new QuadTree<LineString>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			Polygon p = (Polygon) ((MultiPolygon)geo).getGeometryN(0);
//			Id id = new IdImpl((Long) ft.getAttribute("ID"));
			nodes.put(p.getCentroid().getX(),p.getCentroid().getY(), p.getExteriorRing());
			
		}
		
		for (Link link : this.networkLayer.getLinks().values()) {
			Coord c = link.getToNode().getCoord();
			LineString ls = nodes.get(c.getX(),c.getY());
			this.finishLines.put(link, ls);
		}
		
	}


	//Here the perpendicular "finish lines" will be calculated
	private void init() {
		this.finishLines = new HashMap<Link,LineString>();
		this.drivingDirections = new HashMap<Link, Coordinate>();
		this.linksGeos = new HashMap<Link,LineString>();
		for (Link link : this.networkLayer.getLinks().values()) {
			LineString ls = getPerpendicularFinishLine(link, link.getToNode());
			this.finishLines.put(link, ls);
			Coordinate c = new Coordinate(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX(),link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY());
			double length = Math.sqrt(Math.pow(c.x, 2)+Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;
			this.drivingDirections.put(link,c);
			
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			from.x -= c.x*10;
			from.y -= c.y*10;
			
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			to.x += c.x*10;
			to.y += c.y*10;			

			LineString ls1 = this.geofac.createLineString(new Coordinate[] {from,to});
			this.linksGeos.put(link, ls1);
			//				GisDebugger.addGeometry(ls1);
			//				GisDebugger.dump("../../tmp/finishLine.shp");
		}
//		GisDebugger.dump("/home/laemmel/devel/sim2d/tmp/finishLine.shp");

	}

	//needed to generated "finish lines"
	private LineString getPerpendicularFinishLine(Link link, Node node) {
		Coordinate t = MGC.coord2Coordinate(node.getCoord());
		Coordinate pred = MGC.coord2Coordinate(link.getFromNode().getCoord());
		
//		LineString l = this.ls.get(link.getId());
//		if (l == null  && !link.getId().toString().contains("el")) {
//			int i = Integer.parseInt(link.getId().toString());
//			if (i < 100000) {
//				i += 100000;
//			} else if (i >= 100000){
//				i -= 100000;
//			}
//			IdImpl id = new IdImpl(i);
//			l = this.ls.get(id);
//		}
//		if (l == null) {
//			pred = MGC.coord2Coordinate(link.getFromNode().getCoord());
//		} else {
//			double dist = l.getStartPoint().getCoordinate().distance(MGC.coord2Coordinate(node.getCoord()));
//			double dist2 = l.getEndPoint().getCoordinate().distance(MGC.coord2Coordinate(node.getCoord()));
//			if ( dist < dist2) {
//				t = l.getCoordinateN(0);
//				pred = l.getCoordinateN(1);
//			} else {
//				t = l.getCoordinateN(l.getNumPoints()-1);
//				pred = l.getCoordinateN(l.getNumPoints()-2);
//			}
//		}
		Coordinate c = new Coordinate(pred.x-t.x,pred.y-t.y);
//		double scale = ((link.getNumberOfLanes()*0.71)/2)/ Math.sqrt(Math.pow(c.x, 2)+Math.pow(c.y, 2));
		double scale = 6/ Math.sqrt(Math.pow(c.x, 2)+Math.pow(c.y, 2));
		c.x *= scale;
		c.y *= scale;
		Coordinate c1 = new Coordinate(COS_LEFT*c.x + SIN_LEFT*c.y,-SIN_LEFT*c.x+COS_LEFT*c.y);
		c1.x += t.x;
		c1.y += t.y;
		Coordinate c2 = new Coordinate(COS_RIGHT*c.x + SIN_RIGHT*c.y,-SIN_RIGHT*c.x+COS_RIGHT*c.y);
		c2.x += t.x;
		c2.y += t.y;
		LineString ls = this.geofac.createLineString(new Coordinate[]{c1,c2});
//		GisDebugger.addGeometry(ls);
		return ls;
	}


	public List<Floor> getFloors() {
		return this.floors;
	}

	public void removeAgent(Agent2D agent) {
		this.floor.removeAgent(agent);
	}


	public void move(double time) {
		this.floor.move(time);
	}


	public void addAgent(Agent2D agent) {
		this.floor.addAgent(agent);

	}



}
