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
		
	private SimpleFeatureBuilder initFeatureType() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		b.setName("multiPolygon");
		b.add("location", MultiPolygon.class);
		b.add("Id", String.class);
		b.add("Act_home", Integer.class);
		b.add("Act_work", Integer.class);
		b.add("Act_all", Integer.class);
		b.add("caused", Double.class);
		b.add("avgCaused", Double.class);
		b.add("affected", Double.class);
		b.add("avgAffect.", Double.class);
		
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}

	public void writeShapeFileGeometry(Map<Integer, Geometry> zoneId2geometry,
			Map<Integer, Integer> zoneNr2homeActivities,
			Map<Integer, Integer> zoneNr2workActivities,
			Map<Integer, Integer> zoneNr2activities,
			Map<Integer, Double> zoneNr2tollPaymentsCaused,
			Map<Integer, Double> zoneNr2AvgTollPaymentsCaused,
			Map<Integer, Double> zoneNr2tollPaymentsAffected,
			Map<Integer, Double> zoneNr2AvgTollPaymentsAffected,
			String outputFile) {

		SimpleFeatureBuilder factory = initFeatureType();
		Set<SimpleFeature> features = createFeatures(zoneId2geometry,
				zoneNr2homeActivities,
				zoneNr2workActivities,
				zoneNr2activities,
				zoneNr2tollPaymentsCaused,
				zoneNr2AvgTollPaymentsCaused,
				zoneNr2tollPaymentsAffected,
				zoneNr2AvgTollPaymentsAffected,
				factory);
		ShapeFileWriter.writeGeometries(features, outputFile);
		System.out.println("ShapeFile " + outputFile + " written.");	
	}

	private Set<SimpleFeature> createFeatures(
			Map<Integer, Geometry> zoneId2geometry,
			Map<Integer, Integer> zoneNr2homeActivities,
			Map<Integer, Integer> zoneNr2workActivities,
			Map<Integer, Integer> zoneNr2activities,
			Map<Integer, Double> zoneNr2tollPaymentsCaused,
			Map<Integer, Double> zoneNr2AvgTollPaymentsCaused,
			Map<Integer, Double> zoneNr2tollPaymentsAffected,
			Map<Integer, Double> zoneNr2AvgTollPaymentsAffected,
			SimpleFeatureBuilder factory) {
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		for (Integer nr : zoneId2geometry.keySet()){
			features.add(getFeature(nr, zoneId2geometry.get(nr), zoneNr2homeActivities, zoneNr2workActivities, zoneNr2activities, zoneNr2tollPaymentsCaused, zoneNr2AvgTollPaymentsCaused, zoneNr2tollPaymentsAffected, zoneNr2AvgTollPaymentsAffected, factory));
		}
		return (HashSet<SimpleFeature>) features;
		
	}

	private SimpleFeature getFeature(Integer nr, Geometry geometry,
			Map<Integer, Integer> zoneNr2homeActivities,
			Map<Integer, Integer> zoneNr2workActivities,
			Map<Integer, Integer> zoneNr2activities,
			Map<Integer, Double> zoneNr2tollPaymentsCaused,
			Map<Integer, Double> zoneNr2AvgTollPaymentsCaused,
			Map<Integer, Double> zoneNr2tollPaymentsAffected,
			Map<Integer, Double> zoneNr2AvgTollPaymentsAffected,
			SimpleFeatureBuilder factory) {

		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPolygon g = (MultiPolygon) geometryFactory.createGeometry(geometry);
		
		Object [] attribs = new Object[9];
		attribs[0] = g;
		attribs[1] = String.valueOf(nr);
		
		if (zoneNr2homeActivities.containsKey(nr)){
			attribs[2] = zoneNr2homeActivities.get(nr);
		} else {
			attribs[2] = 0;
		}
		
		if (zoneNr2workActivities.containsKey(nr)){
			attribs[3] = zoneNr2workActivities.get(nr);
		} else {
			attribs[3] = 0;
		}
		
		if (zoneNr2activities.containsKey(nr)){
			attribs[4] = zoneNr2activities.get(nr);
		} else {
			attribs[4] = 0;
		}
		
		if (zoneNr2tollPaymentsCaused.containsKey(nr)){
			attribs[5] = zoneNr2tollPaymentsCaused.get(nr);
		} else {
			attribs[5] = 0;
		}
		
		if (zoneNr2AvgTollPaymentsCaused.containsKey(nr)){
			attribs[6] = zoneNr2AvgTollPaymentsCaused.get(nr);
		} else {
			attribs[6] = 0;
		}
		
		if (zoneNr2tollPaymentsAffected.containsKey(nr)){
			attribs[7] = zoneNr2tollPaymentsAffected.get(nr);
		} else {
			attribs[7] = 0;
		}
		
		if (zoneNr2AvgTollPaymentsAffected.containsKey(nr)){
			attribs[8] = zoneNr2AvgTollPaymentsAffected.get(nr);
		} else {
			attribs[8] = 0;
		}
		
		return factory.buildFeature(null, attribs);
	}

}
