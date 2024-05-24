/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.core.api.internal.MatsimSomeReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * @author glaemmel
 * @author dgrether
 * @author mrieser // switch to GeoTools 2.7.3
 */
@Deprecated
public class ShapeFileReader implements MatsimSomeReader {

	private final GeoFileReader geoFileReader = new GeoFileReader();

	public static Collection<SimpleFeature> getAllFeatures(final String filename) {
		return GeoFileReader.getAllFeatures(filename, null);
	}

	public static Collection<SimpleFeature> getAllFeatures(final URL url) {
		return GeoFileReader.getAllFeatures(url);
	}

	/**
	 * Read all simple features from a data store. This method makes sure the store is closed afterwards.
	 * @return list of contained features.
	 */
	public static List<SimpleFeature> getSimpleFeatures(FileDataStore dataStore) throws IOException {
		return GeoFileReader.getSimpleFeatures(dataStore);
	}

	/**
	 * Reads all Features in the file into the returned Set and initializes the instance of this class.
	 */
	public Collection<SimpleFeature> readFileAndInitialize(final String filename) throws UncheckedIOException {
		return geoFileReader.readFileAndInitialize(filename);
	}

	/**
	 * <em>VERY IMPORTANT NOTE</em><br>
	 * <p></p>
	 * There are many ways to use that class in a wrong way. The safe way is the following:
	 * <p></p>
	 * <pre> ShapeFileReader shapeFileReader = new ShapeFileReader();
	 * shapeFileReader.readFileAndInitialize(zonesShapeFile); </pre>
	 * <p></p>
	 * Then, get the features by
	 * <p></p>
	 * <pre> Set<{@link Feature}> features = shapeFileReader.getFeatureSet(); </pre>
	 * <p></p>
	 * If you need metadata you can use
	 * <p></p>
	 * <pre> FeatureSource fs = shapeFileReader.getFeatureSource(); </pre>
	 * <p></p>
	 * to get access to the feature source.<br>
	 * <em>BUT NEVER CALL <code>fs.getFeatures();</code> !!! It can happen that you will read from disk again!!! </em>
	 * <p></p>
	 * <p>
	 * Actually, the whole class must be fixed. But since it is anyway necessary to move to a more recent version of the geotools only this javadoc is added instead.
	 * </p>
	 * <p></p>
	 * <p>
	 * The following old doc is kept here:
	 * </p>
	 * <p></p>
	 * Provides access to a shape file and returns a <code>FeatureSource</code> containing all features.
	 * Take care access means on disk access, i.e. the FeatureSource is only a pointer to the information
	 * stored in the file. This can be horribly slow if invoked many times and throw exceptions if two many read
	 * operations to the same file are performed. In those cases it is recommended to use the method readDataFileToMemory
	 * of this class.
	 *
	 * @param filename File name of a shape file (ending in <code>*.shp</code>)
	 * @return FeatureSource containing all features.
	 * @throws UncheckedIOException if the file cannot be found or another error happens during reading
	 */
	public static SimpleFeatureSource readDataFile(final String filename) throws UncheckedIOException {
		return GeoFileReader.readDataFile(filename);
	}

	public SimpleFeatureSource getFeatureSource() {
		return geoFileReader.getFeatureSource();
	}

	public ReferencedEnvelope getBounds() {
		return geoFileReader.getBounds();
	}

	public DataStore getDataStore() {
		return geoFileReader.getDataStore();
	}

	public SimpleFeatureCollection getFeatureCollection() {
		return geoFileReader.getFeatureCollection();
	}

	public SimpleFeatureType getSchema() {
		return geoFileReader.getSchema();
	}

	public Collection<SimpleFeature> getFeatureSet() {
		return geoFileReader.getFeatureSet();
	}

	public CoordinateReferenceSystem getCoordinateSystem(){
		return geoFileReader.getCoordinateSystem();
	}


}
