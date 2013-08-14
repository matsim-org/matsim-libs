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

package org.matsim.core.utils.gis;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileFeatureStore;
import org.geotools.feature.DefaultFeatureCollections;
import org.geotools.feature.FeatureCollection;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
/**
 * This is a simple utility class that provides methods to write Feature instances
 * of the geotools framework to an ESRI shape file.
 *
 * @author glaemmel
 */
public class ShapeFileWriter implements MatsimSomeWriter {

	private static final Logger log = Logger.getLogger(ShapeFileWriter.class);
	
	public static void writeGeometries(final Collection<SimpleFeature> features, final String filename) {
		if (features.isEmpty()) {
			throw new UncheckedIOException("Cannot write empty collection");
		}
		log.info("Writing shapefile to " + filename);
		try {
			URL fileURL = (new File(filename)).toURI().toURL();

			ShapefileDataStore datastore = new ShapefileDataStore(fileURL);
			SimpleFeature feature = features.iterator().next();
			datastore.createSchema(feature.getFeatureType());

			FeatureCollection<SimpleFeatureType, SimpleFeature> coll = DefaultFeatureCollections.newCollection();
			coll.addAll(features);
			
			SimpleFeatureType featureType = features.iterator().next().getFeatureType();
			ShapefileFeatureStore store = new ShapefileFeatureStore(datastore, new HashSet<String>(), featureType);
			
			store.addFeatures(coll);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
