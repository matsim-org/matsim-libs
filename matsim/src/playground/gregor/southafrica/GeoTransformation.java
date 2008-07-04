/* *********************************************************************** *
 * project: org.matsim.*
 * GeoTransformation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.southafrica;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.utils.gis.ShapeFileReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.gregor.gis.utils.ShapeFileWriter;

import com.vividsolutions.jts.geom.Geometry;

public class GeoTransformation {
	private final static String WGS84_UTM34S = "PROJCS[\"WGS_1984_UTM_Zone_34S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",21],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";
	

	public static void main(final String [] args) throws Exception {
		final String filename = "./southafrica/gt_str_h.shp";
		final FeatureSource fs = ShapeFileReader.readDataFile(filename);
	    final String SourceWKT = fs.getSchema().getDefaultGeometry().getCoordinateSystem().toString();
	    System.out.println("Source WKT:\n" + SourceWKT);
	    final CoordinateReferenceSystem sourceCRS = CRS.parseWKT(SourceWKT);
	    System.out.println("Target WKT:\n" + WGS84_UTM34S);
	    final CoordinateReferenceSystem targetCRS = CRS.parseWKT( WGS84_UTM34S);
	    final MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		final Collection<Feature> transformed = new ArrayList<Feature>();
		final Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			final Feature ft = (Feature) it.next();
			final Geometry targetGeometry = JTS.transform(ft.getDefaultGeometry() , transform);
			ft.setDefaultGeometry(targetGeometry);
			transformed.add(ft);
		}
		ShapeFileWriter.writeGeometries(transformed, "./southafrica/gt_str_h_transformed.shp");
	}
	
}
