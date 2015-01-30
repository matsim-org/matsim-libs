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

package playground.johannes.gsv.zones.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;

import playground.johannes.gsv.zones.Zone;

/**
 * @author johannes
 * 
 */
public class Zone2GeoJSON {

	public static String toJson(Zone zone) {
		GeoJSONWriter writer = new GeoJSONWriter();
		GeoJSON json = writer.write(zone.getGeometry());

		Geometry geometry = (Geometry) GeoJSONFactory.create(json.toString());
		Feature feature = new Feature(geometry, new HashMap<String, Object>(zone.attributes()));

		List<Feature> features = new ArrayList<>(1);
		features.add(feature);
		return writer.write(features).toString();
	}

	public static String toJson(Collection<Zone> zones) {
		GeoJSONWriter writer = new GeoJSONWriter();
		List<Feature> features = new ArrayList<>(1);

		for (Zone zone : zones) {
			GeoJSON json = writer.write(zone.getGeometry());
			Geometry geometry = (Geometry) GeoJSONFactory.create(json.toString());
			Feature feature = new Feature(geometry, new HashMap<String, Object>(zone.attributes()));
			features.add(feature);
		}

		return writer.write(features).toString();
	}

	public static Set<Zone> parseFeatureCollection(String data) {

		GeoJSON json = GeoJSONFactory.create(data);

		if (json instanceof FeatureCollection) {
			GeoJSONReader reader = new GeoJSONReader();
			Set<Zone> zones = new HashSet<>();
			FeatureCollection features = (FeatureCollection) json;
			for (Feature feature : features.getFeatures()) {
				Zone zone = new Zone(reader.read(feature.getGeometry()));
//				System.out.println(zone.getGeometry().toString());
				for (Entry<String, Object> prop : feature.getProperties().entrySet()) {
					Object value = prop.getValue();
					if(value == null) {
						zone.setAttribute(prop.getKey(), "");
					} else {
						zone.setAttribute(prop.getKey(), prop.getValue().toString());
					}
				}
//				System.out.println(zone.attributes().toString());
//				System.exit(0);
				zones.add(zone);
			}
			return zones;
		} else {
			return null;
		}

	}
}
