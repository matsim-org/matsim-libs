/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.benjamin.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class Links2ShapeWriter {

	private static final Logger log = Logger.getLogger(Links2ShapeWriter.class);

	public static void writeLinks2Shape(String fileName, Map<Id<Link>, Link> links, Map<Id, SortedMap<String, String>> attributes){
		PolylineFeatureFactory factory = null;
		if(!(attributes == null) && (attributes.size() > 0)){
			for(SortedMap<String, String> m : attributes.values()){
				factory = initLineFeatureType("transitLines", m);
				break;
			}
		}else{
			factory = initLineFeatureType("transitLines", null);
		}

		write(createLinkFeatures(links, attributes, factory), fileName);
	}

	public static void writeNodes2Shape(String fileName, Map<Id, Node> nodes){
		PointFeatureFactory factory = initPointFeatureType("nodes", null);
		write(createNodeFeatures(nodes, factory), fileName);
	}

	public static void writeDefaultPoints2Shape(String fileName, String name, Map<String, Coord> points, Map<String, SortedMap<String, String>> attributes){
		PointFeatureFactory.Builder builder = new PointFeatureFactory.Builder();
		builder.setName("nodes");
		builder.addAttribute("id", String.class);

		if(!(attributes == null)){
			for (SortedMap<String, String> m : attributes.values()){
				for(String s : m.keySet()){
					builder.addAttribute(s, String.class);
				}
				break;
			}
		}

		write(createDefaultPointFeature(points, attributes, builder.create()), fileName);
	}

	/**
	 * if lines2write == null all lines are written
	 * 
	 * @param fileName
	 * @param schedule
	 * @param lines2write
	 */
	public static void writeTransitLines2Shape(String fileName, TransitSchedule schedule, Collection<Id> lines2write, Map<Id, SortedMap<String, String>> attributes){
		PolylineFeatureFactory factory = null;
		if(!(attributes == null) && (attributes.size() > 0)){
			for(SortedMap<String, String> m : attributes.values()){
				factory = initLineFeatureType("transitLines", m);
				break;
			}
		}else{
			factory = initLineFeatureType("transitLines", null);
		}
		write(createRouteFeatures(schedule, lines2write, attributes, factory), fileName);
	}

	/**
	 * if stops2write == null all stops are written
	 * 
	 * @param fileName
	 * @param stops
	 * @param stops2write
	 */
	public static void writeRouteStops2Shape(String fileName, Map<Id, TransitStopFacility> stops, Collection<Id> stops2write){
		PointFeatureFactory factory = initPointFeatureType("TransitRouteStops", null);
		write(createStopFeatures(stops, stops2write, factory), fileName);
	}


	public static void writePointDist2Shape (String fileName, Map<String, Tuple<Coord, Coord>> points, Map<String, SortedMap<String, String>> attributes){
		PolylineFeatureFactory factory = null;
		for (SortedMap<String, String> m : attributes.values()){
			factory = initLineFeatureType("distance", m);
			break;
		}

		write(createPointDistanceFeatures(points,attributes, factory), fileName);
	}
	/**
	 * @param fileName
	 * @param name
	 * @param lineStrings
	 * @param attributes
	 */
	public static void writeDefaultLineString2Shape(String fileName, String name,  Map<String, SortedMap<Integer, Coord>> lineStrings, Map<String, SortedMap<String, String>> attributes){
		PolylineFeatureFactory factory = null;
		if(attributes == null){
			factory = initLineFeatureType(name, null);
		}else{
			for (SortedMap<String, String> m : attributes.values()){
				factory = initLineFeatureType(name, m);
				break;
			}
		}

		write(createDefaultLineStringFeature(lineStrings, attributes, factory), fileName);
	}

	public static void writeDefaultLineStrings2Shape(String fileName, String name, Map<String, List<Coord>> lineStrings){
		Map<String, SortedMap<Integer, Coord>> map = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer, Coord> ls;
		for(Entry<String, List<Coord>> e: lineStrings.entrySet()){
			ls = new TreeMap<Integer, Coord>();
			for(int i = 0; i< e.getValue().size(); i++){
				ls.put(i, e.getValue().get(i));
			}
			map.put(e.getKey(), ls);
		}
		writeDefaultLineString2Shape(fileName, name, map, null);
	}

	private static void write(Collection<SimpleFeature> features, String fileName){
		if(features.isEmpty()){
			log.error("can not write " + fileName + ", because featurelist is empty...");
		}else{
			ShapeFileWriter.writeGeometries(features, fileName); 
			log.info(fileName + " written!"); 
		}
	}

	private static PolylineFeatureFactory initLineFeatureType(String name, SortedMap<String, String> attributes) {
		PolylineFeatureFactory.Builder builder = new PolylineFeatureFactory.Builder();
		builder.setName(name);
		builder.addAttribute("name", String.class);
		
		if (!(attributes == null)) {
			for (String s : attributes.keySet()) {
				builder.addAttribute(s, String.class);
			}
		}
		
		return builder.create();
	}

	private static PointFeatureFactory initPointFeatureType(String name, SortedMap<String, String> attributes) {
		PointFeatureFactory.Builder builder = new PointFeatureFactory.Builder();
		builder.setName("nodes");
		builder.addAttribute("id", String.class);

		if(!(attributes == null)){
			for(String s : attributes.keySet()){
				builder.addAttribute(s, String.class);
			}
		}
		
		return builder.create();
	}

	private static Collection<SimpleFeature> createRouteFeatures(TransitSchedule schedule, Collection<Id> lines2write, Map<Id, SortedMap<String, String>> attributes, PolylineFeatureFactory factory){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coords;

		for (TransitLine line : schedule.getTransitLines().values()){
			if( (lines2write == null) || lines2write.contains(line.getId())){
				for(TransitRoute route : line.getRoutes().values()){
					coords = new Coordinate[route.getStops().size()];
					int i = 0;
					for(TransitRouteStop stop : route.getStops()){
						coords[i] = MGC.coord2Coordinate(stop.getStopFacility().getCoord());
						i++;
					}
					if(attributes == null){
						feature = getLineStringFeature(coords, line.getId().toString() + "_" + route.getId().toString(), null, factory);
					}else{
						feature = getLineStringFeature(coords, line.getId().toString() + "_" + route.getId().toString(), attributes.get(line.getId()), factory);
					}
					features.add(feature);
				}
			}
		}
		return features;
	}

	private static Collection<SimpleFeature> createLinkFeatures(Map<Id<Link>, Link> links, Map<Id, SortedMap<String, String>> attributes, PolylineFeatureFactory factory) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coords;

		for(Link l : links.values()){
			coords = new Coordinate[2];
			coords[0] = MGC.coord2Coordinate(l.getFromNode().getCoord());
			coords[1] = MGC.coord2Coordinate(l.getToNode().getCoord());
			if(attributes == null){
				feature = getLineStringFeature(coords, l.getId().toString(), null, factory);
			}else{
				feature = getLineStringFeature(coords, l.getId().toString(), attributes.get(l.getId()), factory);
			}
			features.add(feature);
		}
		return features;
	}

	private static Collection<SimpleFeature> createPointDistanceFeatures(Map<String, Tuple<Coord, Coord>> points, Map<String, SortedMap<String, String>> attributes, PolylineFeatureFactory factory){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coords;

		for(Entry<String, Tuple<Coord, Coord>> e : points.entrySet()) {
			coords = new Coordinate[2];
			coords[0] = MGC.coord2Coordinate(e.getValue().getFirst());
			coords[1] = MGC.coord2Coordinate(e.getValue().getSecond());
			feature = getLineStringFeature(coords, e.getKey(), attributes.get(e.getKey()), factory);
			features.add(feature);
		}

		return features;
	}

	private static Collection<SimpleFeature> createStopFeatures(Map<Id, TransitStopFacility> stops, Collection<Id> stops2write, PointFeatureFactory factory) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;

		for(TransitStopFacility stop : stops.values()){
			if((stops2write == null) || stops2write.contains(stop.getId())){
				feature = getPointFeature(stop.getCoord(), stop.getId().toString(), null, factory);
				features.add(feature);
			}
		}
		return features;
	}

	private static Collection<SimpleFeature> createNodeFeatures(Map<Id, Node> nodes, PointFeatureFactory factory) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;

		for(Node n : nodes.values()){
			feature = getPointFeature(n.getCoord(), n.getId().toString(), null, factory);
			features.add(feature);
		}
		return features;
	}

	private static Collection<SimpleFeature> createDefaultPointFeature(Map<String, Coord> points, Map<String, SortedMap<String, String>> attributes, PointFeatureFactory factory){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;

		for(Entry<String, Coord> e: points.entrySet()){
			if(attributes == null){
				feature =  getPointFeature(e.getValue(), e.getKey(), null, factory);
			}else{
				feature =  getPointFeature(e.getValue(), e.getKey(), attributes.get(e.getKey()), factory);
			}
			features.add(feature);
		}

		return features;
	}

	private static Collection<SimpleFeature> createDefaultLineStringFeature(Map<String, SortedMap<Integer, Coord>> lineStrings,	Map<String, SortedMap<String, String>> attributes, PolylineFeatureFactory factory) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coords;

		for(Entry<String, SortedMap<Integer, Coord>> points : lineStrings.entrySet()){
			if (points.getValue().size()<2){
				log.error(points.getKey() + ": not enough points for a lineString. Need at least 2 points!");
			}else{
				int i = 0;
				coords = new Coordinate[points.getValue().size()];
				for(Coord p : points.getValue().values()){
					coords[i] = MGC.coord2Coordinate(p);
					i++;
				}
				if(attributes == null){
					feature = getLineStringFeature(coords, points.getKey(), null, factory);
				}else{
					feature = getLineStringFeature(coords, 
							points.getKey(), 
							attributes.get(points.getKey()),
							factory);
				}
				features.add(feature);

			}

		}


		return features;
	}

	private static SimpleFeature getLineStringFeature(Coordinate[] coords, String name, SortedMap<String, String> attributes, PolylineFeatureFactory factory) {
		Object [] attribs;
		if(attributes == null){
			attribs = new Object[1];
		}else{
			attribs = new Object[attributes.size()+1];
		}
		attribs[0] = name;
		int count = 1;

		if(!(attributes == null)){
			for(String str : attributes.values()){
				attribs[count] = str;
				count++;
			}
		}

		return factory.createPolyline(coords, attribs, name);
	}

	private static SimpleFeature getPointFeature(Coord coord, String id, SortedMap<String, String> attributes, PointFeatureFactory factory) {
		Object[] attribs;
		if (attributes == null) {
			attribs = new Object[1];
		} else {
			attribs = new Object[attributes.size()+1];
		}
		attribs[0] = id;
		int count = 1;

		if(!(attributes == null)){
			for(String str : attributes.values()){
				attribs[count] = str;
				count++;
			}
		}

		return factory.createPoint(MGC.coord2Coordinate(coord), attribs, id);
	}

}
