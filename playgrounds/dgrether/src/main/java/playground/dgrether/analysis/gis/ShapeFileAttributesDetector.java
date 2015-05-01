/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileAttributesDetector
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.gis;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * 
 * Simple class to detect the available attributes of a shape file.
 * @author dgrether
 * 
 */
public class ShapeFileAttributesDetector {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String shapeFile = args[0];
		SimpleFeatureSource fts = ShapeFileReader.readDataFile(shapeFile);

		// Iterator to iterate over the features from the shape file
		SimpleFeatureIterator it = fts.getFeatures().features();
		SimpleFeature ft = it.next(); // A feature contains a geometry (in this case a
														// polygon) and an arbitrary number
		// of other attributes
		for (AttributeDescriptor desc : ft.getFeatureType().getAttributeDescriptors()) {
			System.out.println(desc.getName());
		}
	}

}
