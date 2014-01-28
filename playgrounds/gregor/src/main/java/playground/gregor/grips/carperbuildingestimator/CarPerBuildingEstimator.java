/* *********************************************************************** *
 * project: org.matsim.*
 * CarPerBuildingEstimator.java
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

package playground.gregor.grips.carperbuildingestimator;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class CarPerBuildingEstimator {
	public static void main(String [] args) {
		String districts = args[0];
		String buildings = args[1];
		String output = args[2];
		ShapeFileReader r1 = new ShapeFileReader();
		r1.readFileAndInitialize(districts);
		ShapeFileReader r2 = new ShapeFileReader();
		r2.readFileAndInitialize(buildings);
		
		
		int toGo = r2.getFeatureSet().size();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Population");
		b.setCRS(r2.getCoordinateSystem());
		b.add("location", MultiPolygon.class);
		b.add("persons", Long.class);
		b.add("Privat_PKW", Double.class);
		SimpleFeatureType ft = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);
		
		Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		
		for (SimpleFeature bf : r2.getFeatureSet()) {
			SimpleFeature district = getDistrict(r1,bf);
			double dPkw = (Double)district.getAttribute("Privat_PKW");
			double persons = (Double)district.getAttribute("BevGes");
			double c = dPkw/persons;
			
			int p = (Integer) bf.getAttribute("persons");
			double cars = p*c;
			
			Object geo = bf.getDefaultGeometry();
			
			MultiPolygon mp;
			if (geo instanceof Polygon) {
				mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[] { (Polygon) geo });
			} else if (geo instanceof MultiPolygon) {
				mp = (MultiPolygon) geo;
			} else {
				throw new RuntimeException("unsupported geometry type" + geo);
			}
			SimpleFeature f = builder.buildFeature(null, new Object[] { mp, p,cars });
			fts.add(f);
			
			if (toGo-- % 100 == 0) {
				System.out.println("to go: " + toGo);
			}
		}
		ShapeFileWriter.writeGeometries(fts, output);
	}

	private static SimpleFeature getDistrict(ShapeFileReader r1,
			SimpleFeature bf) {
		Geometry geo = (Geometry) bf.getDefaultGeometry();
		for (SimpleFeature df : r1.getFeatureSet()) {
			Geometry district = (Geometry)df.getDefaultGeometry();
			if (district.contains(geo) ) {
				return df;
			}
		}
		
		for (SimpleFeature df : r1.getFeatureSet()) {
			Geometry district = (Geometry)df.getDefaultGeometry();
			if (!district.intersection(geo).isEmpty()) {
				return df;
			}
		}		
		return null;
	}

}
