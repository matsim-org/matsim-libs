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

package playground.ikaddoura.analysis.shapes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * @author ikaddoura
 *
 */
public class IKShapeFileWriter {
		
	public void writeShapeFileGeometry(Map<Integer, Geometry> zoneNr2zoneGeometry, Map<Integer, Integer> zoneNr2homeActivities, String outputFile) {
		SimpleFeatureBuilder factory = initFeatureType();
		Set<SimpleFeature> features = createFeatures(zoneNr2zoneGeometry, zoneNr2homeActivities, factory);
		ShapeFileWriter.writeGeometries(features, outputFile);
		System.out.println("ShapeFile " + outputFile + " written.");	
	}
		
	private SimpleFeatureBuilder initFeatureType() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		b.setName("multiPolygon");
		b.add("location", MultiPolygon.class);
		b.add("Id", String.class);
		b.add("Act_home", Integer.class);
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}

	private HashSet<SimpleFeature> createFeatures(Map<Integer, Geometry> zoneNr2zoneGeometry, Map<Integer, Integer> zoneNr2homeActivities, SimpleFeatureBuilder factory) {
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		for (Integer nr : zoneNr2zoneGeometry.keySet()){
			features.add(getFeature(nr, zoneNr2zoneGeometry.get(nr), zoneNr2homeActivities, factory));
		}
		return (HashSet<SimpleFeature>) features;
	}
	
	private SimpleFeature getFeature(Integer nr, Geometry geometry, Map<Integer, Integer> zoneNr2homeActivities, SimpleFeatureBuilder factory) {
		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPolygon g = (MultiPolygon) geometryFactory.createGeometry(geometry);
		
		Object [] attribs = new Object[3];
		attribs[0] = g;
		attribs[1] = String.valueOf(nr);
		
		if (zoneNr2homeActivities.containsKey(nr)){
			attribs[2] = zoneNr2homeActivities.get(nr);
		} else {
			attribs[2] = 0;
		}
		
		return factory.buildFeature(null, attribs);
	}

}
