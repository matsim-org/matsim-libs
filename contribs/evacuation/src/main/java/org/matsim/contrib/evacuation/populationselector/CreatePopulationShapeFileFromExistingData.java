/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePopulationShapeFileFromExistingData.java
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

package org.matsim.contrib.evacuation.populationselector;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.contrib.evacuation.io.EvacuationConfigReader;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class CreatePopulationShapeFileFromExistingData {
	
	public static void main(String args[]) {
		String existingDataFile = args[0];
		String evacuationConfig = args[1];
		ShapeFileReader r1 = new ShapeFileReader();
		r1.readFileAndInitialize(existingDataFile);
		EvacuationConfigModule gcm = new EvacuationConfigModule("evacuation");
		EvacuationConfigReader gcd = new EvacuationConfigReader(gcm);//,false);
		gcd.parse(evacuationConfig);
		
		ShapeFileReader r2 = new ShapeFileReader();
		r2.readFileAndInitialize(gcm.getEvacuationAreaFileName());
		
		transformCRS(r1,r2); //only needed if shape files have different coordinate systems; r2 usually is in WGS84 while r1 is projected. So, we transform r2 to r1
		
		if (r2.getFeatureCollection().size() != 1) {
			throw new RuntimeException("The evacuation area must comprise of exactly one feature!");
		}
		Object o = r2.getFeatureSet().iterator().next().getDefaultGeometry();
		Polygon p;
		if (o instanceof Polygon) {
			p = (Polygon) o;
		} else if (o instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon) o;
			if (mp.getNumGeometries() != 1) {
				throw new RuntimeException("The evacuation area must comprise of exactly one polygon!");
			}
			p = (Polygon) mp.getGeometryN(0);
		} else {
			throw new RuntimeException("No usable geometry found!");
		}
		
		
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(r1.getCoordinateSystem());
		b.add("location", MultiPolygon.class);
		b.add("persons", Long.class);
		SimpleFeatureType ft = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);
		
		Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		for (SimpleFeature d : r1.getFeatureSet()) {
			Geometry geo = (Geometry) d.getDefaultGeometry();
			Geometry intersection = p.intersection(geo);
			if (!intersection.isEmpty()) {
				System.out.println(intersection.getArea() + "  " + geo.getArea());
				double popCoeff = intersection.getArea()/geo.getArea();
				int pop;
				if (gcm.getMainTrafficType().equals("vehicular")) {
					pop = (int) (((Double)d.getAttribute("Privat_PKW"))*popCoeff+.5);
					System.out.println(pop + "  " + d.getAttribute("Privat_PKW")) ;
				} else if (gcm.getMainTrafficType().equals("pedestrian")) {
					pop = (int) (((Long)d.getAttribute("persons"))*popCoeff+.5);
					System.out.println(pop + "  " + d.getAttribute("persons")) ;
				} else {
					throw new RuntimeException("unsupport main transport mode:" + gcm.getMainTrafficType());
				}
				MultiPolygon mp;
				if (intersection instanceof Polygon) {
					mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[] { (Polygon) intersection });
				} else if (intersection instanceof MultiPolygon) {
					mp = (MultiPolygon) intersection;
				} else {
					throw new RuntimeException("unsupported geometry type" + intersection.getGeometryType());
				}
				SimpleFeature f = builder.buildFeature(null, new Object[] { mp, pop });
				fts.add(f);
			} else {
				System.out.println("empty");
			}
		}
		ShapeFileWriter.writeGeometries(fts, gcm.getPopulationFileName());
	}
	
    public static void transformCRS(ShapeFileReader r1) {
        CoordinateReferenceSystem target = MGC.getCRS("EPSG: 4326");
        try {
                MathTransform t = CRS.findMathTransform(r1.getCoordinateSystem(), target);
                for (SimpleFeature f : r1.getFeatureSet()) {
                        Geometry geo = (Geometry) f.getDefaultGeometry();
                        Geometry gg = JTS.transform(geo, t);
                        f.setDefaultGeometry(gg);
                        
                }
//                r1.getFeatureSource().getC
        } catch (FactoryException e) {
                e.printStackTrace();
        } catch (MismatchedDimensionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        } catch (TransformException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
}

	public static void transformCRS(ShapeFileReader r2, ShapeFileReader r1) {
		try {
			MathTransform t = CRS.findMathTransform(r1.getCoordinateSystem(), r2.getCoordinateSystem());
			for (SimpleFeature f : r1.getFeatureSet()) {
				Geometry geo = (Geometry) f.getDefaultGeometry();
				Geometry gg = JTS.transform(geo, t);
				f.setDefaultGeometry(gg);
				
			}
//			r1.getFeatureSource().getC
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
