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

package playground.vsp.andreas.utils.pt.transitSchedule2shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

/**
 * 
 * @author droeder
 *
 */
public class DaShapeWriter {

	private static final Logger log = Logger.getLogger(DaShapeWriter.class);
	private static SimpleFeatureBuilder builder;
	
	private static GeometryFactory geometryFactory = new GeometryFactory();
	
	public static void writeLinks2Shape(String fileName, Map<Id, ? extends Link> links, Map<Id, SortedMap<String, Object>> attributes, String targetCoordinateSystem){
		if(attributes == null){
			initLineFeatureType("links", null, true, targetCoordinateSystem);
		}else{
			for(SortedMap<String, Object> m : attributes.values()){
				initLineFeatureType("links", m, targetCoordinateSystem);
				break;
			}
		}

		write(createLinkFeatures(links, attributes), fileName);
	}
	
	public static void writeNodes2Shape(String fileName, Map<Id, ? extends Node> nodes, String targetCoordinateSystem){
		initPointFeatureType("nodes", null, targetCoordinateSystem);
		write(createNodeFeatures(nodes), fileName);
	}
	
	public static void writeDefaultPoints2Shape(String fileName, String name, Map<String, Coord> points, Map<String, SortedMap<String, Object>> attributes, String targetCoordinateSystem){
		if(attributes == null){
			initPointFeatureType(name, null, targetCoordinateSystem);
		}else{
			for (SortedMap<String, Object> m : attributes.values()){
				initPointFeatureType(name, m, targetCoordinateSystem);
				break;
			}
		}
		write(createDefaultPointFeature(points, attributes), fileName);
	}
	
	/**
	 * if lines2write == null all lines are written
	 * 
	 * @param shapeFileOutName
	 * @param transitSchedule
	 * @param lines2write
	 */
	public static void writeTransitLines2Shape(String shapeFileOutName, TransitSchedule transitSchedule, Collection<Id> lines2write, Map<Id, SortedMap<String, Object>> attributes, String targetCoordinateSystem){
		if(!(attributes == null) && (attributes.size() > 0)){
			for(SortedMap<String, Object> m : attributes.values()){
				initLineFeatureType("transitLines", m, targetCoordinateSystem);
				break;
			}
		}else{
			initLineFeatureType("transitLines", null, targetCoordinateSystem);
		}
		write(createRouteFeatures(transitSchedule, lines2write, attributes), shapeFileOutName);
	}
	
	/**
	 * if stops2write == null all stops are written
	 * 
	 * @param fileName
	 * @param stops
	 * @param stops2write
	 */
	public static void writeRouteStops2Shape(String fileName, Map<Id, TransitStopFacility> stops, Collection<Id> stops2write, String targetCoordinateSystem){
		initPointFeatureType("TransitRouteStops", null, targetCoordinateSystem);
		write(createStopFeatures(stops, stops2write), fileName);
	}
	
	
	public static void writePointDist2Shape (String fileName, Map<String, Tuple<Coord, Coord>> points, Map<String, SortedMap<String, Object>> attributes, String targetCoordinateSystem){
		for (SortedMap<String, Object> m : attributes.values()){
			initLineFeatureType("distance", m, targetCoordinateSystem);
			break;
		}
		
		write(createPointDistanceFeatures(points,attributes), fileName);
	}
	/**
	 * @param fileName
	 * @param name
	 * @param lineStrings
	 * @param attributes
	 */
	public static void writeDefaultLineString2Shape(String fileName, String name,  Map<String, SortedMap<Integer, Coord>> lineStrings, Map<String, SortedMap<String, Object>> attributes, String targetCoordinateSystem){
		
		if(attributes == null){
			initLineFeatureType(name, null, targetCoordinateSystem);
		}else{
			for (SortedMap<String, Object> m : attributes.values()){
				initLineFeatureType(name, m, targetCoordinateSystem);
				break;
			}
		}
		
		write(createDefaultLineStringFeature(lineStrings, attributes), fileName);
	}
	
