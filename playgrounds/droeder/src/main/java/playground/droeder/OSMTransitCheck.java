/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.andreas.osmBB.OsmTransitMain;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class OSMTransitCheck {
	public static void main(String[] args){
		String DIR = DaPaths.OUTPUT + "osm/";
		
		/*
		 * generate berlin_subway.osm with osmosis
		 * 
		 * osmosis --rx berlin.osm --tf accept-ways railway=subway 
		 * 		--tf accept-relations route=subway
		 * 		--used-node --wx berlin_subway.osm
		 */
		String INFILE = DIR + "berlin_subway.osm";
		String OUTNET = DIR + "osm_berlin_subway_net.xml";
		String OUTSCHED = DIR + "osm_berlin_subway_sched.xml";
		String OUTVEH = DIR + "osm_berlin_subway_veh.xml";
		
		String OUTNETSHP = DIR + "osm_berlin_subway_net.shp";
		String OUTSCHEDSTOPSSHP = DIR + "osm_berlin_subway_stops_by_sched.shp";
		String OSMSUBWAYLINKSEQUENCESHP = DIR + "osm_subway_link_sequence.shp";
		String OSMCOMPLETESEQUENCE = DIR + "osm_subway_link_complSequ.shp";
		
		String[] modes = new String[1];
		modes[0] = "subway";
		
		new OsmTransitMain(INFILE, TransformationFactory.WGS84, TransformationFactory.DHDN_GK4,OUTNET, OUTSCHED, OUTVEH).convertOsm2Matsim(modes);
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		
		new MatsimNetworkReader(sc).readFile(OUTNET);
		// write network 2 shape
		DaShapeWriter.writeLinks2Shape(OUTNETSHP, sc.getNetwork().getLinks(), null);
		new TransitScheduleReader(sc).readFile(OUTSCHED);
		
		
		// create shape with lineString for every route, based on the TransitRouteStopSequence
		Map<String, SortedMap<Integer, Coord>> lsm = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer, Coord> ls;
		Map<String, SortedMap<String, String>> attribs = new HashMap<String, SortedMap<String,String>>();
		SortedMap<String, String> values;
		
		String id;
		int cnt;
		boolean first = true;
		TransitRouteStop start = null;
		for(TransitLine l : sc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute r: l.getRoutes().values()){
				first = true;
				values= new TreeMap<String, String>();
				values.put("route", l.getId().toString() + "_" + r.getId().toString());
				cnt = 0;
				for(TransitRouteStop s: r.getStops()){
					if(first){
						first = false;
						start = s;
					}else{
						id = start.getStopFacility().getId().toString()+ "_" + s.getStopFacility().getId().toString() + "_" + String.valueOf(cnt);
						ls = new TreeMap<Integer, Coord>();
						ls.put(0, start.getStopFacility().getCoord());
						ls.put(1, s.getStopFacility().getCoord());
						lsm.put(id, ls);
						attribs.put(id, values);
						start = s;
						cnt++;
					}
				}
			}
		}
		DaShapeWriter.writeDefaultLineString2Shape(OUTSCHEDSTOPSSHP, "RouteStopSequence by schedule", lsm, attribs);
		
		
		// create shp with lineString for every link on every route, based on the linkSequence of the route
		lsm = new HashMap<String, SortedMap<Integer,Coord>>();
		attribs = new HashMap<String, SortedMap<String,String>>();
		// create shp with lineString for the complete linkSequence on every route
		Map<String, SortedMap<Integer, Coord>> complSequ = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer, Coord> lsSequ; 
		
		for(TransitLine l : sc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute r : l.getRoutes().values()){
				values = new TreeMap<String, String>();
				values.put("line+route", l.getId().toString() + "_" + r.getId().toString());
				lsSequ = new TreeMap<Integer, Coord>();
				lsSequ.put(0, sc.getNetwork().getLinks().get(r.getRoute().getStartLinkId()).getToNode().getCoord());
				System.out.print(l.getId().toString() + " " + r.getId().toString());
				cnt =0;
				for(Id lId : r.getRoute().getLinkIds()){
					ls = new TreeMap<Integer, Coord>();
					id = l.getId().toString() + "_" + r.getId().toString() + "_" + String.valueOf(cnt);
					attribs.put(id, values);
					
					ls.put(0, sc.getNetwork().getLinks().get(lId).getFromNode().getCoord());
					ls.put(1, sc.getNetwork().getLinks().get(lId).getToNode().getCoord());
					lsSequ.put(cnt + 1, sc.getNetwork().getLinks().get(lId).getToNode().getCoord());
					
					lsm.put(id, ls);
					cnt++;
				}
				complSequ.put(l.getId().toString() +"_" + r.getId().toString(), lsSequ);
				System.out.println("\t #links: " +cnt );
			}
		}
		
//		DaShapeWriter.writeDefaultLineString2Shape(OSMSUBWAYLINKSEQUENCESHP, "RouteLinkSequence by schedule", lsm, attribs);
		DaShapeWriter.writeDefaultLineString2Shape(OSMCOMPLETESEQUENCE, "RouteLinkSequence by schedule ", complSequ, null);
	}

}
