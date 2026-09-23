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
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.matsim.core.api.internal.MatsimSomeWriter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;

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
			SimpleFeatureType featureType = features.iterator().next().getFeatureType();

			if (filename.endsWith(".shp")) {
				log.info("Writing shapefile to " + filename);
				URL fileURL = (new File(filename)).toURI().toURL();
				FileDataStore datastore = new ShapefileDataStore(fileURL);
				datastore.createSchema(featureType);
				SimpleFeatureStore featureSource = (SimpleFeatureStore) datastore.getFeatureSource();
				DefaultFeatureCollection coll = new DefaultFeatureCollection();
				coll.addAll(features);
				featureSource.addFeatures(coll);
				datastore.dispose();

			} else if (filename.endsWith(".gpkg")) {
				log.info("Writing GeoPackage to " + filename);
				writeGeoPackage(features, filename, layerName, featureType);
			} else {
				throw new RuntimeException("Unsupported file type: " + filename);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void writeGeoPackage(Collection<SimpleFeature> features, String filename, Name layerName, SimpleFeatureType featureType) throws IOException {
		File file = new File(filename);
		GeoPackage geopkg = new GeoPackage(file);
		try {
			geopkg.init();

			String entryName = layerName != null ? layerName.getLocalPart() : featureType.getTypeName();

			// Calculate bounds from features
			Envelope envelope = new Envelope();
			for (SimpleFeature feature : features) {
				Geometry geom = (Geometry) feature.getDefaultGeometry();
				if (geom != null && !geom.isEmpty()) {
					envelope.expandToInclude(geom.getEnvelopeInternal());
				}
			}

			FeatureEntry entry = new FeatureEntry();
			entry.setTableName(entryName);
			entry.setGeometryColumn(featureType.getGeometryDescriptor().getLocalName());
			entry.setSrid(getSrid(featureType));
			if (!envelope.isNull()) {
				entry.setBounds(new ReferencedEnvelope(envelope, featureType.getCoordinateReferenceSystem()));
			} else {
				log.warn("All feature geometries are null/empty for '{}'; GeoPackage bounds remain unset.", filename);
			}

			DefaultFeatureCollection coll = new DefaultFeatureCollection();
			coll.addAll(features);

			geopkg.add(entry, coll);
			geopkg.createSpatialIndex(entry);
		} finally {
			try {
				geopkg.close();
			} catch (Exception e) {
				log.warn("Error while closing GeoPackage '{}': {}", filename, e.getMessage(), e);
			}
		}
	}

	private static Integer getSrid(SimpleFeatureType featureType) {
		if (featureType.getCoordinateReferenceSystem() == null) {
			return null;
		}
		try {
			Integer code = org.geotools.referencing.CRS.lookupEpsgCode(featureType.getCoordinateReferenceSystem(), true);
			return code;
		} catch (Exception e) {
			log.warn("Could not determine EPSG code for CRS: " + e.getMessage());
			return null;
		}
	}
}
