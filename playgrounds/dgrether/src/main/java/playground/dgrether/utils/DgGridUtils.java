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

import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.cottbus.scripts.DgCottbus2KoehlerStrehler2010;

import com.vividsolutions.jts.geom.Polygon;


/**
 * @author dgrether
 *
 */
public class DgGridUtils {

	
	public static void writeGrid2Shapefile(DgGrid grid, CoordinateReferenceSystem crs, String shapeFilename){
		Collection<Feature> featureCollection = new ArrayList<Feature>();
		FeatureType featureType = null;
		AttributeType [] attribs = new AttributeType[1];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Polygon", Polygon.class, true, null, null, crs);
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, "grid_cell");
			for (Polygon p : grid){
				DgCottbus2KoehlerStrehler2010.log.info("Grid cell: " + p);
				Feature feature = featureType.create(new Object[] {p});
				featureCollection.add(feature);
			}		
			ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}

}
