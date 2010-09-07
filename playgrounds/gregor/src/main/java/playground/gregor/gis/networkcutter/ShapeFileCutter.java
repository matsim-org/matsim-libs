/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileCutter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.gis.networkcutter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Envelope;

public class ShapeFileCutter {
	private static double max_x = 652088.;
	private static double max_y = 9894785.;
	private final static double MIN_X = 650473.;
	private final static double MIN_Y = 9892816.;
	private static final Envelope e = new Envelope(MIN_X,max_x,MIN_Y,max_y);
	
	public static void main(String [] args) throws IOException {
		String buildings = MY_STATIC_STUFF.CVS_GIS + "buildings_v20090403/buildings_v20090403.shp";
		String out = "tmp/buildings.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(buildings);
		
		List<Feature> outF = new ArrayList<Feature>();
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature f = (Feature) it.next();
			if (e.contains(f.getDefaultGeometry().getCentroid().getCoordinate())) {
				outF.add(f);
			}
		}
		
		ShapeFileWriter.writeGeometries(outF, out);
		
		
	}

}