	public static void writeDefaultLineStrings2Shape(String fileName, String name, Map<String, List<Coord>> lineStrings, String targetCoordinateSystem){
		Map<String, SortedMap<Integer, Coord>> map = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer, Coord> ls;
		for(Entry<String, List<Coord>> e: lineStrings.entrySet()){
			ls = new TreeMap<Integer, Coord>();
			for(int i = 0; i< e.getValue().size(); i++){
				ls.put(i, e.getValue().get(i));
			}
			map.put(e.getKey(), ls);
		}
		writeDefaultLineString2Shape(fileName, name, map, null, targetCoordinateSystem);
	}
	
	private static void write(Collection<SimpleFeature> features, String shapeFileOutName){
		if(features.isEmpty()){
			log.error("can not write " + shapeFileOutName + ", because featurelist is empty...");
		}else{
			ShapeFileWriter.writeGeometries(features, shapeFileOutName); 
			log.info(shapeFileOutName + " written!"); 
		}
	}
	
	private static void initLineFeatureType(String name, SortedMap<String, Object> attributes, String targetCoordinateSystem){
		initLineFeatureType(name, attributes, false, targetCoordinateSystem);
	}
	
	private static void initLineFeatureType(String name, SortedMap<String, Object> attributes, boolean links, String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(name);
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.add("location", LineString.class);
		b.add("name", String.class);
		if (!(attributes == null)) {
			for (Entry<String, Object> e: attributes.entrySet()) {
				b.add(e.getKey(), e.getValue().getClass());
			}
		} else {
			if (links) {
				b.add("capacity", Double.class);
				b.add("length", Double.class);
			}
		}

		builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private static void initPointFeatureType(String name, SortedMap<String, Object> attributes, String targetCoordinateSystem){
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(name);
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.add("location", Point.class);
		b.add("name", String.class);
		if (!(attributes == null)) {
			for (Entry<String, Object> e: attributes.entrySet()) {
				b.add(e.getKey(), e.getValue().getClass());
			}
		}
		
		builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private static Collection<SimpleFeature> createRouteFeatures(TransitSchedule schedule, Collection<Id> lines2write, Map<Id, SortedMap<String, Object>> attributes){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coord;
		
		for (TransitLine line : schedule.getTransitLines().values()){
			if( (lines2write == null) || lines2write.contains(line.getId())){
				for(TransitRoute route : line.getRoutes().values()){
					coord = new Coordinate[route.getStops().size()];
					int i = 0;
					for(TransitRouteStop stop : route.getStops()){
						coord[i] = MGC.coord2Coordinate(stop.getStopFacility().getCoord());
						i++;
					}
					if(attributes == null){
						feature = getLineStringFeature(new CoordinateArraySequence(coord), line.getId().toString() + "_" + route.getId().toString(), null);
					}else{
						feature = getLineStringFeature(new CoordinateArraySequence(coord), line.getId().toString() + "_" + route.getId().toString(), attributes.get(line.getId()));
					}
					features.add(feature);
				}
			}
		}
		return features;
	}
	
	private static Collection<SimpleFeature> createLinkFeatures(Map<Id, ? extends Link> links, Map<Id, SortedMap<String, Object>> attributes) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coord;
		SortedMap<String, Object> attr;
		for(Link l : links.values()){
			coord = new Coordinate[2];
			coord[0] = MGC.coord2Coordinate(l.getFromNode().getCoord());
			coord[1] = MGC.coord2Coordinate(l.getToNode().getCoord());
			if(attributes == null){
				attr = new TreeMap<String, Object>();
				attr.put("capacity", l.getCapacity());
				attr.put("length", l.getLength());
				feature = getLineStringFeature(new CoordinateArraySequence(coord), l.getId().toString(), attr);
			}else{
				feature = getLineStringFeature(new CoordinateArraySequence(coord), l.getId().toString(), attributes.get(l.getId()));
			}
			features.add(feature);
		}
		return features;
	}
	
	private static Collection<SimpleFeature> createPointDistanceFeatures(Map<String, Tuple<Coord, Coord>> points, Map<String, SortedMap<String, Object>> attributes){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coord;
		
		for(Entry<String, Tuple<Coord, Coord>> e : points.entrySet()) {
			coord = new Coordinate[2];
			coord[0] = MGC.coord2Coordinate(e.getValue().getFirst());
			coord[1] = MGC.coord2Coordinate(e.getValue().getSecond());
			feature = getLineStringFeature(new CoordinateArraySequence(coord), e.getKey(), attributes.get(e.getKey()));
			features.add(feature);
		}
		
		return features;
	}
	
	private static Collection<SimpleFeature> createStopFeatures(Map<Id, TransitStopFacility> stops, Collection<Id> stops2write){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		
		for(TransitStopFacility stop : stops.values()){
			if((stops2write == null) || stops2write.contains(stop.getId())){
				feature = getPointFeature(stop.getCoord(), stop.getId().toString(), null);
				features.add(feature);
			}
		}
		return features;
	}
	
	private static Collection<SimpleFeature> createNodeFeatures(Map<Id, ? extends Node> nodes){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		
		for(Node n : nodes.values()){
			feature = getPointFeature(n.getCoord(), n.getId().toString(), null);
			features.add(feature);
		}
		return features;
	}
	
	private static Collection<SimpleFeature> createDefaultPointFeature(Map<String, Coord> points, Map<String, SortedMap<String, Object>> attributes){
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		
		for(Entry<String, Coord> e: points.entrySet()){
			if(attributes == null){
				feature =  getPointFeature(e.getValue(), e.getKey(), null);
			}else{
				feature =  getPointFeature(e.getValue(), e.getKey(), attributes.get(e.getKey()));
			}
			features.add(feature);
		}
		
		return features;
	}
	
	private static Collection<SimpleFeature> createDefaultLineStringFeature(Map<String, SortedMap<Integer, Coord>> lineStrings,	Map<String, SortedMap<String, Object>> attributes) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		SimpleFeature feature;
		Coordinate[] coord;
		
		for(Entry<String, SortedMap<Integer, Coord>> points : lineStrings.entrySet()){
			if (points.getValue().size()<2){
				log.error(points.getKey() + ": not enough points for a lineString. Need at least 2 points!");
			}else{
				int i = 0;
				coord = new Coordinate[points.getValue().size()];
				for(Coord p : points.getValue().values()){
					coord[i] = MGC.coord2Coordinate(p);
					i++;
				}
				if(attributes == null){
					feature = getLineStringFeature(new CoordinateArraySequence(coord), points.getKey(), null);
				}else{
					feature = getLineStringFeature(new CoordinateArraySequence(coord), 
							points.getKey(), 
							attributes.get(points.getKey()));
				}
				features.add(feature);
				
			}
			
		}
		
		
		return features;
	}
	
	private static SimpleFeature getLineStringFeature(CoordinateArraySequence c, String name, SortedMap<String, Object> attributes) {
		LineString s = geometryFactory.createLineString(c);
		Object[] attribs ;
		if(attributes == null){
			attribs = new Object[2];
		}else{
			attribs = new Object[attributes.size()+2];
		}
		attribs[0] = s;
		attribs[1] = name;
		Integer count = 2;
		
		if(!(attributes == null)){
			for(Object o : attributes.values()){
				attribs[count] = o;
				count++;
			}
		}
		
		try {
			return builder.buildFeature(name, attribs);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static SimpleFeature getPointFeature(Coord coord, String id, SortedMap<String, Object> attributes) {
		Point p = geometryFactory.createPoint(MGC.coord2Coordinate(coord));
		Object [] attribs ;
		if(attributes == null){
			attribs = new Object[2];
		}else{
			attribs = new Object[attributes.size()+2];
		}
		attribs[0] = p;
		attribs[1] = id;
		Integer count = 2;
		
		if(!(attributes == null)){
			for(Object o : attributes.values()){
				attribs[count] = o;
				count++;
			}
		}

		try {
			return builder.buildFeature(id, attribs);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}
	
}
