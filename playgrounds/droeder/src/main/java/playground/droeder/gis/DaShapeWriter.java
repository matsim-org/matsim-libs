package playground.droeder.gis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class DaShapeWriter {

	private static final Logger log = Logger
	.getLogger(DaShapeWriter.class);
	private static FeatureType featureType;
	
	private static GeometryFactory geometryFactory = new GeometryFactory();
	
	public static void writeLinks2Shape(String fileName, Map<Id, Link> links, Map<Id, SortedMap<String, String>> attributes){
		if(attributes == null){
			initLineFeatureType("links", null);
		}else{
			for(SortedMap<String, String> m : attributes.values()){
				initLineFeatureType("links", m);
				break;
			}
		}

		write(createLinkFeatures(links, attributes), fileName);
	}
	
	public static void writeNodes2Shape(String fileName, Map<Id, Node> nodes){
		initPointFeatureType("nodes");
		write(createNodeFeatures(nodes), fileName);
	}
	
	/**
	 * if lines2write == null all lines are written
	 * 
	 * @param fileName
	 * @param schedule
	 * @param lines2write
	 */
	public static void writeTransitLines2Shape(String fileName, TransitSchedule schedule, Collection<Id> lines2write, Map<Id, SortedMap<String, String>> attributes){
		for(SortedMap<String, String> m : attributes.values()){
			initLineFeatureType("transitLines", m);
			break;
		}
		write(createRouteFeatures(schedule, lines2write, attributes), fileName);
	}
	
	/**
	 * if stops2write == null all stops are written
	 * 
	 * @param fileName
	 * @param stops
	 * @param stops2write
	 */
	public static void writeRouteStops2Shape(String fileName, Map<Id, TransitStopFacility> stops, Collection<Id> stops2write){
		initPointFeatureType("TransitRouteStops");
		write(createStopFeatures(stops, stops2write), fileName);
	}
	
	
	public static void writePointDist2Shape (String fileName, Map<String, Tuple<Coord, Coord>> points, Map<String, SortedMap<String, String>> attributes){
		for (SortedMap<String, String> m : attributes.values()){
			initLineFeatureType("distance", m);
			break;
		}
		
		write(createPointDistanceFeatures(points,attributes), fileName);
	}
	
	public static void writeDefaultLineString2Shape(String fileName, String name,  Map<String, SortedMap<Integer, Coord>> lineStrings, Map<String, SortedMap<String, String>> attributes){
		
		if(attributes == null){
			initLineFeatureType(name, null);
		}else{
			for (SortedMap<String, String> m : attributes.values()){
				initLineFeatureType(name, m);
				break;
			}
		}
		
		write(createDefaultLineStringFeature(lineStrings, attributes), fileName);
	}
	
	private static void write(Collection<Feature> features, String fileName){
		try {
			ShapeFileWriter.writeGeometries(features, fileName); 
			log.info(fileName + " written!");
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private static void initLineFeatureType(String name, SortedMap<String, String> attributes) {
		AttributeType [] attribs;
		if(attributes == null){
			attribs = new AttributeType[2];
		}else{
			attribs = new AttributeType[attributes.size() + 2];
		}
		
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("name", String.class);
		Integer count = 2;
		
		if(!(attributes == null)){
			for(String s : attributes.keySet()){
				attribs[count] = AttributeTypeFactory.newAttributeType(s, String.class);
				count++;
			}
		}
		
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, name);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private static void initPointFeatureType(String name){
		AttributeType [] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("id", String.class);
		
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, name);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private static Collection<Feature> createRouteFeatures(TransitSchedule schedule, Collection<Id> lines2write, Map<Id, SortedMap<String, String>> attributes){
		Collection<Feature> features = new ArrayList<Feature>();
		Feature feature;
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
					feature = getLineStringFeature(new CoordinateArraySequence(coord), line.getId().toString(), attributes.get(line.getId()));
					features.add(feature);
				}
			}
		}
		return features;
	}
	
	private static Collection<Feature> createLinkFeatures(Map<Id, Link> links, Map<Id, SortedMap<String, String>> attributes) {
		Collection<Feature> features = new ArrayList<Feature>();
		Feature feature;
		Coordinate[] coord;
		
		for(Link l : links.values()){
			coord = new Coordinate[2];
			coord[0] = MGC.coord2Coordinate(l.getFromNode().getCoord());
			coord[1] = MGC.coord2Coordinate(l.getToNode().getCoord());
			if(attributes == null){
				feature = getLineStringFeature(new CoordinateArraySequence(coord), l.getId().toString(), null);
			}else{
				feature = getLineStringFeature(new CoordinateArraySequence(coord), l.getId().toString(), attributes.get(l.getId()));
			}
			features.add(feature);
		}
		return features;
	}
	
	private static Collection<Feature> createPointDistanceFeatures(Map<String, Tuple<Coord, Coord>> points, Map<String, SortedMap<String, String>> attributes){
		Collection<Feature> features = new ArrayList<Feature>();
		Feature feature;
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
	
	private static Collection<Feature> createStopFeatures(Map<Id, TransitStopFacility> stops, Collection<Id> stops2write){
		Collection<Feature> features = new ArrayList<Feature>();
		Feature feature;
		
		for(TransitStopFacility stop : stops.values()){
			if((stops2write == null) || stops2write.contains(stop.getId())){
				feature = getPointFeature(stop.getCoord(), stop.getId().toString());
				features.add(feature);
			}
		}
		return features;
	}
	
	private static Collection<Feature> createNodeFeatures(Map<Id, Node> nodes){
		Collection<Feature> features = new ArrayList<Feature>();
		Feature feature;
		
		for(Node n : nodes.values()){
			feature = getPointFeature(n.getCoord(), n.getId().toString());
			features.add(feature);
		}
		return features;
	}
	
	private static Collection<Feature> createDefaultLineStringFeature(Map<String, SortedMap<Integer, Coord>> lineStrings,	Map<String, SortedMap<String, String>> attributes) {
		Collection<Feature> features = new ArrayList<Feature>();
		Feature feature;
		Coordinate[] coord;
		
		for(Entry<String, SortedMap<Integer, Coord>> points : lineStrings.entrySet()){
			if (points.getValue().size()<2){
				log.error("not enough points for a lineString. Need at least 2 points!");
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
					feature = getLineStringFeature(new CoordinateArraySequence(coord), points.getKey(), attributes.get(points.getKey()));
				}
				features.add(feature);
				
			}
			
		}
		
		
		return features;
	}
	
	private static Feature getLineStringFeature(CoordinateArraySequence c, String name, SortedMap<String, String> attributes) {
		LineString s = geometryFactory.createLineString(c);
		Object [] attribs ;
		if(attributes == null){
			attribs = new Object[2];
		}else{
			attribs = new Object[attributes.size()+2];
		}
		attribs[0] = s;
		attribs[1] = name;
		Integer count = 2;
		
		if(!(attributes == null)){
			for(String str : attributes.values()){
				attribs[count] = str;
				count++;
			}
		}
		
		try {
			return featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Feature getPointFeature(Coord coord, String id) {
		Point p = geometryFactory.createPoint(MGC.coord2Coordinate(coord));
		Object [] attribs = new Object[2];
		attribs[0] = p;
		attribs[1] = id;
		
		try {
			return featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
	}
}
