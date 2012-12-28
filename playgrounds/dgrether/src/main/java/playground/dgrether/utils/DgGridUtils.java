/* *********************************************************************** *
 * project: org.matsim.*
 * DgGridUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;


/**
 * @author dgrether
 *
 */
public class DgGridUtils {

	private static final Logger log = Logger.getLogger(DgGridUtils.class);
	
	public static void writeGrid2Shapefile(DgGrid grid, CoordinateReferenceSystem crs, String shapeFilename){
		Collection<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("grid_cell");
		b.add("location", Polygon.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());

		try {
			for (Polygon p : grid){
				log.info("Grid cell: " + p);
				SimpleFeature feature = builder.buildFeature(null, new Object[] {p});
				featureCollection.add(feature);
			}		
			ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}

}
