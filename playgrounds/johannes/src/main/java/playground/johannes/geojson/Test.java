/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.geojson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class Test {

	/**
	 * @param args
	 * @throws JsonProcessingException
	 */
	public static void main(String[] args) throws JsonProcessingException {
		GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		
		Point p = factory.createPoint(new Coordinate(0, 0));
//		
//		String str = new GeoJson().toJson(p);
//		
//		System.out.println(str);
//
//		FeatureCollection featureCollection = new FeatureCollection();
//		Feature feature = new Feature();
//		org.geojson.Point jsonPoint = new org.geojson.Point();
//		jsonPoint.setCoordinates(new LngLatAlt(0, 0));
//		feature.setGeometry(jsonPoint);
//		feature.setProperty("key", 123);
//		featureCollection.add(feature);
//
//		String json= new ObjectMapper().writeValueAsString(featureCollection);
//		
//		System.out.println(json);
		
		GeoJSONWriter writer = new GeoJSONWriter();
		Geometry jsonGeometry = writer.write(p);
		
		List<Feature> features = new ArrayList<>();
		Map<String, Object> properties = new HashMap<>();
		properties.put("aKey", 123);
		features.add(new Feature(jsonGeometry, properties));
		
		FeatureCollection collection = writer.write(features);
		System.out.println(collection.toString());
	}
}
