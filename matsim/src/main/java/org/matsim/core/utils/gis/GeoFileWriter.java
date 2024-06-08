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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.matsim.core.api.internal.MatsimSomeWriter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a simple utility class that provides methods to write Feature instances
 * of the geotools framework to an ESRI shape or geopackage file.
 *
 * @author glaemmel
 * @author nkuehnel / MOIA // add gpkg support
 */
public class GeoFileWriter implements MatsimSomeWriter {

	private static final Logger log = LogManager.getLogger(GeoFileWriter.class);

	public static void writeGeometries(final Collection<SimpleFeature> features, final String filename) {
		writeGeometries(features, filename, null);
	}


	public static void writeGeometries(final Collection<SimpleFeature> features, final String filename, Name layerName) {
		if (features.isEmpty()) {
			throw new UncheckedIOException(new IOException("Cannot write empty collection"));
		}

		try {
			SimpleFeatureStore featureSource;
			SimpleFeatureType featureType = features.iterator().next().getFeatureType();

			if(filename.endsWith(".shp")) {
				log.info("Writing shapefile to " + filename);
				URL fileURL = (new File(filename)).toURI().toURL();
        	    FileDataStore datastore = new ShapefileDataStore(fileURL);
				datastore.createSchema(featureType);
				featureSource = (SimpleFeatureStore) datastore.getFeatureSource();
        	} else if(filename.endsWith(".gpkg")){
				Map<String, Object> map = new HashMap<>();
				map.put(GeoPkgDataStoreFactory.DBTYPE.key, GeoPkgDataStoreFactory.DBTYPE.sample);
				map.put(GeoPkgDataStoreFactory.DATABASE.key, filename);
				map.put(JDBCDataStoreFactory.BATCH_INSERT_SIZE.key, 50);
				DataStore datastore = DataStoreFinder.getDataStore(map);
				datastore.createSchema(featureType);
				if(layerName == null) {
					layerName = new NameImpl(featureType.getTypeName());
				}
				featureSource = (SimpleFeatureStore) datastore.getFeatureSource(layerName);
            } else {
				throw new RuntimeException("Unsupported file type.");
			}

			DefaultFeatureCollection coll = new DefaultFeatureCollection();
			coll.addAll(features);

			featureSource.addFeatures(coll);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
