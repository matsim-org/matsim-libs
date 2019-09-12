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
package playground.vsp.analysis.modules.ptAccessibility.stops;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 *
 */
public class PtStopMap {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PtStopMap.class);
	
	public static final String FILESUFFIX = "AccessMap";
	
	private GeometryFactory f;

	private HashMap<String, Map<String, Polygon>> map;

	private Map<String, Circle> cluster;

	private String mode;

	public PtStopMap(String mode, Map<String, Circle> cluster ) {
		this.f = new GeometryFactory();
		this.mode = mode;
		
		this.map = new HashMap<String, Map<String, Polygon>>();
		for(String s: cluster.keySet()){
			this.map.put(s, new HashMap<String, Polygon>());
		}
		this.cluster = cluster;
	}
	
	public void addStop(TransitStopFacility stop){
		Polygon g;
		
		for(Entry<String, Map<String, Polygon>> e: this.map.entrySet()){
			g = this.cluster.get(e.getKey()).createPolygon(this.f, stop.getCoord());
			e.getValue().put(stop.getId().toString(), g);
		}
	}

	/**
	 * @param outputFolder
	 */
	public Map<String, MultiPolygon> getCluster() {
		Polygon[] p;
		MultiPolygon mp ;
		Map<String, MultiPolygon> cluster = new HashMap<String, MultiPolygon>();
		
		for(Entry<String, Map<String, Polygon>> e: this.map.entrySet()){
			p = e.getValue().values().toArray(new Polygon[e.getValue().size()]);
			mp = this.f.createMultiPolygon(p);
			cluster.put(e.getKey(), mp);
		}
		return cluster;
	}
	
	public String getMode(){
		return this.mode;
	}
	
	public boolean contains(Coordinate c, String cluster){
		Point p = MGC.coordinate2Point(c);
		for(Geometry g: this.map.get(cluster).values()){
			if(g.contains(p)) return true;
		}
		return false;
	}
	
}

