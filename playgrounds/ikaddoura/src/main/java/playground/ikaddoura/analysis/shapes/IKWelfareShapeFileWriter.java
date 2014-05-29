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
public class IKWelfareShapeFileWriter {
		
	private SimpleFeatureBuilder initFeatureType() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		b.setName("multiPolygon");
		b.add("location", MultiPolygon.class);
		b.add("Id", String.class);
		b.add("Act_home", Integer.class);
		b.add("Act_work", Integer.class);
		b.add("Act_all", Integer.class);
		b.add("tolls", Double.class);
		b.add("userBenef.", Double.class);
		b.add("welfare", Double.class);
		b.add("avgTolls", Double.class);
		b.add("avgUserBe.", Double.class);
		b.add("avgWelfare", Double.class);

		
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}

	public void writeShapeFileGeometry(Map<Integer, Geometry> zoneId2geometry,
			Map<Integer, Integer> zoneNr2homeActivities,
			Map<Integer, Integer> zoneNr2workActivities,
			Map<Integer, Integer> zoneNr2activities,
			Map<Integer, Double> zoneNr2tollPayments,
			Map<Integer, Double> zoneNr2userBenefits,
			Map<Integer, Double> zoneNr2welfareContribution,
			Map<Integer, Double> zoneNr2AvgTollPayments,
			Map<Integer, Double> zoneNr2AvgUserBenefits,
			Map<Integer, Double> zoneNr2AvgWelfareContribution,
			String outputFile) {

		SimpleFeatureBuilder factory = initFeatureType();
		Set<SimpleFeature> features = createFeatures(zoneId2geometry,
				zoneNr2homeActivities,
				zoneNr2workActivities,
				zoneNr2activities,
				zoneNr2tollPayments,
				zoneNr2userBenefits,
				zoneNr2welfareContribution,
				zoneNr2AvgTollPayments,
				zoneNr2AvgUserBenefits,
				zoneNr2AvgWelfareContribution,
				factory);
		ShapeFileWriter.writeGeometries(features, outputFile);
		System.out.println("ShapeFile " + outputFile + " written.");	
	}

	private Set<SimpleFeature> createFeatures(
			Map<Integer, Geometry> zoneId2geometry,
			Map<Integer, Integer> zoneNr2homeActivities,
			Map<Integer, Integer> zoneNr2workActivities,
			Map<Integer, Integer> zoneNr2activities,
			Map<Integer, Double> zoneNr2tollPayments,
			Map<Integer, Double> zoneNr2userBenefits,
			Map<Integer, Double> zoneNr2welfareContribution,
			Map<Integer, Double> zoneNr2tollAvgTollPayments,
			Map<Integer, Double> zoneNr2AvgUserBenefits,
			Map<Integer, Double> zoneNr2AvgWelfareContribution,
			SimpleFeatureBuilder factory) {
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		for (Integer nr : zoneId2geometry.keySet()){
			features.add(getFeature(nr, zoneId2geometry.get(nr), zoneNr2homeActivities, zoneNr2workActivities, zoneNr2activities, zoneNr2tollPayments, zoneNr2userBenefits, zoneNr2welfareContribution, zoneNr2tollAvgTollPayments, zoneNr2AvgUserBenefits, zoneNr2AvgWelfareContribution, factory));
		}
		return (HashSet<SimpleFeature>) features;
		
	}

	private SimpleFeature getFeature(Integer nr, Geometry geometry,
			Map<Integer, Integer> zoneNr2homeActivities,
			Map<Integer, Integer> zoneNr2workActivities,
			Map<Integer, Integer> zoneNr2activities,
			Map<Integer, Double> zoneNr2tollPayments,
			Map<Integer, Double> zoneNr2userBenefits,
			Map<Integer, Double> zoneNr2welfareContribution,
			Map<Integer, Double> zoneNr2tollAvgTollPayments,
			Map<Integer, Double> zoneNr2AvgUserBenefits,
			Map<Integer, Double> zoneNr2AvgWelfareContribution,
			SimpleFeatureBuilder factory) {

		GeometryFactory geometryFactory = new GeometryFactory();
		MultiPolygon g = (MultiPolygon) geometryFactory.createGeometry(geometry);
		
		Object [] attribs = new Object[11];
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
		
		if (zoneNr2tollPayments.containsKey(nr)){
			attribs[5] = zoneNr2tollPayments.get(nr);
		} else {
			attribs[5] = 0;
		}
		
		if (zoneNr2userBenefits.containsKey(nr)){
			attribs[6] = zoneNr2userBenefits.get(nr);
		} else {
			attribs[6] = 0;
		}
		
		if (zoneNr2welfareContribution.containsKey(nr)){
			attribs[7] = zoneNr2welfareContribution.get(nr);
		} else {
			attribs[7] = 0;
		}
		
		if (zoneNr2tollAvgTollPayments.containsKey(nr)){
			attribs[8] = zoneNr2tollAvgTollPayments.get(nr);
		} else {
			attribs[8] = 0;
		}
		
		if (zoneNr2AvgUserBenefits.containsKey(nr)){
			attribs[9] = zoneNr2AvgUserBenefits.get(nr);
		} else {
			attribs[9] = 0;
		}
		
		if (zoneNr2AvgWelfareContribution.containsKey(nr)){
			attribs[10] = zoneNr2AvgWelfareContribution.get(nr);
		} else {
			attribs[10] = 0;
		}
		return factory.buildFeature(null, attribs);
	}

}
