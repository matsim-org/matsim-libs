/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureCoordinateTransformer.java
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

package playground.gregor.grips.geospatial;

import java.util.Collection;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

public class FeatureCoordinateTransformer {
	
	


	private final MathTransform transform;

	public FeatureCoordinateTransformer(CoordinateReferenceSystem src, CoordinateReferenceSystem target) {
		FactoryException ex;
		try {
			this.transform = CRS.findMathTransform(src, target,true);
			return;
		} catch (FactoryException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}
	
	public void transform(SimpleFeature f) {
		Exception ex;
		try {
			Geometry trr = JTS.transform((Geometry) f.getDefaultGeometry(), this.transform);
			f.setDefaultGeometry(trr);
			return;
		} catch (TransformException e) {
			ex = e; 
		}
		throw new RuntimeException(ex);
	}
	
	public void transform(Collection<SimpleFeature> fts) {
		for (SimpleFeature ft : fts) {
			transform(ft);
		}
	}

	public static void main(String [] args) throws NoSuchAuthorityCodeException, FactoryException {
		String input = "some_input_file.shp";
		String output = "some_output_file.shp";
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(input);
		CoordinateReferenceSystem sourceCRS = reader.getCoordinateSystem();
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3395"); // World Mercator coordinate system change this if needed!
		FeatureCoordinateTransformer transform = new FeatureCoordinateTransformer(sourceCRS, targetCRS);
		Collection<SimpleFeature> coll = reader.getFeatureSet();
		transform.transform(coll);
		ShapeFileWriter.writeGeometries(coll, output);
		
		
	}
}
