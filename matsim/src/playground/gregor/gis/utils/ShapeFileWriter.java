/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileWriter.java
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

package playground.gregor.gis.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.Feature;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;

public class ShapeFileWriter {
	private static final Logger log = Logger.getLogger(ShapeFileWriter.class);
	
	
	public static void writeGeometries(Collection<Feature> features, String filename) throws IOException, FactoryException, SchemaException{
		
		log.info("writing features to: " + filename);
		URL fileURL = (new File(filename)).toURL();
		ShapefileDataStore datastore = new ShapefileDataStore(fileURL);
		Feature feature = (Feature) features.iterator().next();
		datastore.createSchema(feature.getFeatureType());
	    
//		Feature [] featuresArray = new Feature [features.size()];
//		features.toArray(featuresArray);
		FeatureStore featureStore = (FeatureStore)(datastore.getFeatureSource(feature.getFeatureType().getTypeName()));
		FeatureReader aReader = DataUtilities.reader(features);
		
		featureStore. addFeatures( aReader);
		log.info("done");
	}
}
