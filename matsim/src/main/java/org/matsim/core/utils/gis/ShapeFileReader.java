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

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author glaemmel
 * @author dgrether
 * @author mrieser // switch to GeoTools 2.7.3
 */
public class ShapeFileReader implements MatsimSomeReader {
    	private static final Logger log = Logger.getLogger(ShapeFileReader.class);

	private SimpleFeatureSource featureSource = null;

	private ReferencedEnvelope bounds = null;

	private DataStore dataStore = null;

	private SimpleFeatureCollection featureCollection = null;

	private SimpleFeatureType schema = null;

	private Collection<SimpleFeature> featureSet = null;

	private CoordinateReferenceSystem crs;

	public static Collection<SimpleFeature> getAllFeatures(final String filename) {
		try {
			File dataFile = new File(filename);
			log.info( "will try to read from " + dataFile.getAbsolutePath() ) ;
			Gbl.assertIf( dataFile.exists() );
			FileDataStore store = FileDataStoreFinder.getDataStore(dataFile);
			SimpleFeatureSource featureSource = store.getFeatureSource();

			SimpleFeatureIterator it = featureSource.getFeatures().features();
			List<SimpleFeature> featureSet = new ArrayList<SimpleFeature>();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				featureSet.add(ft);
			}
			it.close();
			store.dispose();
			return featureSet;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Collection<SimpleFeature> getAllFeatures(final URL url) {
		try {
			log.info( "will try to read from " + url.getPath() ) ;
			FileDataStore store = FileDataStoreFinder.getDataStore(url);
			SimpleFeatureSource featureSource = store.getFeatureSource();

			SimpleFeatureIterator it = featureSource.getFeatures().features();
			List<SimpleFeature> featureSet = new ArrayList<SimpleFeature>();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				featureSet.add(ft);
			}
			it.close();
			store.dispose();
			return featureSet;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Reads all Features in the file into the returned Set and initializes the instance of this class.
	 */
	public Collection<SimpleFeature> readFileAndInitialize(final String filename) throws UncheckedIOException {
		try {
			this.featureSource = ShapeFileReader.readDataFile(filename);
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
		try {
			log.warn("Unsafe method! store.dispose() is not called from within this method");
			File dataFile = new File(filename);
			FileDataStore store = FileDataStoreFinder.getDataStore(dataFile);
			return store.getFeatureSource();
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
