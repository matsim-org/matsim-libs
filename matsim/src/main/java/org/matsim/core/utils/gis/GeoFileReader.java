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

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Counter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author glaemmel
 * @author dgrether
 * @author mrieser // switch to GeoTools 2.7.3
 * @author nkuehnel / MOIA // add gpkg suuport
 */
public class GeoFileReader implements MatsimSomeReader {
    	private static final Logger log = LogManager.getLogger(GeoFileReader.class);

	private SimpleFeatureSource featureSource = null;

	private ReferencedEnvelope bounds = null;

	private DataStore dataStore = null;

	private SimpleFeatureCollection featureCollection = null;

	private SimpleFeatureType schema = null;

	private Collection<SimpleFeature> featureSet = null;

	private CoordinateReferenceSystem crs;

	public static Collection<SimpleFeature> getAllFeatures(final String filename) {
		return getAllFeatures(filename, null);
	}


	public static Collection<SimpleFeature> getAllFeatures(final String filename, Name layerName) {
		try {
			if(filename.endsWith(".shp")) {
				File dataFile = new File(filename);
				log.info("will try to read from " + dataFile.getAbsolutePath());
				Gbl.assertIf(dataFile.exists());
                FileDataStore dataStore = FileDataStoreFinder.getDataStore(dataFile);
				return getSimpleFeatures(dataStore);
			} else if(filename.endsWith(".gpkg")){
				Gbl.assertNotNull(layerName);
				Map<String, Object> params = new HashMap<>();
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, filename);
				params.put(GeoPkgDataStoreFactory.READ_ONLY.key, true);
				DataStore dataStore = DataStoreFinder.getDataStore(params);
				return getSimpleFeatures(dataStore, layerName);
			} else {
				throw new RuntimeException("Unsupported file type.");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}



	public static Collection<SimpleFeature> getAllFeatures(final URL url) {
		try {
			log.info( "will try to read from " + url.getPath() ) ;
			if (url.getFile().endsWith(".gpkg")) {
				return getAllFeaturesGPKG(url);
			}
			return getSimpleFeatures(FileDataStoreFinder.getDataStore(url));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Loads geo package like normal shape files using the first layer found in the file.
	 */
	private static Collection<SimpleFeature> getAllFeaturesGPKG(final URL url) throws URISyntaxException, IOException {

		File file;
		// Remote files have to be downloaded
		if (url.getProtocol().startsWith("http") || url.getProtocol().startsWith("jar")) {

			String name = FilenameUtils.getBaseName(url.getFile());

			Path tmp = Files.createTempFile(name, ".gpkg");
			Files.copy(url.openStream(), tmp, StandardCopyOption.REPLACE_EXISTING);

			file = tmp.toFile();
			file.deleteOnExit();
		} else
		 	file = new File(url.toURI());

		Map<String, Object> params = new HashMap<>();
		params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
		params.put(GeoPkgDataStoreFactory.DATABASE.key, file.toString());
		params.put(GeoPkgDataStoreFactory.READ_ONLY.key, true);
		DataStore dataStore = DataStoreFinder.getDataStore(params);

		String[] typeNames = dataStore.getTypeNames();

		// Use first layer
		return getSimpleFeatures(dataStore, typeNames[0]);
	}

	/**
	 * Read all simple features from a data store. This method makes sure the store is closed afterwards.
	 * @return list of contained features.
	 */
	public static List<SimpleFeature> getSimpleFeatures(FileDataStore dataStore) throws IOException {
		SimpleFeatureSource featureSource = dataStore.getFeatureSource();
		List<SimpleFeature> featureSet = getSimpleFeatures(featureSource);
		dataStore.dispose();
		return featureSet;
	}

	/**
	 * Read all simple features from a data store. This method makes sure the store is closed afterwards.
	 * @return list of contained features.
	 */
	public static List<SimpleFeature> getSimpleFeatures(DataStore dataStore, Name layerName) throws IOException {
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(layerName);
		Gbl.assertNotNull(featureSource);
		List<SimpleFeature> featureSet = getSimpleFeatures(featureSource);
		dataStore.dispose();
		return featureSet;
	}

	/**
	 * Read all simple features from a data store. This method makes sure the store is closed afterwards.
	 * @return list of contained features.
	 * @see #getSimpleFeatures(DataStore, Name)
	 */
	public static List<SimpleFeature> getSimpleFeatures(DataStore dataStore, String layerName) throws IOException {
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(layerName);
		Gbl.assertNotNull(featureSource);
		List<SimpleFeature> featureSet = getSimpleFeatures(featureSource);
		dataStore.dispose();
		return featureSet;
	}

	private static List<SimpleFeature> getSimpleFeatures(SimpleFeatureSource featureSource) throws IOException {
		SimpleFeatureIterator it = featureSource.getFeatures().features();
		List<SimpleFeature> featureSet = new ArrayList<>();
		while (it.hasNext()) {
			SimpleFeature ft = it.next();
			featureSet.add(ft);
		}
		it.close();
		return featureSet;
	}

	public Collection<SimpleFeature> readFileAndInitialize(final String filename) {
		return readFileAndInitialize(filename, null);
	}

		/**
		 * Reads all Features in the file into the returned Set and initializes the instance of this class.
		 */
	public Collection<SimpleFeature> readFileAndInitialize(final String filename, Name layerName) throws UncheckedIOException {
		try {
			this.featureSource = GeoFileReader.readDataFile(filename, layerName);
			this.init();
			SimpleFeature ft = null;
			SimpleFeatureIterator it = this.featureSource.getFeatures().features();
			this.featureSet = new ArrayList<SimpleFeature>();
			log.info("features to read #" + this.featureSource.getFeatures().size());
			Counter cnt = new Counter("features read #");
			while (it.hasNext()) {
				ft = it.next();
				this.featureSet.add(ft);
				cnt.incCounter();
			}
			cnt.printCounter();
			it.close();
			return this.featureSet;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static SimpleFeatureSource readDataFile(final String filename) {
		return readDataFile(filename, null);
	}

		/**
		 * <em>VERY IMPORTANT NOTE</em><br>
		 * <p></p>
		 * There are many ways to use that class in a wrong way. The safe way is the following:
		 * <p></p>
		 * <pre> GeoFileReader geoFileReader = new GeoFileReader();
		 * geoFileReader.readFileAndInitialize(geoFile); </pre>
		 * <p></p>
		 * Then, get the features by
		 * <p></p>
		 * <pre> Set<{@link org.geotools.api.feature.Feature}> features = geoFileReader.getFeatureSet(); </pre>
		 * <p></p>
		 * If you need metadata you can use
		 * <p></p>
		 * <pre> FeatureSource fs = geoFileReader.getFeatureSource(); </pre>
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
		 * Provides access to a shape or geopackage file and returns a <code>FeatureSource</code> containing all features.
		 * Take care access means on disk access, i.e. the FeatureSource is only a pointer to the information
		 * stored in the file. This can be horribly slow if invoked many times and throw exceptions if two many read
		 * operations to the same file are performed. In those cases it is recommended to use the method readDataFileToMemory
		 * of this class.
		 *
		 * @param filename File name of a shape or geopackage file (ending in <code>*.shp</code> or <code>*.gpkg</code>)
		 * @return FeatureSource containing all features.
		 * @throws UncheckedIOException if the file cannot be found or another error happens during reading
		 */
	public static SimpleFeatureSource readDataFile(final String filename, Name layerName) throws UncheckedIOException {
		try {
			log.warn("Unsafe method! store.dispose() is not called from within this method");
			SimpleFeatureSource featureSource;
			if(filename.endsWith(".shp")) {
				File dataFile = new File(filename);
				FileDataStore store = FileDataStoreFinder.getDataStore(dataFile);
                featureSource = store.getFeatureSource();
            } else if(filename.endsWith(".gpkg")) {
				Gbl.assertNotNull(layerName);
				Map<String, Object> params = new HashMap<>();
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, filename);
				params.put(GeoPkgDataStoreFactory.READ_ONLY.key, true);

				DataStore datastore = DataStoreFinder.getDataStore(params);
				featureSource = datastore.getFeatureSource(layerName);
				Gbl.assertNotNull(featureSource);
			} else {
				throw new RuntimeException("Unsupported file type.");
			}
			return featureSource;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void init() {
		try {
			this.bounds = this.featureSource.getBounds();
			this.dataStore = (DataStore) this.featureSource.getDataStore();
			this.featureCollection = this.featureSource.getFeatures();
			this.schema = this.featureSource.getSchema();
			this.crs = this.featureSource.getSchema().getCoordinateReferenceSystem();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public SimpleFeatureSource getFeatureSource() {
		return featureSource;
	}

	public ReferencedEnvelope getBounds() {
		return bounds;
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public SimpleFeatureCollection getFeatureCollection() {
		return featureCollection;
	}

	public SimpleFeatureType getSchema() {
		return schema;
	}

	public Collection<SimpleFeature> getFeatureSet() {
		return featureSet;
	}

	public CoordinateReferenceSystem getCoordinateSystem(){
		return this.crs;
	}


}
